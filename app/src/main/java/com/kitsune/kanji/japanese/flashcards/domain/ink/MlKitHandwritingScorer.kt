package com.kitsune.kanji.japanese.flashcards.domain.ink

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import com.kitsune.kanji.japanese.flashcards.domain.model.StrokeTemplate
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitHandwritingScorer(
    languageTag: String = "ja-JP"
) : HandwritingScorer {
    private val modelId = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja-JP")
    private val model = modelId?.let { id -> DigitalInkRecognitionModel.builder(id).build() }
    private val recognizer = model?.let { model ->
        DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model).build()
        )
    }
    private val modelManager = RemoteModelManager.getInstance()
    private val qualityScorer = DeterministicHandwritingScorer()
    private val downloadLock = Mutex()
    @Volatile
    private var modelReady: Boolean = false

    override suspend fun score(sample: InkSample, template: StrokeTemplate): HandwritingScore {
        logDebug(
            "score.start target=${template.target} strokes=${sample.strokes.size} points=${sample.totalPointCount()} " +
                "canvas=${sample.canvasWidth}x${sample.canvasHeight}"
        )
        if (sample.strokes.isEmpty()) {
            return HandwritingScore(
                score = 0,
                feedback = "No ink captured. Write the character before submitting."
            )
        }
        val recognitionClient = recognizer
        val remoteModel = model
        if (recognitionClient == null || remoteModel == null) {
            logWarn("score.unavailable recognizer/model missing")
            return HandwritingScore(
                score = 0,
                feedback = "Japanese handwriting recognizer is unavailable on this device."
            )
        }
        val ready = ensureModelDownloaded(remoteModel)
        if (!ready) {
            logWarn("score.model_not_ready target=${template.target}")
            return HandwritingScore(
                score = 0,
                feedback = "Japanese handwriting model is not available yet. Check connection and try again."
            )
        }

        val ink = sample.toMlKitInk()
        return try {
            val width = sample.canvasWidth?.takeIf { it > 1f }
            val height = sample.canvasHeight?.takeIf { it > 1f }
            val contextBuilder = RecognitionContext.builder()
            // ML Kit 19.0.0 requires preContext; use a single space for isolated-kanji recognition.
            contextBuilder.setPreContext(MLKIT_PRE_CONTEXT)
            if (width != null && height != null) {
                contextBuilder.setWritingArea(WritingArea(width, height))
            }
            logDebug(
                "score.context preContextLength=${MLKIT_PRE_CONTEXT.length} " +
                    "writingArea=${width ?: "none"}x${height ?: "none"}"
            )
            val context = contextBuilder.build()
            val result = recognitionClient.recognize(ink, context).awaitTask()
            val topCandidate = result.candidates.firstOrNull()
            val topText = topCandidate?.text?.trim().orEmpty()
            val topConfidence = topCandidate?.score
            val quality = qualityScorer.score(sample, template)
            val fusion = fuseMlKitAndQualityScore(
                confidence = topConfidence,
                qualityScore = quality.score,
                strokeCount = sample.strokes.size,
                expectedStrokeCount = template.expectedStrokeCount
            )
            val candidateSummary = result.candidates
                .take(MAX_DEBUG_CANDIDATES)
                .joinToString(separator = "; ") { candidate ->
                    "${candidate.text}:${candidate.score}"
                }
            val expected = normalizeKanji(template.target)
            val recognized = normalizeKanji(topText)
            logDebug(
                "score.result target=${template.target} recognized=$topText confidenceRaw=$topConfidence " +
                    "confidenceMapped=${fusion.confidenceScore} quality=${fusion.qualityScore} fused=${fusion.finalScore} " +
                    "weights=${fusion.confidenceWeight}/${fusion.qualityWeight} strokePenalty=${fusion.strokePenalty} " +
                    "qualityCap=${fusion.qualityCap} usedConfidence=${fusion.usedConfidence} " +
                    "candidates=[$candidateSummary]"
            )
            if (topText.isBlank()) {
                HandwritingScore(
                    score = 0,
                    feedback = "Could not recognize a kanji. Please rewrite it clearly."
                )
            } else if (recognized == expected) {
                val formFeedback = formFeedbackForQuality(fusion.qualityScore)
                HandwritingScore(
                    score = fusion.finalScore,
                    feedback = "Recognized \"$topText\". Form quality: ${fusion.qualityScore}%." +
                        " $formFeedback Score: ${fusion.finalScore}/100.",
                    recognizedText = topText
                )
            } else {
                HandwritingScore(
                    score = 0,
                    feedback = "Recognized \"$topText\". Expected \"${template.target}\".",
                    recognizedText = topText
                )
            }
        } catch (error: Throwable) {
            logError("score.failure target=${template.target} message=${error.message}", error)
            HandwritingScore(
                score = 0,
                feedback = "Handwriting recognition failed. Please try again."
            )
        }
    }

    private suspend fun ensureModelDownloaded(remoteModel: DigitalInkRecognitionModel): Boolean {
        if (modelReady) return true
        return downloadLock.withLock {
            if (modelReady) return@withLock true
            val isDownloaded = runCatching {
                modelManager.isModelDownloaded(remoteModel).awaitTask()
            }.getOrDefault(false)
            logDebug("model.check downloaded=$isDownloaded")
            if (isDownloaded) {
                modelReady = true
                return@withLock true
            }
            val downloaded = runCatching {
                modelManager.download(
                    remoteModel,
                    DownloadConditions.Builder().build()
                ).awaitTask()
                true
            }.getOrDefault(false)
            logDebug("model.download attempted=true success=$downloaded")
            if (!downloaded) {
                return@withLock false
            }
            val confirmed = runCatching {
                modelManager.isModelDownloaded(remoteModel).awaitTask()
            }.getOrDefault(false)
            modelReady = confirmed
            logDebug("model.confirm downloaded=$confirmed")
            confirmed
        }
    }

    private fun InkSample.toMlKitInk(): Ink {
        val inkBuilder = Ink.builder()
        var timestampMillis = 0L
        strokes.forEach { stroke ->
            val points = stroke.points
            if (points.isEmpty()) return@forEach
            val strokeBuilder = Ink.Stroke.builder()
            points.forEach { point ->
                strokeBuilder.addPoint(
                    Ink.Point.create(
                        point.x,
                        point.y,
                        timestampMillis
                    )
                )
                timestampMillis += 16L
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }

    private fun normalizeKanji(value: String): String {
        return value.trim()
            .replace(" ", "")
            .replace("\u3000", "")
    }

    private fun InkSample.totalPointCount(): Int {
        return strokes.sumOf { it.points.size }
    }

    private fun formFeedbackForQuality(qualityScore: Int): String {
        return when {
            qualityScore >= 90 ->
                "Excellent structure and stroke balance."
            qualityScore >= 78 ->
                "Strong form. Minor spacing or angle refinements will make it cleaner."
            qualityScore >= 62 ->
                "Good overall shape. Keep stroke lengths and spacing more consistent."
            qualityScore >= 45 ->
                "The character is recognizable, but stroke placement is uneven. Slow down and separate each stroke."
            else ->
                "Form is unstable. Use larger, deliberate strokes and keep the main frame of the kanji clear."
        }
    }

    private fun logDebug(message: String) {
        if (DEBUG_LOGS) {
            Log.d(LOG_TAG, message)
        }
    }

    private fun logWarn(message: String) {
        if (DEBUG_LOGS) {
            Log.w(LOG_TAG, message)
        }
    }

    private fun logError(message: String, throwable: Throwable) {
        if (DEBUG_LOGS) {
            Log.e(LOG_TAG, message, throwable)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { error ->
            continuation.resumeWithException(error)
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }
}

private const val LOG_TAG = "KitsuneInkMlKit"
private const val DEBUG_LOGS = true
private const val MAX_DEBUG_CANDIDATES = 3
private const val MLKIT_PRE_CONTEXT = " "
