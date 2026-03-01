package com.kitsune.kanji.japanese.flashcards.domain.scoring

const val SCORE_EXCELLENT_MIN = 80
const val SCORE_GOOD_MIN = 65
const val SCORE_OK_MIN = 45
const val SCORE_REINFORCEMENT_CUTOFF = 50
private const val ASSIST_SCORE_REDUCTION_FACTOR = 0.8f

enum class ScoreBand {
    EXCELLENT,
    GOOD,
    OK,
    INCORRECT
}

fun scoreBandFor(score: Int): ScoreBand {
    val normalized = score.coerceIn(0, 100)
    return when {
        normalized >= SCORE_EXCELLENT_MIN -> ScoreBand.EXCELLENT
        normalized >= SCORE_GOOD_MIN -> ScoreBand.GOOD
        normalized >= SCORE_OK_MIN -> ScoreBand.OK
        else -> ScoreBand.INCORRECT
    }
}

fun scoreBandLabel(score: Int): String {
    return when (scoreBandFor(score)) {
        ScoreBand.EXCELLENT -> "Excellent"
        ScoreBand.GOOD -> "Good"
        ScoreBand.OK -> "Ok"
        ScoreBand.INCORRECT -> "Incorrect"
    }
}

fun requiresReinforcement(score: Int): Boolean {
    return score.coerceIn(0, 100) < SCORE_REINFORCEMENT_CUTOFF
}

fun applyAssistPenalty(score: Int, assistCount: Int): Int {
    val normalized = score.coerceIn(0, 100)
    if (assistCount <= 0) return normalized
    val penalized = (normalized * ASSIST_SCORE_REDUCTION_FACTOR).toInt()
    return if (normalized >= SCORE_OK_MIN && penalized < SCORE_OK_MIN) {
        SCORE_OK_MIN
    } else {
        penalized.coerceIn(0, 100)
    }
}

/**
 * Difficulty-aware assist penalty. When a card is much harder than the
 * learner's ability, the penalty is lighter (needing help is expected).
 * When the card is easy relative to ability, the penalty is steeper.
 */
fun applyAssistPenalty(score: Int, assistCount: Int, cardDifficulty: Int, abilityLevel: Float): Int {
    val normalized = score.coerceIn(0, 100)
    if (assistCount <= 0) return normalized
    val gap = cardDifficulty - abilityLevel
    val factor = when {
        gap > 2f -> 0.90f   // hard card: only 10% penalty
        gap < -2f -> 0.70f  // easy card: 30% penalty
        else -> ASSIST_SCORE_REDUCTION_FACTOR // matched: 20% penalty
    }
    val penalized = (normalized * factor).toInt()
    return if (normalized >= SCORE_OK_MIN && penalized < SCORE_OK_MIN) {
        SCORE_OK_MIN
    } else {
        penalized.coerceIn(0, 100)
    }
}
