package com.kitsune.kanji.japanese.flashcards.ui.deck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkPoint
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkSample
import com.kitsune.kanji.japanese.flashcards.domain.ink.InkStroke
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DeckScreen(
    state: DeckUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmitCard: (InkSample, List<String>) -> Unit,
    onSubmitDeck: () -> Unit
) {
    val currentCard = state.currentCard
    var currentSample by remember(currentCard?.cardId) { mutableStateOf(InkSample(emptyList())) }
    var canvasResetCounter by remember(currentCard?.cardId) { mutableIntStateOf(0) }
    var selectedAssistId by remember(currentCard?.cardId) { mutableStateOf<String?>(null) }
    var showGestureOverlay by rememberSaveable(state.deckRunId) { mutableStateOf(true) }
    var cardDragX by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }
    var cardDragY by remember(currentCard?.cardId) { mutableFloatStateOf(0f) }

    fun submitCurrentCard() {
        if (currentSample.strokes.isEmpty()) return
        onSubmitCard(currentSample, listOfNotNull(selectedAssistId))
        canvasResetCounter += 1
        currentSample = InkSample(emptyList())
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
            Text(
                text = "No cards found for this deck.",
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    text = "Card ${state.currentIndex + 1}/${state.session?.cards?.size ?: 0}",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onSubmitDeck) { Text("Submit Deck") }
            }

            CardStackFrame(
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
                            text = "English prompt",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentCard.prompt,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Write the matching kanji from memory.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6A5A48)
                        )
                        if (currentCard.isRetryQueued) {
                            Text(
                                text = "Recycled practice card",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF8A4E2C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    InkPad(
                        resetCounter = canvasResetCounter,
                        onSampleChanged = { sample -> currentSample = sample },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
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
                    onSelectedPowerUpChanged = { selectedAssistId = it },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { submitCurrentCard() },
                    enabled = currentSample.strokes.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF20A557),
                        disabledContainerColor = Color(0xFFB5D5C1)
                    ),
                    modifier = Modifier
                        .height(74.dp)
                        .width(112.dp)
                ) {
                    Text(
                        text = "Submit",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            state.latestScore?.let { score ->
                Text(
                    text = "Latest score: $score" + (
                        state.latestEffectiveScore?.let { effective ->
                            if (effective != score) " (learning score $effective)" else ""
                        } ?: ""
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
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
            state.deckResult?.let { result ->
                val resultLine = buildString {
                    append("Deck submitted. Score ${result.totalScore} (${result.grade})")
                    if (result.deckType == DeckType.EXAM) {
                        append(if (result.passedThreshold) " - Passed" else " - Not passed")
                    }
                }
                Text(text = resultLine, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                result.unlockedPackId?.let { unlocked ->
                    Text(
                        text = "Unlocked next pack: $unlocked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (showGestureOverlay) {
            GestureHelpOverlay(
                onDismiss = { showGestureOverlay = false }
            )
        }
    }
}

@Composable
private fun CardStackFrame(
    dragX: Float,
    dragY: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val leftHintAlpha = ((-dragX - 70f) / 90f).coerceIn(0f, 0.72f)
    val rightHintAlpha = ((dragX - 70f) / 90f).coerceIn(0f, 0.72f)
    val topHintAlpha = ((-dragY - 70f) / 90f).coerceIn(0f, 0.72f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 12.dp, y = 10.dp)
                .rotate(-4f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x55FFFDF9))
                .border(1.dp, Color(0x55CEB89E), RoundedCornerShape(20.dp))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-8).dp, y = 6.dp)
                .rotate(3f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x66FFF7EC))
                .border(1.dp, Color(0x55CEB89E), RoundedCornerShape(20.dp))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
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
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFFFDF8))
                    .border(1.dp, Color(0xFFDCC5A9), RoundedCornerShape(20.dp)),
                content = content
            )
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
            Text("- Kitsune Charm: retry support for tough cards")
            Text("- Fude Hint: reveal a guiding stroke")
            Text("- Radical Lens: highlight the key kanji component")
            Text("Using assists lowers learning score and schedules faster reinforcement.")
            Text("Accepted variants can pass, but canonical JLPT forms score higher.")
            Text("When ready, submit the full deck from the top-right action.")
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Got it")
            }
        }
    }
}

@Composable
private fun PowerUpFooter(
    powerUps: List<PowerUpInventory>,
    selectedPowerUpId: String?,
    onSelectedPowerUpChanged: (String?) -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            powerUps.forEach { powerUp ->
                val isEnabled = powerUp.count > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isEnabled) Color(0xFFFFF1E8) else Color(0xFFF1ECE8))
                        .border(1.dp, Color(0xFFFFC6A5), RoundedCornerShape(12.dp))
                        .clickable {
                            if (isEnabled) {
                                onSelectedPowerUpChanged(
                                    if (selectedPowerUpId == powerUp.id) null else powerUp.id
                                )
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = iconForPowerUp(powerUp.id),
                            contentDescription = powerUp.title,
                            tint = Color(0xFFFF5A00),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = " ${powerUp.count}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF4D3A2F),
                            fontWeight = FontWeight.Medium
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
        "second_chance" -> Icons.Filled.Pets
        "hint_brush" -> Icons.Filled.MenuBook
        "reveal_radical" -> Icons.Filled.AutoFixHigh
        else -> Icons.Filled.AutoFixHigh
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

    LaunchedEffect(resetCounter) {
        strokes = emptyList()
        undoStack = emptyList()
        onSampleChanged(InkSample(emptyList()))
        isEraseMode = false
        operationSnapshotTaken = false
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = Color(0xFFB89B7A), shape = RoundedCornerShape(16.dp))
            .background(Color(0xFFFFFCF6))
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
                        onSampleChanged(strokes.toInkSample())
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
                        onSampleChanged(strokes.toInkSample())
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            for (stroke in strokes) {
                if (stroke.size < 2) continue
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    for (point in stroke.drop(1)) {
                        lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0xFF1E1B18),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
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
                onSampleChanged(InkSample(emptyList()))
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
                    onSampleChanged(strokes.toInkSample())
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

private fun List<List<Offset>>.toInkSample(): InkSample {
    return InkSample(
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
