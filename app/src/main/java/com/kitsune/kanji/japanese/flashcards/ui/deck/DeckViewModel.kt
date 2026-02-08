package com.kitsune.kanji.japanese.flashcards.ui.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
                latestIsCanonical = true
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
        submitCurrentCard(sample = sample, requestedAssists = emptyList())
    }

    fun submitCurrentCard(sample: InkSample, requestedAssists: List<String>) {
        val runId = deckRunId ?: return
        val card = _uiState.value.currentCard ?: return
        val activeAssists = requestedAssists.distinct()
        viewModelScope.launch {
            runCatching {
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
                        scoreCandidate(
                            matchedAnswer = candidate,
                            canonicalAnswer = card.canonicalAnswer,
                            handwritingScore = handwriting.score,
                            baseFeedback = handwriting.feedback
                        )
                    }
                val bestCandidate = candidateScores.maxByOrNull { it.totalScore }
                    ?: error("No scoring template available")
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
                        latestIsCanonical = true
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

private fun scoreCandidate(
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
