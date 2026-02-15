package com.kitsune.kanji.japanese.flashcards.domain.ink

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

internal data class HandwritingFusionResult(
    val finalScore: Int,
    val confidenceScore: Int,
    val qualityScore: Int,
    val strokePenalty: Int,
    val qualityCap: Int,
    val confidenceWeight: Float,
    val qualityWeight: Float,
    val usedConfidence: Boolean
)

internal fun fuseMlKitAndQualityScore(
    confidence: Float?,
    qualityScore: Int,
    strokeCount: Int,
    expectedStrokeCount: Int
): HandwritingFusionResult {
    val normalizedQuality = qualityScore.coerceIn(0, 100)
    val adjustedQuality = adjustQualityForUserInput(normalizedQuality)
    val confidenceScore = mapMlKitConfidenceToPercent(
        confidence = confidence,
        fallbackScore = adjustedQuality
    )
    val usedConfidence = confidence != null && confidence.isFinite()
    val confidenceWeight = if (usedConfidence) 0.55f else 0.35f
    val qualityWeight = 1f - confidenceWeight
    val strokePenalty = strokeCountPenalty(
        strokeCount = strokeCount,
        expectedStrokeCount = expectedStrokeCount
    )
    val effectivePenalty = (strokePenalty.toFloat() * if (usedConfidence) 0.8f else 0.35f)
        .roundToInt()

    val blended = (confidenceScore.toFloat() * confidenceWeight) +
        (adjustedQuality.toFloat() * qualityWeight) -
        effectivePenalty.toFloat()
    val qualityCap = qualityBasedCap(adjustedQuality)
    val nearPerfectBonus = if (adjustedQuality >= 90 && confidenceScore >= 88) {
        4
    } else {
        0
    }
    val poorFormFloor = if (adjustedQuality < 30) {
        52
    } else {
        100
    }
    val finalScore = (blended.roundToInt() + nearPerfectBonus)
        .coerceIn(0, 100)
        .coerceAtMost(minOf(qualityCap, poorFormFloor))

    return HandwritingFusionResult(
        finalScore = finalScore,
        confidenceScore = confidenceScore,
        qualityScore = adjustedQuality,
        strokePenalty = effectivePenalty,
        qualityCap = qualityCap,
        confidenceWeight = confidenceWeight,
        qualityWeight = qualityWeight,
        usedConfidence = usedConfidence
    )
}

internal fun mapMlKitConfidenceToPercent(confidence: Float?, fallbackScore: Int): Int {
    val fallback = fallbackScore.coerceIn(0, 100)
    if (confidence == null || confidence.isNaN() || confidence.isInfinite()) {
        return fallback
    }
    val normalized = when {
        confidence <= 1.2f -> confidence * 100f
        confidence <= 100f -> confidence
        else -> 100f
    }
    return normalized.roundToInt().coerceIn(0, 100)
}

private fun strokeCountPenalty(strokeCount: Int, expectedStrokeCount: Int): Int {
    if (strokeCount <= 0 || expectedStrokeCount <= 0) return 0
    val delta = abs(strokeCount - expectedStrokeCount)
    return when {
        delta == 0 -> 0
        delta == 1 -> 1
        delta == 2 -> 3
        delta == 3 -> 5
        else -> 8
    }
}

private fun qualityBasedCap(qualityScore: Int): Int {
    return when {
        qualityScore < 30 -> 52
        qualityScore < 40 -> 58
        qualityScore < 50 -> 72
        qualityScore < 60 -> 82
        qualityScore < 70 -> 90
        qualityScore < 80 -> 95
        else -> 100
    }
}

private fun adjustQualityForUserInput(rawQuality: Int): Int {
    val q = rawQuality.coerceIn(0, 100)
    val adjusted = when {
        q < 30 -> q * 0.95f
        q < 40 -> q + 7f
        q < 50 -> q + 16f
        q < 60 -> q + 16f
        q < 70 -> q + 12f
        q < 80 -> q + 8f
        q < 90 -> q + 4f
        else -> q.toFloat()
    }
    return max(0, adjusted.roundToInt()).coerceIn(0, 100)
}
