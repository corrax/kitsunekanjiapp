package com.kitsune.kanji.japanese.flashcards.ui.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.DeckThemeOption
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import kotlinx.coroutines.delay

private val AccentOrange = Color(0xFFFF5A00)
private val WarmSurface = Color(0xFFFFF8F1)
private val TextDark = Color(0xFF2D1E14)

data class ExploreUiState(
    val isLoading: Boolean = true,
    val selectedThemeId: String? = null,
    val selectedTopicTrackIds: Set<String> = emptySet(),
    val packs: List<PackProgress> = emptyList(),
    val errorMessage: String? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    state: ExploreUiState,
    onTopicSelected: (DeckThemeOption) -> Unit,
    onTopicSelectionToggled: (DeckThemeOption) -> Unit,
    onStartExamPack: (String) -> Unit
) {
    var toastMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2200)
            toastMessage = null
        }
    }
    val visuals = deckThemeDrawnVisuals(state.selectedThemeId)

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(visuals.baseColor)
    ) {
        Image(
            painter = painterResource(id = visuals.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.13f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(visuals.overlayTop, visuals.overlayBottom)
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Column {
                    Text(
                        text = "Explore Topics",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(3.dp)
                            .background(AccentOrange, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Set your topic preferences for daily decks. Tap any topic to select or remove it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7A6355)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.selectedTopicTrackIds.size} topic(s) selected for deck mix",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentOrange,
                    fontWeight = FontWeight.SemiBold
                )
                state.errorMessage?.let { message ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    deckThemeCatalog.forEach { theme ->
                        val trackId = theme.contentTrackId
                        val isIncluded = trackId != null && trackId in state.selectedTopicTrackIds
                        val isSelected = theme.id == state.selectedThemeId
                        TopicChip(
                            label = theme.title,
                            isIncluded = isIncluded,
                            isSelected = isSelected,
                            onClick = {
                                onTopicSelected(theme)
                            }
                        )
                    }
                }
            }

            val selectedTheme = deckThemeCatalog.firstOrNull { it.id == state.selectedThemeId }
            if (selectedTheme != null) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedTheme.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Text(
                                text = selectedTheme.difficulty + " \u2022 " + selectedTheme.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A6355)
                            )
                        }
                        val isIncluded = selectedTheme.contentTrackId in state.selectedTopicTrackIds
                        Button(
                            onClick = {
                                val trackId = selectedTheme.contentTrackId
                                if (trackId != null && trackId !in state.selectedTopicTrackIds) {
                                    toastMessage = "${
                                        selectedTheme.title
                                    } questions will be included in your daily decks."
                                }
                                onTopicSelectionToggled(selectedTheme)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIncluded) Color(0xFFE8D5C8) else AccentOrange,
                                contentColor = if (isIncluded) Color(0xFF7A6355) else Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = ButtonDefaults.ContentPadding,
                            modifier = Modifier.height(36.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = if (isIncluded) "Unselect" else "Select",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Topic Curriculum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "Kitsune adapts this sequence automatically as you progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A6355)
                    )
                }

                itemsIndexed(state.packs) { index, pack ->
                    TopicCurriculumStep(
                        pack = pack,
                        isLast = index == state.packs.lastIndex,
                        onStart = { onStartExamPack(pack.packId) }
                    )
                }
            }

            item { Spacer(Modifier.height(96.dp)) }
        }

        toastMessage?.let { message ->
            PreferenceToast(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 88.dp)
            )
        }
    }
}

@Composable
private fun TopicChip(
    label: String,
    isIncluded: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        isSelected -> Color(0xFFFFE0CC)
        isIncluded -> Color(0xFFFFEDE5)
        else -> Color.White
    }
    val borderColor = when {
        isSelected -> AccentOrange
        isIncluded -> Color(0xFFFFBE9B)
        else -> Color(0xFFE0E0E0)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isIncluded) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(14.dp),
                tint = AccentOrange
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected || isIncluded) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected || isIncluded) TextDark else Color(0xFF5A4A3E)
        )
    }
}

@Composable
private fun TopicCurriculumStep(
    pack: PackProgress,
    isLast: Boolean,
    onStart: () -> Unit
) {
    val accent = when (pack.status) {
        PackProgressStatus.PASSED -> Color(0xFF2E8B57)
        PackProgressStatus.UNLOCKED -> AccentOrange
        PackProgressStatus.LOCKED -> Color(0xFFC4C7CF)
    }
    val progress = when (pack.status) {
        PackProgressStatus.PASSED -> 1f
        PackProgressStatus.UNLOCKED -> (pack.bestExamScore.coerceAtLeast(25) / 100f).coerceIn(0.25f, 1f)
        PackProgressStatus.LOCKED -> 0f
    }
    val statusText = when (pack.status) {
        PackProgressStatus.PASSED -> "Completed"
        PackProgressStatus.UNLOCKED -> if (pack.bestExamScore > 0) "In progress" else "Ready in curriculum"
        PackProgressStatus.LOCKED -> "Scheduled in curriculum"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                if (pack.status == PackProgressStatus.PASSED) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(3.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (pack.status == PackProgressStatus.PASSED) accent else Color(0xFFD7D9E0))
                )
            }
        }

        Spacer(Modifier.width(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (pack.status != PackProgressStatus.LOCKED) {
                        Modifier.clickable(onClick = onStart)
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = WarmSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row {
                // Left accent border
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .background(accent, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Step ${pack.level}: ${pack.title}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7A6355)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = Color(0xFFE8D5C8)
                    )
                    if (pack.bestExamScore > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Best score: ${pack.bestExamScore}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A6355)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToast(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(272.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1E14)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFFFBE9B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
        }
    }
}
