package com.kitsune.kanji.japanese.flashcards.ui.deck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkPoint
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkStroke
import com.kitsune.kanji.japanese.flashcards.domain.scoring.ScoreBand
import com.kitsune.kanji.japanese.flashcards.domain.scoring.scoreBandFor
import com.kitsune.kanji.japanese.flashcards.ui.common.scoreVisualFor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val CARD_ASSIST_ID = "card_assist"

@Composable
fun DeckScreen(
    state: DeckUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmitCard: (InkSample, String?, String?, List<String>) -> Unit,
    onDeckSubmitted: (String) -> Unit,
    onSubmitDeck: () -> Unit,
    onDismissGestureOverlay: (Boolean) -> Unit,
    onAssistEnabled: () -> Unit = {}
) {
    val currentCard = state.currentCard
    val assistEnabled = currentCard?.cardId != null && currentCard.cardId in state.assistEnabledCardIds
    var currentSample by remember(currentCard?.cardId) { mutableStateOf(InkSample(emptyList())) }
    var typedAnswer by remember(currentCard?.cardId) { mutableStateOf("") }
    var selectedChoice by remember(currentCard?.cardId) { mutableStateOf<String?>(null) }
    var wordBankSelection by remember(currentCard?.cardId) { mutableStateOf(listOf<Int>()) }
    var canvasResetCounter by remember(currentCard?.cardId) { mutableIntStateOf(0) }
    var showAssistConfirm by rememberSaveable(state.deckRunId, currentCard?.cardId) { mutableStateOf(false) }
    var showLeaveConfirm by rememberSaveable(state.deckRunId) { mutableStateOf(false) }
    var cardDragX by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }
    var cardDragY by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }
    var scoreBurst by remember(state.deckRunId) { mutableStateOf<ScoreBurstData?>(null) }
    val scoredCards = state.session?.cards?.mapNotNull { it.resultScore }.orEmpty()
    val reviewedCount = state.reviewedCardCount
    val totalCards = state.totalCardCount
    val remainingCount = state.activeCardCount
    val cardPosition = if (remainingCount == 0) 0 else (state.currentIndex + 1).coerceAtMost(remainingCount)
    val totalPercent = scoredCards.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    val totalBandScore = totalPercent.roundToInt()
    val totalColor = scoreVisualFor(totalBandScore).toneColor
    val assistHintPayload = remember(currentCard?.cardId, assistEnabled) {
        if (assistEnabled && currentCard != null) {
            assistHintFor(currentCard)
        } else {
            null
        }
    }
    val isChoiceCard = currentCard?.choices?.isNotEmpty() == true &&
        currentCard.type in setOf(
            CardType.KANJI_READING,
            CardType.VOCAB_READING,
            CardType.GRAMMAR_CHOICE,
            CardType.SENTENCE_COMPREHENSION
        )
    val usesHandwritingPad = currentCard?.type == CardType.KANJI_WRITE
    val canSubmit = when {
        usesHandwritingPad -> currentSample.strokes.isNotEmpty()
        isChoiceCard -> !selectedChoice.isNullOrBlank()
        else -> typedAnswer.isNotBlank()
    }
    val visibleChoices = remember(currentCard?.cardId, assistEnabled) {
        val card = currentCard ?: return@remember emptyList()
        val choices = card.choices
        if (
            assistEnabled &&
            choices.size >= 3 &&
            card.canonicalAnswer in choices
        ) {
            val distractor = choices.firstOrNull { it != card.canonicalAnswer }
            listOfNotNull(card.canonicalAnswer, distractor)
        } else {
            choices
        }
    }
    LaunchedEffect(visibleChoices, selectedChoice) {
        if (selectedChoice != null && selectedChoice !in visibleChoices) {
            selectedChoice = null
        }
    }
    LaunchedEffect(state.latestSubmissionToken) {
        if (state.latestSubmissionToken == 0L) return@LaunchedEffect
        val latestScore = state.latestScore ?: return@LaunchedEffect
        val burst = ScoreBurstData(
            token = state.latestSubmissionToken,
            score = latestScore
        )
        scoreBurst = burst
        delay(1300)
        if (scoreBurst?.token == burst.token) {
            scoreBurst = null
        }
    }
    LaunchedEffect(state.deckResult?.deckRunId) {
        state.deckResult?.deckRunId?.let { runId ->
            onDeckSubmitted(runId)
        }
    }
    val allGraded = currentCard == null && totalCards > 0 && reviewedCount >= totalCards
    LaunchedEffect(allGraded) {
        if (allGraded) {
            onSubmitDeck()
        }
    }

    fun submitCurrentCard() {
        if (!canSubmit) return
        onSubmitCard(
            currentSample,
            typedAnswer.takeIf { it.isNotBlank() },
            selectedChoice,
            if (assistEnabled) listOf(CARD_ASSIST_ID) else emptyList()
        )
        canvasResetCounter += 1
        currentSample = InkSample(emptyList())
        typedAnswer = ""
        selectedChoice = null
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave deck?") },
            text = { Text("Your progress is saved. You can resume from where you left off.") },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveConfirm = false
                    onBack()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("Stay")
                }
            }
        )
    }

    val (bgImage, bgTint) = remember(state.session?.deckType, state.session?.sourceId) {
        themedDeckBackground(state.session?.deckType, state.session?.sourceId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgTint)
    ) {
        Image(
            painter = painterResource(bgImage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.18f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0x88FFFFFF), Color(0xD9FFFFFF)))
                )
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        if (currentCard == null) {
            if (totalCards > 0 && reviewedCount >= totalCards) {
                // Navigate to report is triggered by LaunchedEffect; show loading while transitioning
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No cards found for this deck.",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xF2FFFFFF))
                        .border(1.dp, Color(0xFFD9B695), CircleShape)
                        .clickable { showLeaveConfirm = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave deck",
                        tint = Color(0xFF6D5444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF2FFFFFF))
                        .border(1.dp, Color(0xFFD9B695), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Card $cardPosition/$remainingCount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF322117)
                    )
                    Text(
                        text = "$remainingCount remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6D5444)
                    )
                    Text(
                        text = "Total ${formatPercent(totalPercent)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = totalColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
                DeckActionPill(
                    text = "Finish",
                    icon = Icons.Filled.DoneAll,
                    onClick = onSubmitDeck,
                    modifier = Modifier
                )
            }

            CardStackFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                dragX = cardDragX,
                dragY = cardDragY,
                onDrag = { delta ->
                    cardDragX = (cardDragX + delta.x).coerceIn(-280f, 280f)
                    cardDragY = (cardDragY + delta.y).coerceIn(-280f, 220f)
                },
                onDragEnd = {
                    when {
                        cardDragY < -150f && abs(cardDragY) >= abs(cardDragX) -> submitCurrentCard()
                        cardDragX > 150f -> onPrevious()
                        cardDragX < -150f -> onNext()
                    }
                    cardDragX = 0f
                    cardDragY = 0f
                },
                onDragCancel = {
                    cardDragX = 0f
                    cardDragY = 0f
                },
                overlay = {
                    scoreBurst?.let { burst ->
                        ScoreBurstOverlay(
                            burst = burst,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 18.dp, vertical = 24.dp)
                        )
                    }
                }
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    val padMinSize = 180.dp
                    val padMaxSize = 360.dp
                    val availableWidth = maxWidth
                    val padMaxByHeight = (maxHeight * 0.58f).coerceAtLeast(padMinSize)
                    val targetPadMax = minOf(padMaxSize, padMaxByHeight)
                    val boundedWidth = availableWidth.coerceIn(padMinSize, targetPadMax)
                    val padSize = if (availableWidth < padMinSize) availableWidth else boundedWidth

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = when (state.session?.deckType) {
                                        DeckType.EXAM -> "Exam Pack"
                                        DeckType.DAILY -> "Daily Deck"
                                        DeckType.REMEDIAL -> "Remedial Deck"
                                        null -> ""
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = promptLabelFor(currentCard.type),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val promptFurigana = currentCard.promptFurigana
                                val shouldRenderFurigana = promptFurigana != null &&
                                    shouldShowFurigana(cardDifficulty = currentCard.difficulty)
                                val plainJapanesePrompt = promptFurigana?.let(::stripFuriganaMarkup)
                                if (shouldRenderFurigana) {
                                    FuriganaText(
                                        text = promptFurigana,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (shouldShowRomanizedPrompt(prompt = currentCard.prompt, japanesePrompt = plainJapanesePrompt)) {
                                        Text(
                                            text = currentCard.prompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF6A5A48)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = plainJapanesePrompt ?: currentCard.prompt,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = instructionFor(currentCard.type),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6A5A48)
                                )
                                if (assistHintPayload != null) {
                                    Text(
                                        text = assistHintPayload.text,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF7A4F34),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (assistHintPayload?.reveal != null) {
                                    LuckyKanjiRevealHint(
                                        reveal = assistHintPayload.reveal,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (!assistHintPayload?.patternHint.isNullOrBlank()) {
                                    Text(
                                        text = assistHintPayload?.patternHint.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF6A5A48)
                                    )
                                }
                                if (assistHintPayload?.wordBank?.isNotEmpty() == true) {
                                    SentenceWordBankHint(
                                        chunks = assistHintPayload.wordBank,
                                        selectedIndices = wordBankSelection.toSet(),
                                        showFurigana = assistHintPayload.showFurigana,
                                        showRomaji = assistHintPayload.showRomaji,
                                        onChunkToggled = { index ->
                                            val updated = toggleWordBankSelection(wordBankSelection, index)
                                            wordBankSelection = updated
                                            applyWordBankSelection(
                                                selection = updated,
                                                chunks = assistHintPayload.wordBank,
                                                onAnswerUpdated = { typedAnswer = it }
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (currentCard.isRetryQueued) {
                                    Text(
                                        text = "Practice card",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF8A4E2C),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (usesHandwritingPad) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                InkPad(
                                    resetCounter = canvasResetCounter,
                                    onSampleChanged = { sample -> currentSample = sample },
                                    modifier = Modifier
                                        .size(padSize)
                                        .aspectRatio(1f)
                                )
                            }
                        } else if (isChoiceCard) {
                            ChoiceAnswerPanel(
                                choices = visibleChoices,
                                selectedChoice = selectedChoice,
                                onChoiceSelected = { selectedChoice = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp)
                            )
                        } else {
                            OutlinedTextField(
                                value = typedAnswer,
                                onValueChange = { typedAnswer = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Type your answer") }
                            )
                        }

                        state.latestMatchedAnswer?.let { matched ->
                            Text(
                                text = if (state.latestIsCanonical) {
                                    "Your answer matched: $matched"
                                } else {
                                    "Accepted answer matched: $matched"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (!state.latestIsCanonical) {
                            state.latestCanonicalAnswer?.let { canonical ->
                                Text(
                                    text = "Expected answer: $canonical",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6E4331)
                                )
                            }
                        }
                        state.latestFeedback?.let { feedback ->
                            Text(
                                text = feedback,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        state.errorMessage?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistFooter(
                    enabled = assistEnabled,
                    onActivate = { showAssistConfirm = true },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { submitCurrentCard() },
                    enabled = canSubmit,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20A557),
                        disabledContainerColor = Color(0xFFB5D5C1)
                    ),
                    modifier = Modifier
                        .height(64.dp)
                        .width(102.dp)
                ) {
                    Text(
                        text = "Submit",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

        }

        if (state.showGestureHelp) {
            GestureHelpOverlay(
                heroImageRes = bgImage,
                deckLabel = when (state.session?.deckType) {
                    DeckType.EXAM -> "Exam Pack"
                    DeckType.DAILY -> "Daily Deck"
                    DeckType.REMEDIAL -> "Remedial Deck"
                    null -> "Deck"
                },
                onDismiss = onDismissGestureOverlay
            )
        }

        if (showAssistConfirm) {
            AlertDialog(
                onDismissRequest = { showAssistConfirm = false },
                title = { Text("Use Hint For This Card?") },
                text = {
                    Text(
                        "Using assist reveals a hint and applies a 20% score reduction " +
                            "(never below the Ok threshold)."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAssistEnabled()
                            showAssistConfirm = false
                        }
                    ) {
                        Text("Use Hint")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAssistConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private data class ScoreBurstData(
    val token: Long,
    val score: Int
)

private data class AssistHintPayload(
    val text: String,
    val reveal: LuckyHintReveal? = null,
    val patternHint: String? = null,
    val wordBank: List<SentenceChunkHint> = emptyList(),
    val showFurigana: Boolean = false,
    val showRomaji: Boolean = false
)

private data class SentenceChunkHint(
    val jp: String,
    val kana: String?,
    val romaji: String?
)

private enum class ScoreBurstTier {
    INCORRECT,
    OK,
    GOOD,
    EXCELLENT
}

@Composable
private fun ScoreBurstOverlay(
    burst: ScoreBurstData,
    modifier: Modifier = Modifier
) {
    val tier = remember(burst.token) { scoreBurstTierFor(burst.score) }
    val popScale = remember(burst.token) { Animatable(if (tier == ScoreBurstTier.INCORRECT) 1f else 0.45f) }
    val popAlpha = remember(burst.token) { Animatable(0f) }
    val shakeOffset = remember(burst.token) { Animatable(0f) }
    val tilt = remember(burst.token) { Animatable(0f) }

    LaunchedEffect(burst.token) {
        popAlpha.animateTo(1f, animationSpec = tween(durationMillis = 130))
        when (tier) {
            ScoreBurstTier.INCORRECT -> {
                repeat(4) {
                    shakeOffset.animateTo(14f, tween(durationMillis = 38))
                    shakeOffset.animateTo(-14f, tween(durationMillis = 38))
                }
                shakeOffset.animateTo(0f, tween(durationMillis = 38))
            }

            ScoreBurstTier.OK -> {
                popScale.animateTo(
                    targetValue = 1.02f,
                    animationSpec = spring(dampingRatio = 0.66f, stiffness = 270f)
                )
            }

            ScoreBurstTier.GOOD -> {
                popScale.animateTo(
                    targetValue = 1.12f,
                    animationSpec = spring(dampingRatio = 0.56f, stiffness = 220f)
                )
                tilt.animateTo(-6f, tween(100))
                tilt.animateTo(0f, tween(120))
            }

            ScoreBurstTier.EXCELLENT -> {
                popScale.animateTo(
                    targetValue = 1.24f,
                    animationSpec = spring(dampingRatio = 0.48f, stiffness = 180f)
                )
                tilt.animateTo(-7f, tween(100))
                tilt.animateTo(7f, tween(100))
                tilt.animateTo(0f, tween(120))
            }
        }
    }

    val headline = when (tier) {
        ScoreBurstTier.INCORRECT -> "Incorrect"
        ScoreBurstTier.OK -> "Ok"
        ScoreBurstTier.GOOD -> "Good"
        ScoreBurstTier.EXCELLENT -> "Excellent"
    }
    val background = when (tier) {
        ScoreBurstTier.INCORRECT -> Color(0xFFBF7A17)
        ScoreBurstTier.OK -> Color(0xFFD9B63A)
        ScoreBurstTier.GOOD -> Color(0xFF85AA3F)
        ScoreBurstTier.EXCELLENT -> Color(0xFF219B5B)
    }
    val border = when (tier) {
        ScoreBurstTier.INCORRECT -> Color(0xFF7A420A)
        ScoreBurstTier.OK -> Color(0xFF7D651A)
        ScoreBurstTier.GOOD -> Color(0xFF546F1D)
        ScoreBurstTier.EXCELLENT -> Color(0xFF0E5831)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = tier == ScoreBurstTier.GOOD || tier == ScoreBurstTier.EXCELLENT,
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 1.15f)
        ) {
            CelebrationGlow(
                intense = tier == ScoreBurstTier.EXCELLENT,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .graphicsLayer {
                    alpha = popAlpha.value
                    scaleX = popScale.value
                    scaleY = popScale.value
                    rotationZ = tilt.value
                }
                .clip(RoundedCornerShape(18.dp))
                .background(background.copy(alpha = 0.95f))
                .border(2.dp, border, RoundedCornerShape(18.dp))
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = "Score ${burst.score}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private fun formatPercent(value: Double): String {
    val normalized = value.coerceIn(0.0, 100.0)
    return if (normalized == normalized.roundToInt().toDouble()) {
        "${normalized.roundToInt()}%"
    } else {
        "${"%.1f".format(normalized)}%"
    }
}

@Composable
private fun CelebrationGlow(
    intense: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "celebration-glow")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (intense) 760 else 920),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow-pulse"
    )
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * if (intense) 0.24f else 0.2f
        val rings = if (intense) 12 else 8
        repeat(rings) { index ->
            val angle = ((index * (360f / rings)) + (pulse * 38f)) * (Math.PI.toFloat() / 180f)
            val orbit = base + (index * 8f) + (pulse * 12f)
            val point = Offset(
                x = center.x + (cos(angle) * orbit),
                y = center.y + (sin(angle) * orbit)
            )
            drawCircle(
                color = if (intense) Color(0xFFFFD26A) else Color(0xFFB8F3D8),
                radius = if (intense) 5f else 4f,
                center = point,
                alpha = if (intense) 0.62f else 0.5f
            )
        }
    }
}

private fun scoreBurstTierFor(score: Int): ScoreBurstTier {
    return when (scoreBandFor(score)) {
        ScoreBand.EXCELLENT -> ScoreBurstTier.EXCELLENT
        ScoreBand.GOOD -> ScoreBurstTier.GOOD
        ScoreBand.OK -> ScoreBurstTier.OK
        ScoreBand.INCORRECT -> ScoreBurstTier.INCORRECT
    }
}

@Composable
private fun DeckActionPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEEFFFFFF))
            .border(1.dp, Color(0xFFD9B695), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color(0xFFFF5A00),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF432B1E),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CardStackFrame(
    modifier: Modifier = Modifier.height(600.dp),
    dragX: Float,
    dragY: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val leftHintAlpha = ((-dragX - 70f) / 90f).coerceIn(0f, 0.72f)
    val rightHintAlpha = ((dragX - 70f) / 90f).coerceIn(0f, 0.72f)
    val topHintAlpha = ((-dragY - 70f) / 90f).coerceIn(0f, 0.72f)
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 12.dp, y = 10.dp)
                .rotate(-4f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x55FFFDF9))
                .border(1.dp, Color(0x55CEB89E), RoundedCornerShape(20.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = (-8).dp, y = 6.dp)
                .rotate(3f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x66FFF7EC))
                .border(1.dp, Color(0x55CEB89E), RoundedCornerShape(20.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                .zIndex(3f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { _, dragAmount -> onDrag(dragAmount) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFDF8))
                    .border(1.dp, Color(0xFFDCC5A9), RoundedCornerShape(20.dp)),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
                overlay?.invoke(this)
            }
        }
        if (leftHintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(4f)
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = leftHintAlpha * 0.7f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Swipe left to skip",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .graphicsLayer { alpha = leftHintAlpha }
                )
            }
        }
        if (rightHintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(4f)
                    .fillMaxHeight()
                    .width(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = rightHintAlpha * 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Swipe right to go back",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .graphicsLayer { alpha = rightHintAlpha }
                )
            }
        }
        if (topHintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(4f)
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = topHintAlpha * 0.7f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Swipe up to submit",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .graphicsLayer { alpha = topHintAlpha }
                )
            }
        }
    }
}

@Composable
private fun GestureHelpOverlay(
    heroImageRes: Int,
    deckLabel: String,
    onDismiss: (Boolean) -> Unit
) {
    var neverShowAgain by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA61F1510))
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFFF8EE))
                .border(1.dp, Color(0xFFE2CDAF), RoundedCornerShape(20.dp)),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Image(
                    painter = painterResource(heroImageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x22000000), Color(0xA6000000))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "How to Play",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = deckLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFE2C7)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GestureHelpItem(
                    accent = Color(0xFF4B78D1),
                    title = "Swipe left or right",
                    detail = "Move between cards",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    kind = GestureGuideKind.HORIZONTAL
                )
                GestureHelpItem(
                    accent = Color(0xFF26A86B),
                    title = "Swipe up",
                    detail = "Submit current card",
                    icon = Icons.Filled.ArrowUpward,
                    kind = GestureGuideKind.UPWARD
                )
                GestureHelpItem(
                    accent = Color(0xFFB86A1B),
                    title = "Assist",
                    detail = "Hint + 20% score penalty",
                    icon = Icons.Filled.Brush,
                    kind = GestureGuideKind.ASSIST
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = neverShowAgain,
                    onCheckedChange = { checked -> neverShowAgain = checked }
                )
                Text("Don't show again", style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = { onDismiss(neverShowAgain) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun GestureHelpItem(
    accent: Color,
    title: String,
    detail: String,
    icon: ImageVector,
    kind: GestureGuideKind
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.11f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GestureGuideVector(
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            accent = accent,
            kind = kind
        )
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5F4A3B)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

private enum class GestureGuideKind {
    HORIZONTAL,
    UPWARD,
    ASSIST
}

@Composable
private fun GestureGuideVector(
    modifier: Modifier,
    accent: Color,
    kind: GestureGuideKind
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.08f))
    ) {
        val stroke = 4f
        val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        when (kind) {
            GestureGuideKind.HORIZONTAL -> {
                val y = size.height * 0.52f
                val start = size.width * 0.16f
                val end = size.width * 0.84f
                drawLine(
                    color = accent.copy(alpha = 0.55f),
                    start = Offset(start, y),
                    end = Offset(end, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = dashed
                )
                drawLine(
                    color = accent,
                    start = Offset(start + 16f, y - 10f),
                    end = Offset(start, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(start + 16f, y + 10f),
                    end = Offset(start, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(end - 16f, y - 10f),
                    end = Offset(end, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(end - 16f, y + 10f),
                    end = Offset(end, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            GestureGuideKind.UPWARD -> {
                val x = size.width * 0.5f
                val top = size.height * 0.18f
                val bottom = size.height * 0.82f
                drawLine(
                    color = accent.copy(alpha = 0.56f),
                    start = Offset(x, bottom),
                    end = Offset(x, top),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = dashed
                )
                drawLine(
                    color = accent,
                    start = Offset(x - 12f, top + 12f),
                    end = Offset(x, top),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(x + 12f, top + 12f),
                    end = Offset(x, top),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }

            GestureGuideKind.ASSIST -> {
                val center = Offset(size.width * 0.38f, size.height * 0.5f)
                drawCircle(
                    color = accent.copy(alpha = 0.22f),
                    radius = min(size.width, size.height) * 0.22f,
                    center = center
                )
                drawLine(
                    color = accent,
                    start = Offset(center.x - 7f, center.y),
                    end = Offset(center.x + 7f, center.y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(center.x, center.y - 7f),
                    end = Offset(center.x, center.y + 7f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                val sparkle = Offset(size.width * 0.7f, size.height * 0.35f)
                drawLine(
                    color = accent,
                    start = Offset(sparkle.x - 8f, sparkle.y),
                    end = Offset(sparkle.x + 8f, sparkle.y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent,
                    start = Offset(sparkle.x, sparkle.y - 8f),
                    end = Offset(sparkle.x, sparkle.y + 8f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun promptLabelFor(type: CardType): String {
    return when (type) {
        CardType.KANJI_WRITE -> "English prompt"
        CardType.KANJI_READING -> "Kanji reading"
        CardType.VOCAB_READING -> "Vocab check"
        CardType.GRAMMAR_CHOICE -> "Grammar choice"
        CardType.GRAMMAR_CLOZE_WRITE -> "Grammar cloze"
        CardType.SENTENCE_COMPREHENSION -> "Sentence understanding"
        CardType.SENTENCE_BUILD -> "Sentence construction"
    }
}

private fun instructionFor(type: CardType): String {
    return when (type) {
        CardType.KANJI_WRITE -> "Write the matching kanji from memory."
        CardType.KANJI_READING -> "Choose the correct reading for the kanji."
        CardType.VOCAB_READING -> "Pick the best answer."
        CardType.GRAMMAR_CHOICE -> "Choose the grammar pattern that fits."
        CardType.GRAMMAR_CLOZE_WRITE -> "Type the missing grammar form."
        CardType.SENTENCE_COMPREHENSION -> "Select the best meaning."
        CardType.SENTENCE_BUILD -> "Type the sentence that matches the prompt."
    }
}

private fun assistHintFor(card: com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard): AssistHintPayload {
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
            AssistHintPayload(
                text = if (answer.length > 1) {
                    "Assist active: partial reveal is shown for the first kanji."
                } else {
                    "Assist active: partial kanji reveal shown below."
                },
                reveal = if (revealKanji != null && revealMode != null) {
                    LuckyHintReveal(kanji = revealKanji, mode = revealMode)
                } else {
                    null
                }
            )
        }

        CardType.KANJI_READING,
        CardType.GRAMMAR_CHOICE,
        CardType.SENTENCE_COMPREHENSION,
        CardType.VOCAB_READING -> AssistHintPayload(
            text = "Assist active: one distractor removed."
        )

        CardType.GRAMMAR_CLOZE_WRITE -> {
            val answer = card.canonicalAnswer
            val grammarHint = inferGrammarHint(card.meaning, card.prompt, answer)
            val firstCharHint = answer.firstOrNull()?.let { "Starts with: ${it}\u2026" }
            AssistHintPayload(
                text = "Assist active: grammar pattern hint below.",
                patternHint = listOfNotNull(grammarHint, firstCharHint, "Length: ${answer.length}")
                    .joinToString(" \u00B7 ")
            )
        }

        CardType.SENTENCE_BUILD -> sentenceBuildAssistHint(card)
    }
}

private fun sentenceBuildAssistHint(card: com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard): AssistHintPayload {
    val japaneseAnswer = resolveJapaneseAnswer(card)
    val romajiAnswer = resolveRomajiAnswer(card)
    val chunks = sentenceAssistChunks(japaneseAnswer, romajiAnswer)
    val particleFlow = sentenceParticleFlowHint(japaneseAnswer)
    val isN3OrHigher = card.cardId.startsWith("jlpt_n3_core_") || card.difficulty >= 8
    val easierLevel = !isN3OrHigher
    return when {
        easierLevel && chunks.size >= 3 -> AssistHintPayload(
            text = "Assist active: arrange these chunks, then type the final sentence.",
            patternHint = particleFlow,
            wordBank = chunks.shuffled(Random(card.cardId.hashCode())),
            showFurigana = true,
            showRomaji = true
        )

        chunks.size >= 3 && isN3OrHigher -> AssistHintPayload(
            text = "Assist active: build the sentence in clause order.",
            patternHint = particleFlow,
            wordBank = chunks.take(4),
            showFurigana = false,
            showRomaji = false
        )

        else -> AssistHintPayload(
            text = "Assist active: focus on particle order and verb ending.",
            patternHint = particleFlow
        )
    }
}

private fun inferGrammarHint(meaning: String?, prompt: String, answer: String): String {
    val m = meaning?.lowercase().orEmpty()
    val p = prompt.lowercase()
    return when {
        m.contains("te-form") || m.contains("て-form") ->
            "Think: te-form conjugation"
        m.contains("please") || answer.endsWith("ください") ->
            "Think: polite request form"
        m.contains("prohibition") || m.contains("must not") || answer.endsWith("いけません") || answer.endsWith("だめです") ->
            "Think: prohibition (must not)"
        m.contains("must do") || m.contains("なければ") || answer.contains("なければ") ->
            "Think: obligation (must do)"
        m.contains("want to") || answer.endsWith("たい") || answer.endsWith("たいです") ->
            "Think: desire form (\u301Ctai)"
        m.contains("intend") || answer.contains("つもり") ->
            "Think: intention (tsumori)"
        m.contains("even if") || answer.contains("としても") || answer.contains("ても") ->
            "Think: concessive (even if)"
        m.contains("when") || m.contains("if") || answer.contains("とき") || answer.contains("たら") ->
            "Think: conditional/temporal"
        m.contains("cause") || m.contains("because") || m.contains("node") || answer.contains("ので") || answer.contains("から") ->
            "Think: cause-and-effect"
        m.contains("come to be") || answer.contains("ように") ->
            "Think: change of state (you ni)"
        m.contains("polite") || answer.endsWith("ます") || answer.endsWith("ません") ->
            "Think: polite verb ending"
        m.contains("please don") || answer.contains("ないで") ->
            "Think: negative request"
        m.contains("can") || m.contains("able") || answer.contains("られ") || answer.contains("できる") ->
            "Think: potential/ability form"
        m.contains("passive") || m.contains("受身") ->
            "Think: passive form"
        m.contains("comparison") || answer.contains("より") || answer.contains("ほう") ->
            "Think: comparison"
        p.contains("（") && p.contains("）") ->
            "Focus on the grammar that completes the sentence."
        else ->
            "Focus on the grammar form that fits."
    }
}

private fun resolveJapaneseAnswer(card: com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard): String {
    val candidates = listOf(card.canonicalAnswer) + card.acceptedAnswers
    return candidates.firstOrNull { containsJapaneseScript(it) } ?: card.canonicalAnswer
}

private fun resolveRomajiAnswer(card: com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard): String? {
    val candidates = listOf(card.canonicalAnswer) + card.acceptedAnswers
    return candidates.firstOrNull { containsLatinLetters(it) }
}

private fun sentenceAssistChunks(answer: String, romajiAnswer: String?): List<SentenceChunkHint> {
    val jpChunks = sentenceAssistChunksJapanese(answer)
    if (jpChunks.isEmpty()) return emptyList()
    val romajiChunks = romajiChunksForJapanese(jpChunks, romajiAnswer)
    return jpChunks.mapIndexed { index, chunk ->
        val romaji = romajiChunks?.getOrNull(index)
        SentenceChunkHint(
            jp = chunk,
            kana = romaji?.let(::romanizedToHiragana),
            romaji = romaji
        )
    }
}

private fun sentenceAssistChunksJapanese(answer: String): List<String> {
    val compact = answer.trim().removeSuffix("。")
    if (compact.isBlank()) return emptyList()
    if (compact.contains(' ')) {
        return compact
            .split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    val normalized = compact
        .replace("、", " ")
        .replace(",", " ")
    if (normalized.contains(' ')) {
        return normalized
            .split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    return splitJapaneseByParticles(compact)
        .flatMap(::splitLongJapaneseChunk)
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun splitJapaneseByParticles(sentence: String): List<String> {
    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    var index = 0
    while (index < sentence.length) {
        val particle = japaneseParticleAt(sentence, index)
        if (particle != null && shouldSplitParticle(sentence, index, particle)) {
            if (current.isNotEmpty()) {
                chunks += current.toString()
                current.clear()
            }
            chunks += particle
            index += particle.length
            continue
        }
        current.append(sentence[index])
        index += 1
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

private fun japaneseParticleAt(text: String, index: Int): String? {
    return JAPANESE_PARTICLE_ORDER.firstOrNull { particle ->
        text.startsWith(particle, startIndex = index)
    }
}

private fun shouldSplitParticle(text: String, start: Int, particle: String): Boolean {
    if (start == 0) return false
    val endExclusive = start + particle.length
    val next = text.getOrNull(endExclusive)
    val prev = text.getOrNull(start - 1)
    if (particle.length > 1) return true
    if (particle == "を") return true
    if (next == null || isSentenceBoundary(next)) return true
    if (isKanjiOrKatakana(next) || isLatinOrDigit(next)) return true
    if (
        particle in HIRAGANA_PARTICLES_FOLLOWING_KANJI &&
        prev != null &&
        isKanjiOrKatakana(prev) &&
        isHiragana(next)
    ) {
        return true
    }
    return false
}

private fun splitLongJapaneseChunk(chunk: String): List<String> {
    if (chunk.length < 8 || chunk in JAPANESE_PARTICLE_ROMAJI.keys) return listOf(chunk)
    val splitIndex = JAPANESE_ASSIST_SUFFIX_SPLITS
        .mapNotNull { suffix ->
            chunk.indexOf(suffix).takeIf { it > 1 && it < chunk.length - 1 }
        }
        .minOrNull()
    if (splitIndex == null) return listOf(chunk)
    return listOf(
        chunk.substring(0, splitIndex),
        chunk.substring(splitIndex)
    )
}

private fun isSentenceBoundary(character: Char): Boolean {
    return character in setOf('、', '。', ',', '.', '!', '?', '！', '？')
}

private fun isHiragana(character: Char): Boolean {
    return character in '\u3040'..'\u309F'
}

private fun isKanjiOrKatakana(character: Char): Boolean {
    return when (Character.UnicodeBlock.of(character)) {
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A -> true
        else -> false
    }
}

private fun isLatinOrDigit(character: Char): Boolean {
    return character in 'a'..'z' || character in 'A'..'Z' || character.isDigit()
}

private fun sentenceParticleFlowHint(answer: String): String {
    val compact = answer.trim().removeSuffix("。")
    if (compact.isBlank()) return "Pattern hint: keep natural sentence flow."
    val flow = if (compact.contains(' ')) {
        val parts = compact
            .split(Regex("""\s+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val particles = parts.filter { it in ROMAJI_PARTICLES }
        if (particles.isNotEmpty()) particles.take(4) else parts.take(4)
    } else {
        splitJapaneseByParticles(compact)
            .filter { it in JAPANESE_PARTICLE_ROMAJI.keys }
            .take(4)
            .toList()
    }
    return if (flow.isNotEmpty()) {
        "Pattern hint: ${flow.joinToString(" -> ")}"
    } else {
        "Pattern hint: keep natural sentence flow."
    }
}

private fun toggleWordBankSelection(current: List<Int>, index: Int): List<Int> {
    return if (index in current) {
        current.filterNot { it == index }
    } else {
        current + index
    }
}

private fun applyWordBankSelection(
    selection: List<Int>,
    chunks: List<SentenceChunkHint>,
    onAnswerUpdated: (String) -> Unit
) {
    if (selection.isEmpty()) {
        onAnswerUpdated("")
        return
    }
    val ordered = selection.mapNotNull { chunks.getOrNull(it) }
    val useJapaneseSpacing = ordered.any { containsJapaneseScript(it.jp) }
    val joiner = if (useJapaneseSpacing) "" else " "
    onAnswerUpdated(ordered.joinToString(separator = joiner) { it.jp })
}

private fun romajiChunksForJapanese(
    jpChunks: List<String>,
    romajiSentence: String?
): List<String>? {
    if (romajiSentence.isNullOrBlank()) return null
    val tokens = romajiSentence.trim()
        .split(Regex("""\s+"""))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null
    val chunks = mutableListOf<String>()
    var tokenIndex = 0
    for (chunkIndex in jpChunks.indices) {
        val jpChunk = jpChunks[chunkIndex]
        val particleRomaji = JAPANESE_PARTICLE_ROMAJI[jpChunk]
        if (particleRomaji != null) {
            chunks += particleRomaji
            if (tokenIndex < tokens.size && tokens[tokenIndex] == particleRomaji) {
                tokenIndex += 1
            }
            continue
        }
        val nextParticleRomaji = jpChunks
            .drop(chunkIndex + 1)
            .firstNotNullOfOrNull { JAPANESE_PARTICLE_ROMAJI[it] }
        val startIndex = tokenIndex
        while (tokenIndex < tokens.size) {
            val token = tokens[tokenIndex]
            if (nextParticleRomaji != null && token == nextParticleRomaji) {
                break
            }
            tokenIndex += 1
        }
        if (tokenIndex == startIndex && tokenIndex < tokens.size) {
            tokenIndex += 1
        }
        val combined = tokens.subList(startIndex, tokenIndex).joinToString(" ")
        chunks += combined
    }
    if (chunks.any { it.isBlank() }) {
        return balancedRomajiChunks(jpChunks, tokens)
    }
    return chunks
}

private fun balancedRomajiChunks(jpChunks: List<String>, tokens: List<String>): List<String> {
    if (tokens.isEmpty()) return List(jpChunks.size) { "" }
    val nonParticleIndices = jpChunks
        .mapIndexedNotNull { index, chunk ->
            if (chunk in JAPANESE_PARTICLE_ROMAJI.keys) null else index
        }
    if (nonParticleIndices.isEmpty()) {
        return jpChunks.map { chunk -> JAPANESE_PARTICLE_ROMAJI[chunk].orEmpty() }
    }
    val distributed = MutableList(jpChunks.size) { "" }
    var tokenCursor = 0
    nonParticleIndices.forEachIndexed { index, chunkIndex ->
        val remainingTokens = tokens.size - tokenCursor
        val remainingChunks = nonParticleIndices.size - index
        val takeCount = if (remainingChunks <= 0) 0 else (remainingTokens / remainingChunks).coerceAtLeast(1)
        val end = (tokenCursor + takeCount).coerceAtMost(tokens.size)
        distributed[chunkIndex] = tokens.subList(tokenCursor, end).joinToString(" ")
        tokenCursor = end
    }
    if (tokenCursor < tokens.size) {
        val lastIndex = nonParticleIndices.last()
        val tail = tokens.subList(tokenCursor, tokens.size).joinToString(" ")
        distributed[lastIndex] = listOf(distributed[lastIndex], tail)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
    jpChunks.forEachIndexed { index, chunk ->
        val particleRomaji = JAPANESE_PARTICLE_ROMAJI[chunk]
        if (particleRomaji != null) {
            distributed[index] = particleRomaji
        }
    }
    return distributed
}

@Composable
private fun LuckyKanjiRevealHint(
    reveal: LuckyHintReveal,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .clip(cardShape)
            .background(Color(0xFFFFF4E7))
            .border(1.dp, Color(0xFFD7B189), cardShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = luckyRevealLabel(reveal.mode),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF7A4F34),
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFFFFCF7))
                .border(1.dp, Color(0xFFDCC0A2), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reveal.kanji,
                modifier = Modifier.partialKanjiReveal(reveal.mode),
                color = Color(0xFF5C3118),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun luckyRevealLabel(mode: KanjiHintRevealMode): String {
    return when (mode) {
        KanjiHintRevealMode.TOP_STROKE_SLICE -> "Assist reveal: first-stroke region"
        KanjiHintRevealMode.TOP_LEFT_QUADRANT -> "Assist reveal: top-left quarter"
    }
}

private fun Modifier.partialKanjiReveal(mode: KanjiHintRevealMode): Modifier {
    return drawWithContent {
        when (mode) {
            KanjiHintRevealMode.TOP_STROKE_SLICE -> {
                clipRect(
                    left = size.width * 0.06f,
                    top = 0f,
                    right = size.width * 0.94f,
                    bottom = size.height * 0.48f
                ) {
                    this@drawWithContent.drawContent()
                }
            }

            KanjiHintRevealMode.TOP_LEFT_QUADRANT -> {
                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width * 0.54f,
                    bottom = size.height * 0.54f
                ) {
                    this@drawWithContent.drawContent()
                }
            }
        }
    }
}

private fun shouldShowFurigana(cardDifficulty: Int): Boolean {
    return cardDifficulty <= 4
}

private fun stripFuriganaMarkup(text: String): String {
    return FURIGANA_TOKEN_REGEX.replace(text, "$1")
}

private fun shouldShowRomanizedPrompt(prompt: String, japanesePrompt: String?): Boolean {
    return japanesePrompt != null &&
        containsJapaneseScript(japanesePrompt) &&
        containsLatinLetters(prompt) &&
        !containsJapaneseScript(prompt)
}

private fun containsJapaneseScript(text: String): Boolean {
    return text.any { character ->
        when (Character.UnicodeBlock.of(character)) {
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A -> true
            else -> false
        }
    }
}

private fun containsLatinLetters(text: String): Boolean {
    return text.any { it in 'a'..'z' || it in 'A'..'Z' }
}

private val ROMAJI_PARTICLES = setOf(
    "wa", "ga", "o", "ni", "de", "to", "no", "mo", "ka", "e",
    "kara", "made", "node", "kedo", "yori"
)

private val JAPANESE_PARTICLE_ORDER = listOf(
    "から", "まで", "より", "って", "では", "には",
    "へ", "を", "に", "で", "と", "が", "は", "の", "も", "か"
)

private val HIRAGANA_PARTICLES_FOLLOWING_KANJI = setOf(
    "に", "で", "と", "が", "は", "の", "も", "へ"
)

private val JAPANESE_ASSIST_SUFFIX_SPLITS = listOf(
    "なければなりません",
    "いただけませんか",
    "ていただけませんか",
    "してください",
    "と思います",
    "ませんか",
    "ましょう",
    "ました",
    "ます",
    "です"
)

private val JAPANESE_PARTICLE_ROMAJI = mapOf(
    "は" to "wa",
    "が" to "ga",
    "を" to "o",
    "に" to "ni",
    "で" to "de",
    "と" to "to",
    "の" to "no",
    "も" to "mo",
    "か" to "ka",
    "へ" to "e",
    "から" to "kara",
    "まで" to "made",
    "より" to "yori",
    "って" to "tte",
    "では" to "dewa",
    "には" to "niwa"
)

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceAnswerPanel(
    choices: List<String>,
    selectedChoice: String?,
    onChoiceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFCF6))
            .border(2.dp, Color(0xFFB89B7A), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choices.forEach { option ->
                val selected = selectedChoice == option
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color(0xFFFFDFC8) else Color(0xFFFFFFFF))
                        .border(1.dp, Color(0xFFD6B290), RoundedCornerShape(12.dp))
                        .clickable { onChoiceSelected(option) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color(0xFF2D1E14)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FuriganaText(
    text: String,
    modifier: Modifier = Modifier
) {
    val tokens = remember(text) { parseFuriganaTokens(text) }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tokens.forEach { token ->
            when (token) {
                is FuriganaToken.Plain -> Text(
                    text = token.value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                is FuriganaToken.Ruby -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = token.reading,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF7A4F34)
                    )
                    Text(
                        text = token.base,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceWordBankHint(
    chunks: List<SentenceChunkHint>,
    selectedIndices: Set<Int>,
    showFurigana: Boolean,
    showRomaji: Boolean,
    onChunkToggled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF4E7))
            .border(1.dp, Color(0xFFD7B189), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Word bank",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF7A4F34),
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chunks.forEachIndexed { index, chunk ->
                val selected = index in selectedIndices
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFFFFDFC8) else Color(0xFFFFFCF7))
                        .border(
                            1.dp,
                            if (selected) Color(0xFFFFB17A) else Color(0xFFDCC0A2),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onChunkToggled(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (showFurigana && !chunk.kana.isNullOrBlank() && containsJapaneseScript(chunk.jp)) {
                            Text(
                                text = chunk.kana,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7A4F34)
                            )
                        }
                        Text(
                            text = chunk.jp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5C3118),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (showRomaji && !chunk.romaji.isNullOrBlank()) {
                            Text(
                                text = chunk.romaji,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF7A4F34)
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface FuriganaToken {
    data class Plain(val value: String) : FuriganaToken
    data class Ruby(val base: String, val reading: String) : FuriganaToken
}

private val FURIGANA_TOKEN_REGEX = Regex("""([^\s{}]+)\{([^{}]+)\}""")

private fun parseFuriganaTokens(text: String): List<FuriganaToken> {
    val tokens = mutableListOf<FuriganaToken>()
    var cursor = 0
    FURIGANA_TOKEN_REGEX.findAll(text).forEach { match ->
        if (cursor < match.range.first) {
            tokens.add(FuriganaToken.Plain(text.substring(cursor, match.range.first)))
        }
        tokens.add(FuriganaToken.Ruby(base = match.groupValues[1], reading = match.groupValues[2]))
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        tokens.add(FuriganaToken.Plain(text.substring(cursor)))
    }
    return tokens.filterNot { token ->
        token is FuriganaToken.Plain && token.value.isBlank()
    }
}

@Composable
private fun AssistFooter(
    enabled: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(
                1.dp,
                if (enabled) Color(0xFFFF8C4E) else Color(0xFFFFCCAF),
                RoundedCornerShape(14.dp)
            )
            .clickable(enabled = !enabled, onClick = onActivate)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = if (enabled) "Assist On" else "Assist Off",
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color(0xFF8A3F1C) else Color(0xFF6A5243),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = if (enabled) {
                "Hint active for this question. Assist cannot be turned off for this card."
            } else {
                "Tap to activate a hint for this question."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4F3A2E)
        )
    }
}

@Composable
private fun InkPad(
    resetCounter: Int,
    onSampleChanged: (InkSample) -> Unit,
    modifier: Modifier = Modifier
) {
    var strokes by remember { mutableStateOf(listOf<List<Offset>>()) }
    var undoStack by remember { mutableStateOf(listOf<List<List<Offset>>>()) }
    var isEraseMode by remember { mutableStateOf(false) }
    var operationSnapshotTaken by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(resetCounter) {
        strokes = emptyList()
        undoStack = emptyList()
        onSampleChanged(InkSample(emptyList(), canvasSize.width.toFloat(), canvasSize.height.toFloat()))
        isEraseMode = false
        operationSnapshotTaken = false
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 1.dp, color = Color(0xFFBFA489), shape = RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFCF8))
            .onSizeChanged { size ->
                canvasSize = size
                if (strokes.isNotEmpty()) {
                    onSampleChanged(strokes.toInkSample(canvasSize))
                }
            }
            .pointerInput(resetCounter, isEraseMode) {
                detectDragGestures(
                    onDragStart = { start ->
                        if (!operationSnapshotTaken) {
                            undoStack = undoStack + listOf(strokes)
                            operationSnapshotTaken = true
                        }
                        if (isEraseMode) {
                            strokes = eraseAt(strokes, start)
                        } else {
                            strokes = strokes + listOf(listOf(start))
                        }
                        onSampleChanged(strokes.toInkSample(canvasSize))
                    },
                    onDragEnd = {
                        operationSnapshotTaken = false
                    },
                    onDragCancel = {
                        operationSnapshotTaken = false
                    },
                    onDrag = { change, _ ->
                        if (isEraseMode) {
                            strokes = eraseAt(strokes, change.position)
                        } else {
                            val updated = strokes.toMutableList()
                            if (updated.isNotEmpty()) {
                                val currentStroke = updated.last().toMutableList()
                                currentStroke.add(change.position)
                                updated[updated.lastIndex] = currentStroke
                                strokes = updated
                            }
                        }
                        onSampleChanged(strokes.toInkSample(canvasSize))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDimension = min(size.width, size.height)
            val guideStroke = (minDimension * 0.012f).coerceAtLeast(1f)
            val guideColor = Color(0xFFB9A58F)
            val dash = PathEffect.dashPathEffect(
                intervals = floatArrayOf(minDimension * 0.05f, minDimension * 0.03f)
            )
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            drawLine(
                color = guideColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = guideStroke,
                pathEffect = dash
            )
            drawLine(
                color = guideColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = guideStroke,
                pathEffect = dash
            )
            val strokeWidth = (minDimension * 0.08f).coerceAtLeast(2f)
            for (stroke in strokes) {
                if (stroke.isEmpty()) continue
                if (stroke.size == 1) {
                    drawCircle(
                        color = Color(0xFF2B1E17),
                        radius = strokeWidth / 2f,
                        center = stroke.first()
                    )
                    continue
                }
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    for (point in stroke.drop(1)) {
                        lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF2B1E17),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            InkToolChip(
                icon = Icons.Filled.Close,
                tint = Color(0xFFD44343),
                label = "Clear"
            ) {
                undoStack = undoStack + listOf(strokes)
                strokes = emptyList()
                onSampleChanged(InkSample(emptyList(), canvasSize.width.toFloat(), canvasSize.height.toFloat()))
            }
            InkToolChip(
                icon = Icons.AutoMirrored.Filled.Undo,
                tint = Color(0xFF6E5A4A),
                label = "Undo",
                enabled = undoStack.isNotEmpty()
            ) {
                if (undoStack.isNotEmpty()) {
                    val previous = undoStack.last()
                    undoStack = undoStack.dropLast(1)
                    strokes = previous
                    onSampleChanged(strokes.toInkSample(canvasSize))
                }
            }
            val oppositeModeIcon = if (isEraseMode) Icons.Filled.Brush else Icons.AutoMirrored.Filled.Backspace
            val oppositeModeTint = if (isEraseMode) Color(0xFF3E72CF) else Color(0xFFC86CAA)
            val oppositeModeLabel = if (isEraseMode) "Draw" else "Erase"
            InkToolChip(
                icon = oppositeModeIcon,
                tint = oppositeModeTint,
                label = oppositeModeLabel
            ) {
                isEraseMode = !isEraseMode
            }
        }
    }
}

@Composable
private fun InkToolChip(
    icon: ImageVector,
    tint: Color,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Color(0xE6FFF7EC) else Color(0xDDE8E1D4))
            .border(1.dp, Color(0xFFDBC4A5), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else Color(0xFF9E8E7C),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = " $label",
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) Color(0xFF634A39) else Color(0xFF9E8E7C)
            )
        }
    }
}

private fun eraseAt(
    source: List<List<Offset>>,
    center: Offset,
    radius: Float = 24f
): List<List<Offset>> {
    val radiusSquared = radius * radius
    return source.mapNotNull { stroke ->
        val filtered = stroke.filter { point ->
            val dx = point.x - center.x
            val dy = point.y - center.y
            (dx * dx) + (dy * dy) > radiusSquared
        }
        if (filtered.size >= 2) filtered else null
    }
}

private fun List<List<Offset>>.toInkSample(canvasSize: IntSize): InkSample {
    return InkSample(
        canvasWidth = canvasSize.width.toFloat(),
        canvasHeight = canvasSize.height.toFloat(),
        strokes = map { stroke ->
            InkStroke(
                points = stroke.map { point ->
                    InkPoint(x = point.x, y = point.y)
                }
            )
        }
    )
}

private fun themedDeckBackground(deckType: DeckType?, sourceId: String?): Pair<Int, Color> {
    val key = sourceId?.lowercase().orEmpty()
    return when {
        key.contains("food") -> Pair(R.drawable.pack_scene_food, Color(0xFFFFE8D2))
        key.contains("transport") -> Pair(R.drawable.pack_scene_city, Color(0xFFE4F1FF))
        key.contains("shopping") -> Pair(R.drawable.hero_autumn, Color(0xFFFFEFE0))
        key.contains("daily_life") -> Pair(R.drawable.pack_scene_temple, Color(0xFFF2E8DD))
        key.contains("jlpt_n3") -> Pair(R.drawable.hero_autumn, Color(0xFFF4ECFF))
        key.contains("jlpt_n4") -> Pair(R.drawable.hero_summer, Color(0xFFE8F7FF))
        key.contains("jlpt_n5") -> Pair(R.drawable.hero_spring, Color(0xFFFFF2E8))
        deckType == DeckType.DAILY -> Pair(R.drawable.pack_scene_food, Color(0xFFFFE8D2))
        deckType == DeckType.REMEDIAL -> Pair(R.drawable.pack_scene_temple, Color(0xFFF2E8DD))
        else -> Pair(R.drawable.pack_scene_city, Color(0xFFEAF1F5))
    }
}
