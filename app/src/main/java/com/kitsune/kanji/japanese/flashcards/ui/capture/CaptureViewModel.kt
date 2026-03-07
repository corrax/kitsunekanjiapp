package com.kitsune.kanji.japanese.flashcards.ui.capture

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.BuildConfig
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.CaptureQuotaPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CaptureStatus
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CapturedCardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CapturedMediaEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CapturedTermEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardEntity
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.util.UUID

// ── Backend contract ─────────────────────────────────────────────────────────

@Serializable
private data class BackendRequest(val image: String, val mimeType: String = "image/jpeg")

@Serializable
private data class BackendTerm(
    val kanji: String,
    val reading: String = "",
    val meaning: String = "",
    val notes: String = "",
    val jlptLevel: String = "unknown"
)

@Serializable
private data class BackendResponse(val terms: List<BackendTerm>)

// ── UI models ────────────────────────────────────────────────────────────────

data class RecognizedTerm(
    val text: String,
    val reading: String,
    val meaning: String,
    val jlptLevel: String = "unknown",
    val confidence: Float,
    val selected: Boolean = true
)

data class CaptureHistoryItem(
    val cardId: String,
    val kanji: String,
    val kana: String,
    val meaning: String,
    val jlptLevel: String,
    val includeInDaily: Boolean
)

data class CaptureUiState(
    val phase: CapturePhase = CapturePhase.CAMERA,
    val recognizedTerms: List<RecognizedTerm> = emptyList(),
    val capturedHistory: List<CaptureHistoryItem> = emptyList(),
    val isProcessing: Boolean = false,
    val savedCount: Int = 0,
    val errorMessage: String? = null,
    val capturesUsedThisWeek: Int = 0,
    val isPlusEntitled: Boolean = false
) {
    val isQuotaExceeded: Boolean
        get() = !isPlusEntitled && capturesUsedThisWeek >= CaptureQuotaPreferences.FREE_WEEKLY_LIMIT
}

enum class CapturePhase { CAMERA, PROCESSING, REVIEW, SAVED, HISTORY }

// ── ViewModel ────────────────────────────────────────────────────────────────

