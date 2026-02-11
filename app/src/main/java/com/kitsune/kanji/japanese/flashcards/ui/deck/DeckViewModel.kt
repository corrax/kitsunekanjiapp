package com.kitsune.kanji.japanese.flashcards.ui.deck

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
import kotlin.math.roundToInt
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
    val currentCard: DeckCard?
        get() = session?.cards?.getOrNull(currentIndex)
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
            val maxIndex = (state.session?.cards?.lastIndex ?: 0).coerceAtLeast(0)
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
        val activeAssists = requestedAssists
            .distinct()
            .filterNot { it == PowerUpPreferences.POWER_UP_LUCKY_COIN }
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
                    feedback = bestCandidate.feedback
                )
                repository.submitCard(deckRunId = runId, submission = submission)
                val refreshed = repository.loadDeck(runId) ?: error("Deck not found")
                val powerUps = repository.getPowerUps()
                val reranked = rerankUnrevealedCards(
                    session = refreshed,
                    currentIndex = _uiState.value.currentIndex
                )
                Triple(reranked, bestCandidate, powerUps)
            }.onSuccess { (session, handwriting, powerUps) ->
                val effectiveScore = (handwriting.totalScore - (activeAssists.size * ASSIST_SCORE_PENALTY))
                    .coerceIn(0, 100)
                _uiState.update {
                    it.copy(
                        session = session,
                        powerUps = powerUps,
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
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "Failed to submit card.")
                }
            }
        }
    }

    fun usePowerUp(powerUpId: String) {
        if (powerUpId != PowerUpPreferences.POWER_UP_LUCKY_COIN) {
            return
        }
        val runId = deckRunId ?: return
        val state = _uiState.value
        val session = state.session ?: return
        val currentIndex = state.currentIndex
        if (session.cards.isEmpty() || currentIndex !in session.cards.indices) {
            return
        }

        viewModelScope.launch {
            runCatching {
                val consumed = repository.consumePowerUp(powerUpId)
                if (!consumed) error("No Lucky Coin available.")

                val targetIndex = pickRerollTargetIndex(session.cards, currentIndex)
                    ?: error("No alternative card to reroll into.")
                val swapped = swapCards(session.cards, currentIndex, targetIndex)
                repository.reorderDeckCards(
                    deckRunId = runId,
                    cardIdsInOrder = swapped.map { it.cardId }
                )
                val refreshed = repository.loadDeck(runId) ?: error("Deck not found")
                val reranked = rerankUnrevealedCards(refreshed, currentIndex)
                val inventory = repository.getPowerUps()
                reranked to inventory
            }.onSuccess { (rerolledSession, powerUps) ->
                _uiState.update {
                    it.copy(
                        session = rerolledSession,
                        powerUps = powerUps,
                        latestFeedback = "Lucky Coin rerolled this card.",
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
                        powerUps = powerUps,
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
                scoreHandwritingCandidate(
                    matchedAnswer = candidate,
                    canonicalAnswer = card.canonicalAnswer,
                    handwritingScore = handwriting.score,
                    baseFeedback = handwriting.feedback
                )
            }
        return candidateScores.maxByOrNull { it.totalScore }
            ?: error("No scoring template available")
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
            else -> "Needs reinforcement. Canonical answer: ${card.canonicalAnswer}"
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
            .replace("　", "")
            .lowercase()
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

private fun scoreHandwritingCandidate(
    matchedAnswer: String,
    canonicalAnswer: String,
    handwritingScore: Int,
    baseFeedback: String
): CandidateScore {
    val isCanonical = matchedAnswer == canonicalAnswer
    val knowledgeScore = if (isCanonical) 100 else 82
    val total = ((handwritingScore * 0.8f) + (knowledgeScore * 0.2f))
        .roundToInt()
        .coerceIn(0, 100)
    val answerFeedback = if (isCanonical) {
        "Canonical match."
    } else {
        "Accepted variant. JLPT canonical: $canonicalAnswer"
    }
    return CandidateScore(
        matchedAnswer = matchedAnswer,
        canonicalAnswer = canonicalAnswer,
        isCanonical = isCanonical,
        handwritingScore = handwritingScore,
        knowledgeScore = knowledgeScore,
        totalScore = total,
        feedback = "$baseFeedback $answerFeedback"
    )
}

private fun pickRerollTargetIndex(cards: List<DeckCard>, currentIndex: Int): Int? {
    val current = cards.getOrNull(currentIndex) ?: return null
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

private fun rerankUnrevealedCards(session: DeckSession, currentIndex: Int): DeckSession {
    if (session.cards.isEmpty()) return session
    val answered = session.cards.filter { it.resultScore != null }
    val observedAverage = answered.mapNotNull { it.resultScore }.average().toFloat().takeIf { !it.isNaN() } ?: 78f
    val difficultyAnchor = if (answered.isEmpty()) {
        session.cards.maxOfOrNull { it.difficulty }?.toFloat() ?: 1f
    } else {
        answered.map { it.difficulty }.average().toFloat()
    }
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
