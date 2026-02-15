package com.kitsune.kanji.japanese.flashcards.ui.common

import androidx.compose.ui.graphics.Color
import com.kitsune.kanji.japanese.flashcards.domain.scoring.ScoreBand
import com.kitsune.kanji.japanese.flashcards.domain.scoring.scoreBandFor

data class ScoreVisual(
    val label: String,
    val toneColor: Color,
    val stampBackground: Color,
    val stampText: Color
)

fun scoreVisualFor(score: Int): ScoreVisual {
    return when (scoreBandFor(score)) {
        ScoreBand.EXCELLENT -> ScoreVisual(
            label = "Excellent",
            toneColor = Color(0xFF1E8D53),
            stampBackground = Color(0xFFE6F7ED),
            stampText = Color(0xFF155E39)
        )

        ScoreBand.GOOD -> ScoreVisual(
            label = "Good",
            toneColor = Color(0xFF789C36),
            stampBackground = Color(0xFFEEF5DD),
            stampText = Color(0xFF4E6A1C)
        )

        ScoreBand.ACCEPTABLE -> ScoreVisual(
            label = "Acceptable",
            toneColor = Color(0xFFB79A1E),
            stampBackground = Color(0xFFF9F2D9),
            stampText = Color(0xFF7D6614)
        )

        ScoreBand.INCORRECT -> ScoreVisual(
            label = "Incorrect",
            toneColor = Color(0xFFBF7A17),
            stampBackground = Color(0xFFFBE9D6),
            stampText = Color(0xFF7A4A0C)
        )
    }
}
