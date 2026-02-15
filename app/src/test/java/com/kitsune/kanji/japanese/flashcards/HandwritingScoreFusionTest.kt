package com.kitsune.kanji.japanese.flashcards

import com.kitsune.kanji.japanese.flashcards.domain.ink.fuseMlKitAndQualityScore
import com.kitsune.kanji.japanese.flashcards.domain.ink.mapMlKitConfidenceToPercent
import org.junit.Assert.assertTrue
import org.junit.Test

class HandwritingScoreFusionTest {
    @Test
    fun mapMlKitConfidenceToPercent_nullConfidence_usesFallback() {
        val mapped = mapMlKitConfidenceToPercent(
            confidence = null,
            fallbackScore = 84
        )
        assertTrue(mapped == 84)
    }

    @Test
    fun fuseMlKitAndQualityScore_sameConfidence_separatesByQuality() {
        val higherQuality = fuseMlKitAndQualityScore(
            confidence = 0.70f,
            qualityScore = 82,
            strokeCount = 5,
            expectedStrokeCount = 5
        )
        val lowerQuality = fuseMlKitAndQualityScore(
            confidence = 0.70f,
            qualityScore = 44,
            strokeCount = 5,
            expectedStrokeCount = 5
        )

        assertTrue(higherQuality.finalScore >= lowerQuality.finalScore + 10)
    }

    @Test
    fun fuseMlKitAndQualityScore_lowQuality_isCappedEvenWhenConfidenceHigh() {
        val fused = fuseMlKitAndQualityScore(
            confidence = 0.95f,
            qualityScore = 32,
            strokeCount = 5,
            expectedStrokeCount = 5
        )
        assertTrue(fused.finalScore <= 58)
    }

    @Test
    fun fuseMlKitAndQualityScore_largeStrokeMismatch_penalizesScore() {
        val aligned = fuseMlKitAndQualityScore(
            confidence = 0.80f,
            qualityScore = 78,
            strokeCount = 4,
            expectedStrokeCount = 4
        )
        val mismatched = fuseMlKitAndQualityScore(
            confidence = 0.80f,
            qualityScore = 78,
            strokeCount = 1,
            expectedStrokeCount = 7
        )
        assertTrue(aligned.finalScore >= mismatched.finalScore + 6)
    }

    @Test
    fun fuseMlKitAndQualityScore_midQualityFallsInStudyOnTheGoBand() {
        val fused = fuseMlKitAndQualityScore(
            confidence = null,
            qualityScore = 48,
            strokeCount = 2,
            expectedStrokeCount = 7
        )
        assertTrue("mid-quality final=${fused.finalScore}", fused.finalScore in 60..80)
    }
}