class CaptureViewModel(
    private val repository: KitsuneRepository,
    private val cacheDir: File,
    private val billingPreferences: BillingPreferences,
    private val captureQuotaPreferences: CaptureQuotaPreferences,
    startInHistory: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState = _uiState.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private var lastMediaId: String? = null
    private var cachedPurchaseToken: String? = null

    init {
        viewModelScope.launch {
            combine(
                billingPreferences.entitlementFlow,
                captureQuotaPreferences.weeklyUsedFlow
            ) { entitlement, used ->
                entitlement to used
            }.collect { (entitlement, used) ->
                cachedPurchaseToken = entitlement.lastPurchaseToken
                _uiState.update {
                    it.copy(
                        isPlusEntitled = entitlement.isPlusEntitled,
                        capturesUsedThisWeek = used
                    )
                }
            }
        }
        if (startInHistory) {
            _uiState.update { it.copy(phase = CapturePhase.HISTORY) }
            loadHistory()
        }
    }

    // ── Capture flow ─────────────────────────────────────────────────────────

    fun processImage(bitmap: Bitmap) {
        if (_uiState.value.isQuotaExceeded) return
        _uiState.update { it.copy(isProcessing = true, phase = CapturePhase.PROCESSING, errorMessage = null) }
        viewModelScope.launch {
            try {
                val terms = analyzeWithBackend(bitmap)
                if (terms.isEmpty()) {
                    _uiState.update {
                        it.copy(isProcessing = false, phase = CapturePhase.CAMERA, errorMessage = "No Japanese text found in this image.")
                    }
                    return@launch
                }

                // Count against free-tier quota only when not Plus (Plus = unlimited)
                val newCount = if (_uiState.value.isPlusEntitled) {
                    _uiState.value.capturesUsedThisWeek
                } else {
                    captureQuotaPreferences.incrementWeeklyUsed()
                }
                _uiState.update { it.copy(capturesUsedThisWeek = newCount) }

                val mediaId = UUID.randomUUID().toString()
                lastMediaId = mediaId
                val file = File(cacheDir, "capture_$mediaId.jpg")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                }
                repository.saveCapturedMedia(
                    CapturedMediaEntity(
                        mediaId = mediaId,
                        localUri = file.absolutePath,
                        capturedAtEpochMillis = System.currentTimeMillis(),
                        ocrText = terms.joinToString(", ") { it.text },
                        status = CaptureStatus.RESOLVED
                    )
                )
                _uiState.update {
                    it.copy(phase = CapturePhase.REVIEW, recognizedTerms = terms, isProcessing = false)
                }
            } catch (e: Exception) {
                val msg = friendlyErrorMessageFor(e)
                if (msg == "QUOTA_EXCEEDED") {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            phase = CapturePhase.CAMERA,
                            capturesUsedThisWeek = CaptureQuotaPreferences.FREE_WEEKLY_LIMIT
                        )
                    }
                } else {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = msg, phase = CapturePhase.CAMERA) }
                }
            }
        }
    }

    private suspend fun analyzeWithBackend(bitmap: Bitmap): List<RecognizedTerm> =
        withContext(Dispatchers.IO) {
            val backendUrl = BuildConfig.CAPTURE_BACKEND_URL.trim()
            if (backendUrl.isBlank()) {
                throw Exception(
                    "Capture backend not configured.\n" +
                    "Set CAPTURE_BACKEND_URL in build.gradle to enable photo analysis."
                )
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            val requestJson = json.encodeToString(BackendRequest(image = base64))

            val connection = URL(backendUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            val token = cachedPurchaseToken
            if (!token.isNullOrBlank()) {
                connection.setRequestProperty("x-purchase-token", token)
            }
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            connection.outputStream.use { os ->
                os.write(requestJson.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw Exception(backendErrorMessage(code, errorBody))
            }
            val responseBody = connection.inputStream.bufferedReader().readText()

            json.decodeFromString<BackendResponse>(responseBody).terms.map { term ->
                RecognizedTerm(
                    text = term.kanji,
                    reading = term.reading,
                    meaning = term.meaning.ifBlank { "—" },
                    jlptLevel = term.jlptLevel.ifBlank { "unknown" },
                    confidence = 1.0f,
                    selected = true
                )
            }
        }

    private fun friendlyErrorMessageFor(error: Exception): String {
        return when (error) {
            is UnknownHostException,
            is ConnectException -> "No internet connection. Check your network and try again."
            is SocketTimeoutException -> "Connection timed out. Please try again."
            else -> error.message ?: "Analysis failed."
        }
    }

    private fun backendErrorMessage(code: Int, responseBody: String): String {
        val normalized = responseBody.uppercase()
        return when {
            code == HttpURLConnection.HTTP_BAD_REQUEST && normalized.contains("INVALID_ARGUMENT") ->
                "Could not read this image clearly. Point at Japanese text or labeled objects and try again."
            code == HttpURLConnection.HTTP_UNAVAILABLE ->
                "Capture service is temporarily unavailable. Please try again."
            code == 402 ->
                "QUOTA_EXCEEDED"
            code == 429 ->
                "Too many capture requests. Please wait a moment and try again."
            else -> "Backend returned $code"
        }
    }

    fun toggleTerm(index: Int) {
        _uiState.update { state ->
            val updated = state.recognizedTerms.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(selected = !updated[index].selected)
            }
            state.copy(recognizedTerms = updated)
        }
    }

    fun saveSelectedTerms() {
        val mediaId = lastMediaId ?: return
        val selected = _uiState.value.recognizedTerms.filter { it.selected }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val terms = selected.map { term ->
                CapturedTermEntity(
                    termId = UUID.randomUUID().toString(),
                    mediaId = mediaId,
                    kanji = term.text,
                    kana = term.reading,
                    meaning = term.meaning,
                    cropRect = null,
                    confidence = term.confidence,
                    source = "llm",
                    jlptLevel = term.jlptLevel
                )
            }
            repository.saveCapturedTerms(terms)

            var saved = 0
            for ((i, term) in terms.withIndex()) {
                val s = selected[i]
                val cardId = "cap_${term.termId}"
                val choices = generateDistractors(s.meaning, selected.map { it.meaning })
                repository.saveCard(
                    CardEntity(
                        cardId = cardId,
                        type = CardType.KANJI_MEANING,
                        prompt = s.text,
                        canonicalAnswer = s.meaning,
                        acceptedAnswersRaw = s.meaning,
                        reading = s.reading,
                        meaning = s.meaning,
                        promptFurigana = null,
                        choicesRaw = choices.joinToString("|"),
                        difficulty = 3,
                        templateId = "tmpl_$cardId"
                    )
                )
                repository.saveCapturedCard(
                    CapturedCardEntity(
                        cardId = cardId,
                        termId = term.termId,
                        createdAtEpochMillis = now,
                        includeInDaily = true
                    )
                )
                saved++
            }
            _uiState.update { it.copy(phase = CapturePhase.SAVED, savedCount = saved) }
        }
    }

    fun resetToCamera() {
        _uiState.update { CaptureUiState() }
        lastMediaId = null
    }

    // ── History flow ─────────────────────────────────────────────────────────

    fun loadHistory() {
        viewModelScope.launch {
            val rows = repository.getCapturedHistory()
            _uiState.update { state ->
                state.copy(
                    capturedHistory = rows.map { row ->
                        CaptureHistoryItem(
                            cardId = row.cardId,
                            kanji = row.kanji,
                            kana = row.kana ?: "",
                            meaning = row.meaning ?: "—",
                            jlptLevel = row.jlptLevel,
                            includeInDaily = row.includeInDaily
                        )
                    }
                )
            }
        }
    }

    fun toggleCardInDaily(cardId: String, include: Boolean) {
        viewModelScope.launch {
            repository.setCapturedCardInDaily(cardId, include)
            _uiState.update { state ->
                state.copy(
                    capturedHistory = state.capturedHistory.map {
                        if (it.cardId == cardId) it.copy(includeInDaily = include) else it
                    }
                )
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            repository.deleteCapturedCard(cardId)
            _uiState.update { state ->
                state.copy(capturedHistory = state.capturedHistory.filter { it.cardId != cardId })
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun generateDistractors(correct: String, allMeanings: List<String>): List<String> {
        val pool = listOf(
            "sugar-free", "half price", "new product", "spicy", "exit",
            "entrance", "push", "pull", "receipt", "address",
            "station", "express", "transfer", "prohibited", "caution"
        )
        val others = (allMeanings + pool).filter { it != correct }.distinct().shuffled().take(3)
        return (others + correct).shuffled()
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            cacheDir: File,
            billingPreferences: BillingPreferences,
            captureQuotaPreferences: CaptureQuotaPreferences,
            startInHistory: Boolean = false
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                CaptureViewModel(
                    repository = repository,
                    cacheDir = cacheDir,
                    billingPreferences = billingPreferences,
                    captureQuotaPreferences = captureQuotaPreferences,
                    startInHistory = startInHistory
                )
            }
        }
    }
}
