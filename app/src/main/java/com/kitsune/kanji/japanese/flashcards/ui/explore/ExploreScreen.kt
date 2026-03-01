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
    onTopicPillPressed: (DeckThemeOption) -> Unit,
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
                Text(
                    text = "Explore Topics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Set your topic preferences for daily decks. Tap any topic to select or remove it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${state.selectedTopicTrackIds.size} topic(s) selected for deck mix",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
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
                                if (trackId != null && trackId !in state.selectedTopicTrackIds) {
                                    toastMessage = "${
                                        theme.title
                                    } questions will be included in your daily decks."
                                }
                                onTopicPillPressed(theme)
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
                        Column {
                            Text(
                                text = selectedTheme.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedTheme.difficulty + " \u2022 " + selectedTheme.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (selectedTheme.contentTrackId in state.selectedTopicTrackIds) {
                                "Selected"
                            } else {
                                "Not selected"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTheme.contentTrackId in state.selectedTopicTrackIds) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(
                                    if (selectedTheme.contentTrackId in state.selectedTopicTrackIds) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Topic Curriculum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kitsune adapts this sequence automatically as you progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isIncluded -> MaterialTheme.colorScheme.tertiaryContainer
        else -> Color.White
    }
    val border = if (isSelected || isIncluded) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    } else {
        Color(0xFFE0E0E0)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isIncluded) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
        PackProgressStatus.PASSED -> Color(0xFF3BA56A)
        PackProgressStatus.UNLOCKED -> MaterialTheme.colorScheme.primary
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step ${pack.level}: ${pack.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                if (pack.bestExamScore > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Best score: ${pack.bestExamScore}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF203548)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                tint = Color(0xFF9BE7C4),
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
