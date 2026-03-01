package com.kitsune.kanji.japanese.flashcards.ui.deck

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kitsune.kanji.japanese.flashcards.data.local.OnboardingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.domain.ink.HandwritingScorer
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.model.CardSubmission
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckResult
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckSession
import com.kitsune.kanji.japanese.flashcards.domain.scoring.applyAssistPenalty
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
    val latestFeedback: String? = null,
    val latestScore: Int? = null,
    val latestMatchedAnswer: String? = null,
    val latestCanonicalAnswer: String? = null,
    val latestIsCanonical: Boolean = true,
    val latestSubmissionToken: Long = 0L,
    val sessionTargetScore: Float = 82f,
    val deckResult: DeckResult? = null,
    val showGestureHelp: Boolean = true,
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
    private val handwritingScorer: HandwritingScorer,
    private val onboardingPreferences: OnboardingPreferences
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
                showGestureHelp = true,
                deckResult = null
            )
        }
        loadGestureHelpPreference()
        loadDeck(deckRunId)
    }

    fun autoInitialize(trackId: String) {
        if (deckRunId != null && _uiState.value.session != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                repository.initialize()
                repository.createOrLoadDailyDeck(trackId)
            }.onSuccess { deck ->
                initialize(deck.deckRunId)
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    fun dismissGestureHelp(neverShowAgain: Boolean) {
        viewModelScope.launch {
            if (neverShowAgain) {
                onboardingPreferences.setDeckHowToPlayDismissed(dismissed = true)
            }
            _uiState.update { it.copy(showGestureHelp = false) }
        }
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
            .filter { it.isNotBlank() }
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
                val reranked = rerankUnrevealedCards(
                    session = refreshed,
                    currentCardId = card.cardId
                )
                reranked to bestCandidate
            }.onSuccess { (session, handwriting) ->
                val adjustedScore = applyAssistPenalty(
                    score = handwriting.totalScore,
                    assistCount = activeAssists.size,
                    cardDifficulty = card.difficulty,
                    abilityLevel = estimateSessionAbility(session)
                )
                _uiState.update {
                    it.copy(
                        session = session,
                        currentIndex = resolveCurrentIndex(session.cards, currentVisibleIndex),
                        latestFeedback = handwriting.feedback,
                        latestScore = adjustedScore,
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
                repository.loadDeck(deckRunId) ?: error("Deck not found.")
            }.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        session = session,
                        currentIndex = resolveCurrentIndex(session.cards, it.currentIndex),
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
        val acceptedAnswers = (card.acceptedAnswers + card.canonicalAnswer)
            .distinct()
        val canonicalForms = comparableAnswerForms(card.canonicalAnswer)
        val acceptedFormsByAnswer = acceptedAnswers.associateWith(::comparableAnswerForms)
        val userForms = comparableAnswerForms(userAnswer)
        val matchedAccepted = acceptedAnswers.firstOrNull { accepted ->
            val acceptedForms = acceptedFormsByAnswer[accepted].orEmpty()
            userForms.any { it in acceptedForms }
        }
        val isAccepted = matchedAccepted != null
        val isCanonical = userForms.any { it in canonicalForms }
        val knowledgeScore = when {
            isCanonical -> 100
            isAccepted -> 90
            else -> 0
        }
        val feedback = when {
            isCanonical -> "Exact match."
            isAccepted -> "Accepted variant. Canonical answer: ${card.canonicalAnswer}"
            else -> "Incorrect. Canonical answer: ${card.canonicalAnswer}"
        }
        return CandidateScore(
            matchedAnswer = matchedAccepted ?: userAnswer,
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
            .replace(Regex("""[\s\u3000]+"""), "")
            .replace("’", "'")
            .replace("・", "")
            .replace("。", "")
            .replace("、", "")
            .replace(",", "")
            .lowercase()
    }

    private fun comparableAnswerForms(raw: String): Set<String> {
        val base = normalizeAnswer(raw)
        if (base.isBlank()) return emptySet()
        val forms = linkedSetOf(base)
        val hira = katakanaToHiragana(base)
        if (hira.isNotBlank()) {
            forms += hira
            if (isKanaOnly(hira)) {
                forms += normalizeAnswer(hiraganaToRomaji(hira))
            }
        }
        if (containsLatinLetters(base)) {
            val romajiHira = romanizedToHiragana(base)
            if (romajiHira.isNotBlank()) {
                forms += romajiHira
                forms += normalizeAnswer(hiraganaToRomaji(romajiHira))
            }
        }
        return forms.filter { it.isNotBlank() }.toSet()
    }

    private fun katakanaToHiragana(text: String): String {
        if (text.isBlank()) return text
        val builder = StringBuilder(text.length)
        text.forEach { ch ->
            builder.append(
                when (ch) {
                    in '\u30A1'..'\u30F6' -> (ch.code - 0x60).toChar()
                    '\u30FC' -> ch
                    else -> ch
                }
            )
        }
        return builder.toString()
    }

    private fun isKanaOnly(text: String): Boolean {
        return text.all { ch ->
            ch in '\u3040'..'\u309F' || ch == '\u30FC'
        }
    }

    private fun containsLatinLetters(text: String): Boolean {
        return text.any { it in 'a'..'z' || it in 'A'..'Z' }
    }

    private fun romanizedToHiragana(romaji: String): String {
        val normalized = romaji
            .lowercase()
            .replace("-", "")
            .replace(" ", "")
        if (normalized.isBlank()) return ""
        val result = StringBuilder()
        var index = 0
        while (index < normalized.length) {
            val current = normalized[index]
            val next = normalized.getOrNull(index + 1)

            if (
                next != null &&
                current == next &&
                current in "bcdfghjklmpqrstvwxyz" &&
                current != 'n'
            ) {
                result.append('っ')
                index += 1
                continue
            }

            if (current == 'n') {
                if (next == null) {
                    result.append('ん')
                    index += 1
                    continue
                }
                if (next == '\'') {
                    result.append('ん')
                    index += 2
                    continue
                }
                if (next == 'n') {
                    result.append('ん')
                    index += 1
                    continue
                }
                if (next !in "aeiouy") {
                    result.append('ん')
                    index += 1
                    continue
                }
            }

            val kanaEntry = ROMAJI_TO_HIRAGANA.firstOrNull { (latin, _) ->
                normalized.startsWith(latin, startIndex = index)
            }
            if (kanaEntry != null) {
                result.append(kanaEntry.second)
                index += kanaEntry.first.length
            } else {
                result.append(current)
                index += 1
            }
        }
        return result.toString()
    }

    private fun hiraganaToRomaji(hiragana: String): String {
        val normalized = katakanaToHiragana(hiragana)
        if (normalized.isBlank()) return ""
        val result = StringBuilder()
        var index = 0
        while (index < normalized.length) {
            val current = normalized[index]
            if (current == 'っ') {
                val nextPair = normalized.substring(index + 1).take(2)
                val nextSingle = normalized.getOrNull(index + 1)?.toString().orEmpty()
                val nextRomaji = HIRAGANA_TO_ROMAJI[nextPair] ?: HIRAGANA_TO_ROMAJI[nextSingle]
                if (!nextRomaji.isNullOrBlank()) {
                    result.append(nextRomaji.first())
                }
                index += 1
                continue
            }
            val pair = normalized.substring(index).take(2)
            val pairRomaji = HIRAGANA_TO_ROMAJI[pair]
            if (pairRomaji != null) {
                result.append(pairRomaji)
                index += 2
                continue
            }
            val single = HIRAGANA_TO_ROMAJI[current.toString()]
            if (single != null) {
                result.append(single)
            } else if (current != 'ー') {
                result.append(current)
            }
            index += 1
        }
        return result.toString()
    }

    private fun loadGestureHelpPreference() {
        viewModelScope.launch {
            val shouldShow = onboardingPreferences.shouldShowDeckHowToPlay()
            _uiState.update { it.copy(showGestureHelp = shouldShow) }
        }
    }

    companion object {
        fun factory(
            repository: KitsuneRepository,
            handwritingScorer: HandwritingScorer,
            onboardingPreferences: OnboardingPreferences
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DeckViewModel(
                    repository = repository,
                    handwritingScorer = handwritingScorer,
                    onboardingPreferences = onboardingPreferences
                )
            }
        }
    }
}

private val ROMAJI_TO_HIRAGANA = listOf(
    "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
    "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
    "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
    "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
    "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
    "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
    "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
    "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
    "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
    "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
    "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
    "shi" to "し", "chi" to "ち", "tsu" to "つ", "fu" to "ふ",
    "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
    "sa" to "さ", "su" to "す", "se" to "せ", "so" to "そ",
    "ta" to "た", "te" to "て", "to" to "と",
    "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "は", "hi" to "ひ", "he" to "へ", "ho" to "ほ",
    "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
    "ya" to "や", "yu" to "ゆ", "yo" to "よ",
    "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
    "wa" to "わ", "wo" to "を",
    "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
    "za" to "ざ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
    "da" to "だ", "de" to "で", "do" to "ど",
    "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
    "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
    "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お"
)

private val HIRAGANA_TO_ROMAJI = mapOf(
    "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
    "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
    "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
    "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
    "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
    "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
    "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
    "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
    "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
    "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
    "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
    "し" to "shi", "ち" to "chi", "つ" to "tsu", "ふ" to "fu",
    "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
    "さ" to "sa", "す" to "su", "せ" to "se", "そ" to "so",
    "た" to "ta", "て" to "te", "と" to "to",
    "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
    "は" to "ha", "ひ" to "hi", "へ" to "he", "ほ" to "ho",
    "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
    "や" to "ya", "ゆ" to "yu", "よ" to "yo",
    "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
    "わ" to "wa", "を" to "o", "ん" to "n",
    "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
    "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
    "だ" to "da", "で" to "de", "ど" to "do",
    "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
    "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
    "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o"
)

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

private fun estimateSessionAbility(session: DeckSession): Float {
    val answered = session.cards.filter { it.resultScore != null }
    if (answered.isEmpty()) return session.cards.map { it.difficulty }.average().toFloat().takeIf { !it.isNaN() } ?: 3f
    val avgDifficulty = answered.map { it.difficulty }.average().toFloat()
    val avgScore = answered.mapNotNull { it.resultScore }.average().toFloat()
    return (avgDifficulty + (avgScore - 68f) / 6f).coerceIn(1f, 12f)
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

    // Dynamic target: shift based on session performance to keep learner in
    // the zone of proximal development
    val baseTarget = when {
        observedAverage >= 90f -> 75f  // stretch the learner with harder cards
        observedAverage < 55f -> 88f   // ease off with simpler cards
        else -> 82f
    }

    // Momentum: check last 3 answered cards for streaks
    val recentScores = answered.takeLast(3).mapNotNull { it.resultScore }
    val momentumBonus = when {
        recentScores.size >= 3 && recentScores.all { it >= 80 } -> -5f  // all excellent → lower target (harder)
        recentScores.size >= 3 && recentScores.all { it < 45 } -> 5f   // all incorrect → raise target (easier)
        else -> 0f
    }
    val targetScore = (baseTarget + momentumBonus).coerceIn(70f, 92f)

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
        val fit = 100f - abs(predicted - targetScore)
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
