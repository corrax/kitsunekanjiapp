package com.kitsune.kanji.japanese.flashcards.ui.deck

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.PowerUpPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.ink.HandwritingScorer
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.model.CardSubmission
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckResult
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckSession
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeckUiState(
    val isLoading: Boolean = true,
    val deckRunId: String = "",
    val session: DeckSession? = null,
    val currentIndex: Int = 0,
    val powerUps: List<PowerUpInventory> = emptyList(),
    val activeHintCardId: String? = null,
    val activeHintText: String? = null,
    val activeHintReveal: LuckyHintReveal? = null,
    val latestFeedback: String? = null,
    val latestScore: Int? = null,
    val latestEffectiveScore: Int? = null,
    val latestMatchedAnswer: String? = null,
    val latestCanonicalAnswer: String? = null,
    val latestIsCanonical: Boolean = true,
    val latestSubmissionToken: Long = 0L,
    val sessionTargetScore: Float = 82f,
    val deckResult: DeckResult? = null,
    val errorMessage: String? = null
) {
    val activeCards: List<DeckCard>
        get() = session?.cards?.filter { it.resultScore == null }.orEmpty()
    val totalCardCount: Int
        get() = session?.cards?.size ?: 0
    val reviewedCardCount: Int
        get() = session?.cards?.count { it.resultScore != null } ?: 0
    val activeCardCount: Int
        get() = activeCards.size
    val currentCard: DeckCard?
        get() = activeCards.getOrNull(currentIndex)
}

data class LuckyHintReveal(
    val kanji: String,
    val mode: KanjiHintRevealMode
)

enum class KanjiHintRevealMode {
    TOP_STROKE_SLICE,
    TOP_LEFT_QUADRANT
}

