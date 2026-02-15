package com.kitsune.kanji.japanese.flashcards.domain.scoring

const val SCORE_EXCELLENT_MIN = 80
const val SCORE_GOOD_MIN = 65
const val SCORE_ACCEPTABLE_MIN = 45
const val SCORE_REINFORCEMENT_CUTOFF = 50

enum class ScoreBand {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    INCORRECT
}

fun scoreBandFor(score: Int): ScoreBand {
    val normalized = score.coerceIn(0, 100)
    return when {
        normalized >= SCORE_EXCELLENT_MIN -> ScoreBand.EXCELLENT
        normalized >= SCORE_GOOD_MIN -> ScoreBand.GOOD
        normalized >= SCORE_ACCEPTABLE_MIN -> ScoreBand.ACCEPTABLE
        else -> ScoreBand.INCORRECT
    }
}

fun scoreBandLabel(score: Int): String {
    return when (scoreBandFor(score)) {
        ScoreBand.EXCELLENT -> "Excellent"
        ScoreBand.GOOD -> "Good"
        ScoreBand.ACCEPTABLE -> "Acceptable"
        ScoreBand.INCORRECT -> "Incorrect"
    }
}

fun requiresReinforcement(score: Int): Boolean {
    return score.coerceIn(0, 100) < SCORE_REINFORCEMENT_CUTOFF
}
