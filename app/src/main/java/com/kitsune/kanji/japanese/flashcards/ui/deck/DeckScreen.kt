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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.PowerUpPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.entity.CardType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkPoint
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkStroke
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun DeckScreen(
    state: DeckUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmitCard: (InkSample, String?, String?, List<String>) -> Unit,
    onUsePowerUp: (String) -> Unit,
    onDeckSubmitted: (String) -> Unit,
    onSubmitDeck: () -> Unit
) {
    val currentCard = state.currentCard
    var currentSample by remember(currentCard?.cardId) { mutableStateOf(InkSample(emptyList())) }
    var typedAnswer by remember(currentCard?.cardId) { mutableStateOf("") }
    var selectedChoice by remember(currentCard?.cardId) { mutableStateOf<String?>(null) }
    var canvasResetCounter by remember(currentCard?.cardId) { mutableIntStateOf(0) }
    var selectedAssistId by remember(currentCard?.cardId) { mutableStateOf<String?>(null) }
    var pendingPowerUpId by rememberSaveable(state.deckRunId, currentCard?.cardId) {
        mutableStateOf<String?>(null)
    }
    var showGestureOverlay by rememberSaveable(state.deckRunId) { mutableStateOf(true) }
    var cardDragX by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }
    var cardDragY by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }
    var scoreBurst by remember(state.deckRunId) { mutableStateOf<ScoreBurstData?>(null) }
    val scoredCards = state.session?.cards?.mapNotNull { it.resultScore }.orEmpty()
    val reviewedCount = state.reviewedCardCount
    val totalCards = state.totalCardCount
    val remainingCount = state.activeCardCount
    val cardPosition = if (remainingCount == 0) 0 else (state.currentIndex + 1).coerceAtMost(remainingCount)
    val runScoreTotal = scoredCards.sum()
    val runAverageScore = scoredCards.takeIf { it.isNotEmpty() }?.average()?.roundToInt() ?: 0
    val activeLuckyHint = state.activeHintText
        ?.takeIf { state.activeHintCardId == currentCard?.cardId }
    val activeLuckyReveal = state.activeHintReveal
        ?.takeIf { state.activeHintCardId == currentCard?.cardId }
    val isChoiceCard = currentCard?.choices?.isNotEmpty() == true &&
        currentCard.type in setOf(
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
    val visibleChoices = remember(currentCard?.cardId, selectedAssistId) {
        val card = currentCard ?: return@remember emptyList()
        val choices = card.choices
        if (
            selectedAssistId == PowerUpPreferences.POWER_UP_HINT_BRUSH &&
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
        val effective = state.latestEffectiveScore ?: latestScore
        val burst = ScoreBurstData(
            token = state.latestSubmissionToken,
            score = latestScore,
            effectiveScore = effective
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

    fun submitCurrentCard() {
        if (!canSubmit) return
        onSubmitCard(
            currentSample,
            typedAnswer.takeIf { it.isNotBlank() },
            selectedChoice,
            listOfNotNull(selectedAssistId)
        )
        canvasResetCounter += 1
        currentSample = InkSample(emptyList())
        typedAnswer = ""
        selectedChoice = null
        selectedAssistId = null
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
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (totalCards > 0 && reviewedCount >= totalCards) {
                        "All cards in this session are graded."
                    } else {
                        "No cards found for this deck."
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                if (totalCards > 0 && reviewedCount >= totalCards) {
                    Button(onClick = onSubmitDeck) {
                        Text("Submit Deck")
                    }
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
                DeckActionPill(
                    text = "Back",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                )
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF2FFFFFF))
                        .border(1.dp, Color(0xFFD9B695), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                }
                DeckActionPill(
                    text = "Finish",
                    icon = Icons.Filled.DoneAll,
                    onClick = onSubmitDeck,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DeckMetricCard(
                    icon = Icons.Filled.AutoFixHigh,
                    title = "Run Score",
                    value = runScoreTotal.toString(),
                    modifier = Modifier.weight(1f)
                )
                DeckMetricCard(
                    icon = Icons.Filled.Pets,
                    title = "Reviewed",
                    value = "$reviewedCount/$totalCards (avg $runAverageScore)",
                    modifier = Modifier.weight(1f)
                )
            }

            CardStackFrame(
                modifier = Modifier.weight(1f),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
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
                        val furiganaPrompt = currentCard.promptFurigana
                            ?.takeIf { shouldShowFurigana(cardDifficulty = currentCard.difficulty) }
                        if (furiganaPrompt != null) {
                            FuriganaText(
                                text = furiganaPrompt,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = currentCard.prompt,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = instructionFor(currentCard.type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6A5A48)
                        )
                        val assistHint = if (selectedAssistId == PowerUpPreferences.POWER_UP_HINT_BRUSH) {
                            assistHintTextFor(currentCard)
                        } else {
                            null
                        }
                        val effectiveHint = activeLuckyHint ?: assistHint
                        if (effectiveHint != null) {
                            Text(
                                text = effectiveHint,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF7A4F34),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (activeLuckyReveal != null) {
                            LuckyKanjiRevealHint(
                                reveal = activeLuckyReveal,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (currentCard.isRetryQueued) {
                            Text(
                                text = "Recycled practice card",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8A4E2C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (usesHandwritingPad) {
                        InkPad(
                            resetCounter = canvasResetCounter,
                            onSampleChanged = { sample -> currentSample = sample },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else if (isChoiceCard) {
                        ChoiceAnswerPanel(
                            choices = visibleChoices,
                            selectedChoice = selectedChoice,
                            onChoiceSelected = { selectedChoice = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PowerUpFooter(
                    powerUps = state.powerUps,
                    selectedPowerUpId = selectedAssistId,
                    onPowerUpTapped = { powerUpId ->
                        if (selectedAssistId == powerUpId && !isInstantPowerUp(powerUpId)) {
                            selectedAssistId = null
                        } else {
                            pendingPowerUpId = powerUpId
                        }
                    },
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

            state.latestMatchedAnswer?.let { matched ->
                Text(
                    text = if (state.latestIsCanonical) {
                        "Matched: $matched (canonical)"
                    } else {
                        "Matched accepted variant: $matched"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!state.latestIsCanonical) {
                state.latestCanonicalAnswer?.let { canonical ->
                    Text(
                        text = "JLPT canonical answer: $canonical",
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

        val pendingPowerUp = state.powerUps.firstOrNull { it.id == pendingPowerUpId }
        if (pendingPowerUp != null) {
            val activation = powerUpActivationInfo(pendingPowerUp.id)
            AlertDialog(
                onDismissRequest = { pendingPowerUpId = null },
                title = { Text(activation.title) },
                text = {
                    Text(
                        text = "${activation.effect}\n\n${activation.instructions}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (activation.instantUse) {
                                onUsePowerUp(pendingPowerUp.id)
                            } else {
                                selectedAssistId = pendingPowerUp.id
                            }
                            pendingPowerUpId = null
                        }
                    ) {
                        Text(if (activation.instantUse) "Use Now" else "Arm Assist")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingPowerUpId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showGestureOverlay) {
            GestureHelpOverlay(
                onDismiss = { showGestureOverlay = false }
            )
        }
    }
}

private data class ScoreBurstData(
    val token: Long,
    val score: Int,
    val effectiveScore: Int
)

private enum class ScoreBurstTier {
    MISS,
    GOOD,
    GREAT,
    EXCELLENT
}

@Composable
private fun ScoreBurstOverlay(
    burst: ScoreBurstData,
    modifier: Modifier = Modifier
) {
    val tier = remember(burst.token) { scoreBurstTierFor(burst.effectiveScore) }
    val popScale = remember(burst.token) { Animatable(if (tier == ScoreBurstTier.MISS) 1f else 0.45f) }
    val popAlpha = remember(burst.token) { Animatable(0f) }
    val shakeOffset = remember(burst.token) { Animatable(0f) }
    val tilt = remember(burst.token) { Animatable(0f) }

    LaunchedEffect(burst.token) {
        popAlpha.animateTo(1f, animationSpec = tween(durationMillis = 130))
        when (tier) {
            ScoreBurstTier.MISS -> {
                repeat(4) {
                    shakeOffset.animateTo(14f, tween(durationMillis = 38))
                    shakeOffset.animateTo(-14f, tween(durationMillis = 38))
                }
                shakeOffset.animateTo(0f, tween(durationMillis = 38))
            }

            ScoreBurstTier.GOOD -> {
                popScale.animateTo(
                    targetValue = 1.03f,
                    animationSpec = spring(dampingRatio = 0.66f, stiffness = 270f)
                )
            }

            ScoreBurstTier.GREAT -> {
                popScale.animateTo(
                    targetValue = 1.13f,
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
        ScoreBurstTier.MISS -> "Reinforce"
        ScoreBurstTier.GOOD -> "Good"
        ScoreBurstTier.GREAT -> "Great"
        ScoreBurstTier.EXCELLENT -> "Excellent"
    }
    val background = when (tier) {
        ScoreBurstTier.MISS -> Color(0xFFE16957)
        ScoreBurstTier.GOOD -> Color(0xFF219B5B)
        ScoreBurstTier.GREAT -> Color(0xFF1A9A83)
        ScoreBurstTier.EXCELLENT -> Color(0xFFF39C12)
    }
    val border = when (tier) {
        ScoreBurstTier.MISS -> Color(0xFF6F221A)
        ScoreBurstTier.GOOD -> Color(0xFF0E5831)
        ScoreBurstTier.GREAT -> Color(0xFF0C5448)
        ScoreBurstTier.EXCELLENT -> Color(0xFF8C4D00)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = tier == ScoreBurstTier.GREAT || tier == ScoreBurstTier.EXCELLENT,
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
            if (burst.effectiveScore != burst.score) {
                Text(
                    text = "Learning score ${burst.effectiveScore}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFF4E5)
                )
            }
        }
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
    return when {
        score >= 95 -> ScoreBurstTier.EXCELLENT
        score >= 85 -> ScoreBurstTier.GREAT
        score >= 70 -> ScoreBurstTier.GOOD
        else -> ScoreBurstTier.MISS
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
private fun DeckMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xF2FFFFFF))
            .border(1.dp, Color(0xFFD9B695), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFFF7B39),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF684E3E)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF432B1E)
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
                    .padding(start = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF000000).copy(alpha = leftHintAlpha))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("Next Card", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        if (rightHintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF000000).copy(alpha = rightHintAlpha))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("Previous Card", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        if (topHintAlpha > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF000000).copy(alpha = topHintAlpha))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("Submit Card", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GestureHelpOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBF1F1510))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFFFC7AA), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("How to Play", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Drag cards left/right freely to move between cards.")
            Text("Drag up to submit a written card for scoring.")
            Text("You can also tap the green Submit button in the footer.")
            Text("Power-ups:")
            Text("- Lucky Coin: instant hint for the current question")
            Text("- Insight Lens: arm assist to narrow options on submit")
            Text("- Kitsune Charm: swap this card for another unanswered one")
            Text("Tap a power-up and confirm in the dialog to activate it.")
            Text("Power-up counts are account inventory shared across decks/challenges.")
            Text("Using assists lowers learning score and schedules faster reinforcement.")
            Text("Accepted variants can pass, but canonical JLPT forms score higher.")
            Text("When ready, submit the full deck from the top-right action.")
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Got it")
            }
        }
    }
}

private fun promptLabelFor(type: CardType): String {
    return when (type) {
        CardType.KANJI_WRITE -> "English prompt"
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
        CardType.VOCAB_READING -> "Pick the best answer."
        CardType.GRAMMAR_CHOICE -> "Choose the grammar pattern that fits."
        CardType.GRAMMAR_CLOZE_WRITE -> "Type the missing grammar form."
        CardType.SENTENCE_COMPREHENSION -> "Select the best meaning."
        CardType.SENTENCE_BUILD -> "Type the sentence that matches the prompt."
    }
}

private fun assistHintTextFor(card: com.kitsune.kanji.japanese.flashcards.domain.model.DeckCard): String {
    return when (card.type) {
        CardType.GRAMMAR_CHOICE,
        CardType.SENTENCE_COMPREHENSION,
        CardType.VOCAB_READING -> "Insight Lens armed: one distractor removed."
        else -> "Insight Lens armed: submit to apply assist penalty and context support."
    }
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
                color = Color(0xFF7A4F34).copy(alpha = 0.2f),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
            )
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
        KanjiHintRevealMode.TOP_STROKE_SLICE -> "Lucky Coin reveal: first-stroke region"
        KanjiHintRevealMode.TOP_LEFT_QUADRANT -> "Lucky Coin reveal: top-left quarter"
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

private sealed interface FuriganaToken {
    data class Plain(val value: String) : FuriganaToken
    data class Ruby(val base: String, val reading: String) : FuriganaToken
}

private fun parseFuriganaTokens(text: String): List<FuriganaToken> {
    val regex = Regex("""([^\s{}]+)\{([^{}]+)\}""")
    val tokens = mutableListOf<FuriganaToken>()
    var cursor = 0
    regex.findAll(text).forEach { match ->
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
private fun PowerUpFooter(
    powerUps: List<PowerUpInventory>,
    selectedPowerUpId: String?,
    onPowerUpTapped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = powerUps.firstOrNull { it.id == selectedPowerUpId }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFCCAF), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Power-Ups (shared account inventory)",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6A5243),
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            powerUps.forEach { powerUp ->
                val isEnabled = powerUp.count > 0
                val isArmed = selectedPowerUpId == powerUp.id && !isInstantPowerUp(powerUp.id)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                !isEnabled -> Color(0xFFF1ECE8)
                                isArmed -> Color(0xFFFFE2CF)
                                else -> Color(0xFFFFF1E8)
                            }
                        )
                        .border(
                            1.dp,
                            if (isArmed) Color(0xFFFF8C4E) else Color(0xFFFFC6A5),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            if (isEnabled) {
                                onPowerUpTapped(powerUp.id)
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = iconForPowerUp(powerUp.id),
                                contentDescription = powerUp.title,
                                tint = Color(0xFFFF5A00),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " ${powerUp.count}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF4D3A2F),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = shortPowerUpTitle(powerUp.id),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4D3A2F),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when {
                                !isEnabled -> "Empty"
                                isArmed -> "Armed"
                                isInstantPowerUp(powerUp.id) -> "Tap to use"
                                else -> "Tap to arm"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7A5A48)
                        )
                    }
                }
            }
        }

        if (selected != null) {
            Text(
                text = "${selected.title}: ${selected.description}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2F221C)
            )
        }
    }
}

private fun iconForPowerUp(powerUpId: String): ImageVector {
    return when (powerUpId) {
        PowerUpPreferences.POWER_UP_LUCKY_COIN -> Icons.Filled.Casino
        PowerUpPreferences.POWER_UP_HINT_BRUSH -> Icons.Filled.Visibility
        PowerUpPreferences.POWER_UP_KITSUNE_CHARM -> Icons.Filled.Pets
        else -> Icons.Filled.AutoFixHigh
    }
}

private data class PowerUpActivationInfo(
    val title: String,
    val effect: String,
    val instructions: String,
    val instantUse: Boolean
)

private fun powerUpActivationInfo(powerUpId: String): PowerUpActivationInfo {
    return when (powerUpId) {
        PowerUpPreferences.POWER_UP_LUCKY_COIN -> PowerUpActivationInfo(
            title = "Use Lucky Coin?",
            effect = "Reveal a hint for this card now.",
            instructions = "Hint applies immediately and Lucky Coin is consumed.",
            instantUse = true
        )

        PowerUpPreferences.POWER_UP_HINT_BRUSH -> PowerUpActivationInfo(
            title = "Arm Insight Lens?",
            effect = "Narrow answer options and apply assist penalty on submit.",
            instructions = "Insight Lens remains armed for this card until you submit or unarm it.",
            instantUse = false
        )

        PowerUpPreferences.POWER_UP_KITSUNE_CHARM -> PowerUpActivationInfo(
            title = "Use Kitsune Charm?",
            effect = "Swap this question for another unanswered card.",
            instructions = "Swap happens immediately and Kitsune Charm is consumed.",
            instantUse = true
        )

        else -> PowerUpActivationInfo(
            title = "Use Power-Up?",
            effect = "Activate this power-up.",
            instructions = "This action may consume inventory.",
            instantUse = true
        )
    }
}

private fun isInstantPowerUp(powerUpId: String): Boolean {
    return powerUpId == PowerUpPreferences.POWER_UP_LUCKY_COIN ||
        powerUpId == PowerUpPreferences.POWER_UP_KITSUNE_CHARM
}

private fun shortPowerUpTitle(powerUpId: String): String {
    return when (powerUpId) {
        PowerUpPreferences.POWER_UP_LUCKY_COIN -> "Coin"
        PowerUpPreferences.POWER_UP_HINT_BRUSH -> "Lens"
        PowerUpPreferences.POWER_UP_KITSUNE_CHARM -> "Charm"
        else -> "Power"
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