class DeckViewModel(
    private val repository: KitsuneRepository,
    private val handwritingScorer: HandwritingScorer
) : ViewModel() {
    private var deckRunId: String? = null

    private val _uiState = MutableStateFlow(
        DeckUiState(
            isLoading = true,
            deckRunId = ""
        )
    )
    val uiState = _uiState.asStateFlow()

    fun initialize(deckRunId: String) {
        if (this.deckRunId == deckRunId && _uiState.value.session != null) {
            return
        }
        this.deckRunId = deckRunId
        _uiState.update {
            it.copy(
                deckRunId = deckRunId,
                currentIndex = 0,
                activeHintCardId = null,
                activeHintText = null,
                activeHintReveal = null,
                latestScore = null,
                latestFeedback = null,
                latestMatchedAnswer = null,
                latestCanonicalAnswer = null,
                latestIsCanonical = true,
                latestSubmissionToken = 0L,
                deckResult = null
            )
        }
        loadDeck(deckRunId)
    }

    fun goPrevious() {
        _uiState.update { state ->
            state.copy(currentIndex = (state.currentIndex - 1).coerceAtLeast(0))
        }
    }

    fun goNext() {
        _uiState.update { state ->
            val maxIndex = (state.activeCardCount - 1).coerceAtLeast(0)
            state.copy(currentIndex = (state.currentIndex + 1).coerceAtMost(maxIndex))
        }
    }

    fun submitCurrentCard(sample: InkSample) {
        submitCurrentCard(
            sample = sample,
            typedAnswer = null,
            selectedChoice = null,
            requestedAssists = emptyList()
        )
    }

    fun submitCurrentCard(
        sample: InkSample,
        typedAnswer: String?,
        selectedChoice: String?,
        requestedAssists: List<String>
    ) {
        val runId = deckRunId ?: return
        val card = _uiState.value.currentCard ?: return
        logDebug(
            "submit.start runId=$runId cardId=${card.cardId} type=${card.type} " +
                "strokes=${sample.strokes.size} points=${sample.totalPointCount()} " +
                "typed=${typedAnswer?.take(24)} choice=$selectedChoice assists=${requestedAssists.joinToString(",")}"
        )
        val currentVisibleIndex = _uiState.value.currentIndex
        val activeAssists = requestedAssists
            .distinct()
            .filterNot {
                it == PowerUpPreferences.POWER_UP_LUCKY_COIN ||
                    it == PowerUpPreferences.POWER_UP_KITSUNE_CHARM
            }
        viewModelScope.launch {
            runCatching {
                val bestCandidate = evaluateSubmission(
                    card = card,
                    sample = sample,
                    typedAnswer = typedAnswer,
                    selectedChoice = selectedChoice
                )
                val submission = CardSubmission(
                    cardId = card.cardId,
                    cardDifficulty = card.difficulty,
                    score = bestCandidate.totalScore,
                    handwritingScore = bestCandidate.handwritingScore,
                    knowledgeScore = bestCandidate.knowledgeScore,
                    matchedAnswer = bestCandidate.matchedAnswer,
                    canonicalAnswer = bestCandidate.canonicalAnswer,
                    isCanonicalMatch = bestCandidate.isCanonical,
                    requestedAssists = activeAssists,
                    strokeCount = sample.strokes.size,
                    strokePathsRaw = encodeInkSample(sample),
                    feedback = bestCandidate.feedback
                )
                logDebug(
                    "submit.payload cardId=${card.cardId} total=${submission.score} handwriting=${submission.handwritingScore} " +
                        "knowledge=${submission.knowledgeScore} matched=${submission.matchedAnswer} canonical=${submission.canonicalAnswer} " +
                        "isCanonical=${submission.isCanonicalMatch} strokeCount=${submission.strokeCount}"
                )
                repository.submitCard(deckRunId = runId, submission = submission)
                val refreshed = repository.loadDeck(runId) ?: error("Deck not found")
                val powerUps = repository.getPowerUps()
                val reranked = rerankUnrevealedCards(
                    session = refreshed,
                    currentCardId = card.cardId
                )
                Triple(reranked, bestCandidate, powerUps)
            }.onSuccess { (session, handwriting, powerUps) ->
                val effectiveScore = (handwriting.totalScore - (activeAssists.size * ASSIST_SCORE_PENALTY))
                    .coerceIn(0, 100)
                _uiState.update {
                    it.copy(
                        session = session,
                        currentIndex = resolveCurrentIndex(session.cards, currentVisibleIndex),
                        powerUps = powerUps,
                        activeHintCardId = null,
                        activeHintText = null,
                        activeHintReveal = null,
                        latestFeedback = handwriting.feedback,
                        latestScore = handwriting.totalScore,
                        latestEffectiveScore = effectiveScore,
                        latestMatchedAnswer = handwriting.matchedAnswer,
                        latestCanonicalAnswer = handwriting.canonicalAnswer,
                        latestIsCanonical = handwriting.isCanonical,
                        latestSubmissionToken = System.nanoTime(),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                logError("submit.failure cardId=${card.cardId} message=${error.message}", error)
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to submit card.")
                }
            }
        }
    }

    fun usePowerUp(powerUpId: String) {
        when (powerUpId) {
            PowerUpPreferences.POWER_UP_LUCKY_COIN -> useLuckyCoin()
            PowerUpPreferences.POWER_UP_KITSUNE_CHARM -> useKitsuneCharm()
            else -> return
        }
    }

    fun submitDeck() {
        val runId = deckRunId ?: return
        viewModelScope.launch {
            runCatching {
                repository.submitDeck(runId)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(deckResult = result, errorMessage = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to submit deck.")
                }
            }
        }
    }

    private fun loadDeck(deckRunId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val session = repository.loadDeck(deckRunId) ?: error("Deck not found.")
                val powerUps = repository.getPowerUps()
                session to powerUps
            }.onSuccess { (session, powerUps) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        currentIndex = resolveCurrentIndex(session.cards, it.currentIndex),
                        powerUps = powerUps,
                        activeHintCardId = null,
                        activeHintText = null,
                        activeHintReveal = null,
                        latestMatchedAnswer = null,
                        latestCanonicalAnswer = null,
                        latestIsCanonical = true,
                        deckResult = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load deck."
                    )
                }
            }
        }
    }

    private suspend fun evaluateSubmission(
        card: DeckCard,
        sample: InkSample,
        typedAnswer: String?,
        selectedChoice: String?
    ): CandidateScore {
        return when (card.type) {
            CardType.KANJI_WRITE -> evaluateHandwriting(card = card, sample = sample)
            CardType.KANJI_READING,
            CardType.GRAMMAR_CHOICE,
            CardType.SENTENCE_COMPREHENSION -> evaluateKnowledge(
                card = card,
                rawAnswer = selectedChoice ?: typedAnswer
            )
            CardType.GRAMMAR_CLOZE_WRITE,
            CardType.SENTENCE_BUILD,
            CardType.VOCAB_READING -> {
                if (sample.strokes.isNotEmpty() && prefersHandwriting(card)) {
                    evaluateHandwriting(card = card, sample = sample)
                } else {
                    evaluateKnowledge(card = card, rawAnswer = typedAnswer ?: selectedChoice)
                }
            }
        }
    }

    private suspend fun evaluateHandwriting(card: DeckCard, sample: InkSample): CandidateScore {
        if (sample.strokes.isEmpty()) {
            error("Write an answer before submitting.")
        }
        val template = repository.loadTemplate(card.templateId) ?: error("Missing template")
        val candidateScores = card.acceptedAnswers
            .ifEmpty { listOf(card.canonicalAnswer) }
            .distinct()
            .mapNotNull { candidate ->
                val candidateTemplate = if (candidate == card.canonicalAnswer) {
                    template
                } else {
                    repository.loadTemplateForTarget(candidate)
                } ?: return@mapNotNull null
                val handwriting = handwritingScorer.score(sample, candidateTemplate)
                logDebug(
                    "handwriting.candidate cardId=${card.cardId} candidate=$candidate recognized=${handwriting.recognizedText} " +
                        "score=${handwriting.score} feedback=${handwriting.feedback}"
                )
                scoreBinaryHandwritingCandidate(
                    recognizedText = handwriting.recognizedText,
                    matchedAnswer = candidate,
                    canonicalAnswer = card.canonicalAnswer,
                    handwritingScore = handwriting.score,
                    baseFeedback = handwriting.feedback
                )
            }
        val best = candidateScores.maxByOrNull { it.totalScore }
            ?: error("No scoring template available")
        logDebug(
            "handwriting.best cardId=${card.cardId} matched=${best.matchedAnswer} total=${best.totalScore} " +
                "handwriting=${best.handwritingScore} canonical=${best.canonicalAnswer} isCanonical=${best.isCanonical}"
        )
        return best
    }

    private fun evaluateKnowledge(card: DeckCard, rawAnswer: String?): CandidateScore {
        val userAnswer = rawAnswer.orEmpty().trim()
        if (userAnswer.isBlank()) {
            error("Answer is required before submit.")
        }
        val normalizedCanonical = normalizeAnswer(card.canonicalAnswer)
        val normalizedAccepted = card.acceptedAnswers
            .ifEmpty { listOf(card.canonicalAnswer) }
            .map { normalizeAnswer(it) }
            .toSet()
        val normalizedUser = normalizeAnswer(userAnswer)
        val isAccepted = normalizedUser in normalizedAccepted
        val isCanonical = normalizedUser == normalizedCanonical
        val knowledgeScore = when {
            isCanonical -> 100
            isAccepted -> 90
            else -> 34
        }
        val feedback = when {
            isCanonical -> "Exact match."
            isAccepted -> "Accepted variant. Canonical answer: ${card.canonicalAnswer}"
            else -> "Incorrect. Canonical answer: ${card.canonicalAnswer}"
        }
        return CandidateScore(
            matchedAnswer = userAnswer,
            canonicalAnswer = card.canonicalAnswer,
            isCanonical = isCanonical,
            handwritingScore = if (isAccepted) 100 else 0,
            knowledgeScore = knowledgeScore,
            totalScore = knowledgeScore,
            feedback = feedback
        )
    }

    private fun prefersHandwriting(card: DeckCard): Boolean {
        return card.acceptedAnswers
            .ifEmpty { listOf(card.canonicalAnswer) }
            .all { answer -> answer.length == 1 && answer.first().code > 0x3000 }
    }

    private fun normalizeAnswer(raw: String): String {
        return raw.trim()
            .replace(" ", "")
            .replace("\u3000", "")
            .lowercase()
    }

    private fun useLuckyCoin() {
        val state = _uiState.value
        val card = state.currentCard ?: return
        viewModelScope.launch {
            runCatching {
                val consumed = repository.consumePowerUp(PowerUpPreferences.POWER_UP_LUCKY_COIN)
                if (!consumed) error("No Lucky Coin available.")
                val powerUps = repository.getPowerUps()
                powerUps to luckyHintFor(card)
            }.onSuccess { (powerUps, hintPayload) ->
                _uiState.update {
                    it.copy(
                        powerUps = powerUps,
                        activeHintCardId = card.cardId,
                        activeHintText = hintPayload.text,
                        activeHintReveal = hintPayload.reveal,
                        latestFeedback = "Lucky Coin used. Hint revealed.",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to use Lucky Coin.")
                }
            }
        }
    }

    private fun useKitsuneCharm() {
        val runId = deckRunId ?: return
        val state = _uiState.value
        val session = state.session ?: return
        val currentCard = state.currentCard ?: return
        val currentDeckIndex = session.cards.indexOfFirst { it.cardId == currentCard.cardId }
        if (currentDeckIndex < 0) return

        viewModelScope.launch {
            runCatching {
                val consumed = repository.consumePowerUp(PowerUpPreferences.POWER_UP_KITSUNE_CHARM)
                if (!consumed) error("No Kitsune Charm available.")
                val targetIndex = pickSwapTargetIndex(session.cards, currentCard.cardId)
                    ?: error("No unanswered card available to swap in.")
                val swapped = swapCards(session.cards, currentDeckIndex, targetIndex)
                repository.reorderDeckCards(
                    deckRunId = runId,
                    cardIdsInOrder = swapped.map { it.cardId }
                )
                val refreshed = repository.loadDeck(runId) ?: error("Deck not found")
                val powerUps = repository.getPowerUps()
                refreshed to powerUps
            }.onSuccess { (refreshedSession, powerUps) ->
                _uiState.update {
                    it.copy(
                        session = refreshedSession,
                        currentIndex = resolveCurrentIndex(refreshedSession.cards, it.currentIndex),
                        powerUps = powerUps,
                        activeHintCardId = null,
                        activeHintText = null,
                        activeHintReveal = null,
                        latestFeedback = "Kitsune Charm swapped this question.",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to use Kitsune Charm.")
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            handwritingScorer: HandwritingScorer
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeckViewModel(
                    repository = repository,
                    handwritingScorer = handwritingScorer
                )
            }
        }
    }
}

private const val ASSIST_SCORE_PENALTY = 12

private data class CandidateScore(
    val matchedAnswer: String,
    val canonicalAnswer: String,
    val isCanonical: Boolean,
    val handwritingScore: Int,
    val knowledgeScore: Int,
    val totalScore: Int,
    val feedback: String
)

private fun scoreBinaryHandwritingCandidate(
    recognizedText: String?,
    matchedAnswer: String,
    canonicalAnswer: String,
    handwritingScore: Int,
    baseFeedback: String
): CandidateScore {
    val normalizedHandwriting = handwritingScore.coerceIn(0, 100)
    val isRecognizedMatch = normalizedHandwriting > 0
    val isCanonical = matchedAnswer == canonicalAnswer
    val knowledgeScore = when {
        !isRecognizedMatch -> 0
        isCanonical -> 100
        else -> 90
    }
    val total = if (isRecognizedMatch) normalizedHandwriting else 0
    val recognizedAnswer = recognizedText
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    val answerFeedback = when {
        !isRecognizedMatch && recognizedAnswer != null ->
            "Recognized \"$recognizedAnswer\", which is not an accepted answer."
        !isRecognizedMatch ->
            "Could not recognize an accepted kanji."
        isCanonical ->
            "Accepted answer."
        else ->
            "Accepted alternate answer."
    }
    val storedAnswer = when {
        isRecognizedMatch -> matchedAnswer
        recognizedAnswer != null -> recognizedAnswer
        else -> matchedAnswer
    }
    return CandidateScore(
        matchedAnswer = storedAnswer,
        canonicalAnswer = canonicalAnswer,
        isCanonical = isCanonical && isRecognizedMatch,
        handwritingScore = normalizedHandwriting,
        knowledgeScore = knowledgeScore,
        totalScore = total,
        feedback = "$baseFeedback $answerFeedback".trim()
    )
}

private fun pickSwapTargetIndex(cards: List<DeckCard>, currentCardId: String): Int? {
    val currentIndex = cards.indexOfFirst { it.cardId == currentCardId }
    if (currentIndex < 0) return null
    val current = cards[currentIndex]
    val candidates = cards.withIndex()
        .filter { indexed ->
            indexed.index > currentIndex &&
                indexed.value.resultScore == null &&
                indexed.value.cardId != current.cardId
        }
    if (candidates.isEmpty()) return null
    val answeredAverage = cards.mapNotNull { it.resultScore }
        .average()
        .toFloat()
        .takeIf { !it.isNaN() } ?: 78f
    return if (answeredAverage >= 90f) {
        candidates.maxByOrNull { it.value.difficulty }?.index
    } else {
        candidates.minByOrNull { indexed ->
            abs(indexed.value.difficulty - current.difficulty)
        }?.index
    }
}

private fun swapCards(cards: List<DeckCard>, first: Int, second: Int): List<DeckCard> {
    if (first !in cards.indices || second !in cards.indices || first == second) return cards
    val mutable = cards.toMutableList()
    val temp = mutable[first]
    mutable[first] = mutable[second]
    mutable[second] = temp
    return mutable.mapIndexed { index, card ->
        card.copy(position = index + 1)
    }
}

private fun rerankUnrevealedCards(session: DeckSession, currentCardId: String?): DeckSession {
    if (session.cards.isEmpty()) return session
    val answered = session.cards.filter { it.resultScore != null }
    val observedAverage = answered.mapNotNull { it.resultScore }.average().toFloat().takeIf { !it.isNaN() } ?: 78f
    val difficultyAnchor = if (answered.isEmpty()) {
        session.cards.maxOfOrNull { it.difficulty }?.toFloat() ?: 1f
    } else {
        answered.map { it.difficulty }.average().toFloat()
    }
    val currentIndex = currentCardId
        ?.let { cardId -> session.cards.indexOfFirst { it.cardId == cardId } }
        ?.takeIf { it >= 0 }
        ?: session.cards.indexOfFirst { it.resultScore == null }.coerceAtLeast(0)
    val lockUntil = (currentIndex + 2).coerceAtMost(session.cards.size)
    val prefix = session.cards.take(lockUntil)
    val unrevealed = session.cards.drop(lockUntil).filter { it.resultScore == null }
    val alreadyScoredTail = session.cards.drop(lockUntil).filter { it.resultScore != null }
    val sortedTail = unrevealed.sortedByDescending { card ->
        val predicted = (observedAverage - ((card.difficulty - difficultyAnchor) * 7f)).coerceIn(0f, 100f)
        val fit = 100f - abs(predicted - 82f)
        val challengeBonus = if (observedAverage >= 90f && card.difficulty > difficultyAnchor) 8f else 0f
        fit + challengeBonus
    }
    val recomposed = prefix + sortedTail + alreadyScoredTail
    val reindexed = recomposed.mapIndexed { index, card ->
        card.copy(position = index + 1)
    }
    return session.copy(cards = reindexed)
}

private fun resolveCurrentIndex(cards: List<DeckCard>, preferredIndex: Int): Int {
    val unansweredCount = cards.count { it.resultScore == null }
    if (unansweredCount <= 0) return 0
    return preferredIndex.coerceIn(0, unansweredCount - 1)
}

private data class LuckyHintPayload(
    val text: String,
    val reveal: LuckyHintReveal? = null
)

private fun luckyHintFor(card: DeckCard): LuckyHintPayload {
    return when (card.type) {
        CardType.KANJI_WRITE -> {
            val answer = card.canonicalAnswer
            val revealKanji = answer.firstOrNull()?.toString()
            val revealMode = revealKanji
                ?.firstOrNull()
                ?.let { codepoint ->
                    if (codepoint.code % 2 == 0) {
                        KanjiHintRevealMode.TOP_STROKE_SLICE
                    } else {
                        KanjiHintRevealMode.TOP_LEFT_QUADRANT
                    }
                }
            val text = if (answer.length > 1) {
                "Hint: partial reveal is shown for the first kanji."
            } else {
                "Hint: partial kanji reveal shown below."
            }
            LuckyHintPayload(
                text = text,
                reveal = if (revealKanji != null && revealMode != null) {
                    LuckyHintReveal(kanji = revealKanji, mode = revealMode)
                } else {
                    null
                }
            )
        }

        CardType.GRAMMAR_CHOICE,
        CardType.KANJI_READING,
        CardType.SENTENCE_COMPREHENSION,
        CardType.VOCAB_READING -> {
            val answer = card.canonicalAnswer
            LuckyHintPayload(
                text = "Hint: answer starts with \"${answer.take(1)}\" and has ${answer.length} character(s)."
            )
        }

        else -> {
            val answer = card.canonicalAnswer
            LuckyHintPayload(
                text = "Hint: answer starts with \"${answer.take(1)}\"."
            )
        }
    }
}

private fun encodeInkSample(sample: InkSample): String? {
    if (sample.strokes.isEmpty()) return null
    return sample.strokes.joinToString("|") { stroke ->
        stroke.points.joinToString(";") { point ->
            "${point.x},${point.y}"
        }
    }.ifBlank { null }
}

private fun InkSample.totalPointCount(): Int {
    return strokes.sumOf { it.points.size }
}

private fun logDebug(message: String) {
    if (DECK_DEBUG_LOGS) {
        Log.d(DECK_LOG_TAG, message)
    }
}

private fun logError(message: String, throwable: Throwable) {
    if (DECK_DEBUG_LOGS) {
        Log.e(DECK_LOG_TAG, message, throwable)
    }
}

private const val DECK_LOG_TAG = "KitsuneDeckScore"
private const val DECK_DEBUG_LOGS = true
