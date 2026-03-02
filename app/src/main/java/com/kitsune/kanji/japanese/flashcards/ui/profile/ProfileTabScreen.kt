package com.kitsune.kanji.japanese.flashcards.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.JlptLevelProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarmSurface = Color(0xFFFFF8F1)
private val AccentOrange = Color(0xFFFF5A00)
private val TextDark = Color(0xFF2D1E14)

data class ProfileTabUiState(
    val isLoading: Boolean = true,
    val selectedThemeId: String? = null,
    val rankSummary: UserRankSummary = UserRankSummary(
        hiddenRating = 0,
        level = 1,
        title = "Beginner",
        wordsCovered = 0,
        totalWords = 0,
        easyWordScore = null,
        hardWordScore = null
    ),
    val lifetimeScore: Int = 0,
    val lifetimeCardsReviewed: Int = 0,
    val jlptProgress: List<JlptLevelProgress> = defaultJlptProgress(),
    val recentRuns: List<DeckRunHistoryItem> = emptyList(),
    val errorMessage: String? = null
)

@Composable
fun ProfileTabScreen(
    state: ProfileTabUiState,
    onOpenRunReport: (String) -> Unit,
    onOpenUpgrade: () -> Unit
) {
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
            alpha = 0.12f,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AccentOrange)
                    )
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Spacer(Modifier.height(4.dp))
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                RankCard(rankSummary = state.rankSummary)
            }

            item {
                StatsRow(
                    lifetimeScore = state.lifetimeScore,
                    lifetimeCardsReviewed = state.lifetimeCardsReviewed,
                    wordsCovered = state.rankSummary.wordsCovered
                )
            }

            item {
                Text(
                    text = "JLPT Progress (N5 to N1)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(state.jlptProgress) { progress ->
                JlptProgressCard(progress = progress)
            }

            if (state.recentRuns.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(state.recentRuns) { run ->
                    RunHistoryCard(
                        run = run,
                        onClick = { onOpenRunReport(run.deckRunId) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun JlptProgressCard(progress: JlptLevelProgress) {
    val percent = if (progress.totalCount > 0) {
        ((progress.completionRatio * 100f).toInt()).coerceIn(0, 100)
    } else {
        0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = progress.level,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = if (progress.totalCount > 0) {
                        "${progress.answeredCount}/${progress.totalCount} ($percent%)"
                    } else {
                        "0/0"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6355)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.completionRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AccentOrange,
                trackColor = Color(0xFFFFE0CC)
            )
            if (progress.totalCount == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "No JLPT ${progress.level} question bank seeded yet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A6355)
                )
            }
        }
    }
}

@Composable
private fun RankCard(rankSummary: UserRankSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Orange accent gradient at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(AccentOrange, Color(0xFFFF8C4E), Color(0x00FF8C4E))
                        ),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Kitsune Learner",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF7A6355)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = rankSummary.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Level ${rankSummary.level} \u2022 Rating ${rankSummary.hiddenRating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7A6355)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${rankSummary.wordsCovered} / ${rankSummary.totalWords} words covered",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6355)
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    lifetimeScore: Int,
    lifetimeCardsReviewed: Int,
    wordsCovered: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Score",
            value = "$lifetimeScore",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Cards",
            value = "$lifetimeCardsReviewed",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Words",
            value = "$wordsCovered",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7A6355)
            )
        }
    }
}

@Composable
private fun RunHistoryCard(run: DeckRunHistoryItem, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy \u2022 HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(run.submittedAtEpochMillis))
    val typeLabel = when (run.deckType) {
        DeckType.DAILY -> "Daily"
        DeckType.EXAM -> "Exam"
        DeckType.REMEDIAL -> "Remedial"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$typeLabel \u2022 $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6355)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${run.cardsReviewed} cards reviewed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${run.totalScore}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        run.totalScore >= 80 -> Color(0xFF2E8B57)
                        run.totalScore >= 60 -> Color(0xFFD4880F)
                        else -> Color(0xFFCC3D33)
                    }
                )
                Text(
                    text = run.grade,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF7A6355)
                )
            }
        }
    }
}

private fun defaultJlptProgress(): List<JlptLevelProgress> {
    return listOf(
        JlptLevelProgress(level = "N5", trackId = "jlpt_n5_core", answeredCount = 0, totalCount = 0),
        JlptLevelProgress(level = "N4", trackId = "jlpt_n4_core", answeredCount = 0, totalCount = 0),
        JlptLevelProgress(level = "N3", trackId = "jlpt_n3_core", answeredCount = 0, totalCount = 0),
        JlptLevelProgress(level = "N2", trackId = "jlpt_n2_core", answeredCount = 0, totalCount = 0),
        JlptLevelProgress(level = "N1", trackId = "jlpt_n1_core", answeredCount = 0, totalCount = 0)
    )
}
