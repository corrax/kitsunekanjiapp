package com.kitsune.kanji.japanese.flashcards.ui.profile

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.entity.DeckType
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.domain.model.DeckRunHistoryItem
import com.kitsune.kanji.japanese.flashcards.domain.model.JlptLevelDetail
import com.kitsune.kanji.japanese.flashcards.domain.model.SkillBreakdown
import com.kitsune.kanji.japanese.flashcards.domain.model.UserRankSummary
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals
import com.kitsune.kanji.japanese.flashcards.ui.common.scoreVisualFor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarmSurface = Color(0xFFFFF8F1)
private val AccentOrange = Color(0xFFFF5A00)
private val TextDark = Color(0xFF2D1E14)
private val TextMuted = Color(0xFF7A6355)

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
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val jlptLevelDetails: List<JlptLevelDetail> = emptyList(),
    val selectedLevelIndex: Int = 0,
    val recentRuns: List<DeckRunHistoryItem> = emptyList(),
    val errorMessage: String? = null,
    val isPlusEntitled: Boolean = false,
    val capturesUsedThisWeek: Int = 0,
    val freeWeeklyLimit: Int = 3
)

@Composable
fun ProfileTabScreen(
    state: ProfileTabUiState,
    onOpenRunReport: (String) -> Unit,
    onOpenUpgrade: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onPageChanged: (Int) -> Unit
) {
    val visuals = deckThemeDrawnVisuals(state.selectedThemeId)
    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = state.selectedLevelIndex) {
        state.jlptLevelDetails.size.coerceAtLeast(1)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.selectedLevelIndex) {
            onPageChanged(pagerState.currentPage)
        }
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
                        painter = painterResource(id = R.drawable.ic_launcher_playstore),
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
                state.errorMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item {
                RankCard(rankSummary = state.rankSummary)
            }

            item {
                PlanCard(
                    isPlusEntitled = state.isPlusEntitled,
                    capturesUsedThisWeek = state.capturesUsedThisWeek,
                    freeWeeklyLimit = state.freeWeeklyLimit,
                    onUpgrade = onOpenUpgrade
                )
            }

            item {
                StatsRow(
                    lifetimeScore = state.lifetimeScore,
                    lifetimeCardsReviewed = state.lifetimeCardsReviewed,
                    wordsCovered = state.rankSummary.wordsCovered,
                    currentStreak = state.currentStreak
                )
            }

            if (state.jlptLevelDetails.isNotEmpty()) {
                item {
                    Text(
                        text = "JLPT Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                item {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        val detail = state.jlptLevelDetails.getOrNull(page)
                        if (detail != null) {
                            HeroLevelCard(detail = detail, modifier = Modifier.padding(horizontal = 2.dp))
                        }
                    }
                }

                item {
                    PagerDotIndicators(
                        count = state.jlptLevelDetails.size,
                        currentPage = pagerState.currentPage
                    )
                }

                val currentDetail = state.jlptLevelDetails.getOrNull(state.selectedLevelIndex)
                if (currentDetail != null && currentDetail.totalCount > 0) {
                    item {
                        SkillBreakdownSection(
                            level = currentDetail.level,
                            skills = currentDetail.skills
                        )
                    }
                }
            }

            if (state.recentRuns.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        androidx.compose.material3.TextButton(onClick = onOpenHistory) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentOrange
                            )
                        }
                    }
                }

                items(state.recentRuns) { run ->
                    RunHistoryCard(
                        run = run,
                        onClick = { onOpenRunReport(run.deckRunId) }
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        androidx.compose.material3.TextButton(onClick = onOpenHistory) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentOrange
                            )
                        }
                    }
                    Text(
                        text = "Complete a deck to see your activity here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HeroLevelCard(detail: JlptLevelDetail, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(AccentOrange, Color(0xFFFF8C4E), Color(0x00FF8C4E))
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.level,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        lineHeight = 60.sp
                    )
                    Text(
                        text = if (detail.totalCount > 0) {
                            "${detail.answeredCount} / ${detail.totalCount} cards seen"
                        } else {
                            "Coming soon"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    if (detail.packsTotal > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${detail.packsPassed} / ${detail.packsTotal} packs passed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(88.dp),
                        color = Color(0xFFFFE0CC),
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { detail.completionRatio },
                        modifier = Modifier.size(88.dp),
                        color = AccentOrange,
                        strokeWidth = 8.dp,
                        strokeCap = StrokeCap.Round
                    )
                    val pct = (detail.completionRatio * 100).toInt()
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (detail.packStatuses.isNotEmpty()) {
                PackDotRow(
                    statuses = detail.packStatuses,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PackDotRow(statuses: List<PackProgressStatus>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        statuses.forEach { status ->
            when (status) {
                PackProgressStatus.PASSED -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(AccentOrange)
                )
                PackProgressStatus.UNLOCKED -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, AccentOrange, CircleShape)
                )
                PackProgressStatus.LOCKED -> Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0D5CC))
                )
            }
        }
    }
}

@Composable
private fun PagerDotIndicators(count: Int, currentPage: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(count) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AccentOrange else Color(0xFFE0D5CC))
            )
        }
    }
}

@Composable
private fun SkillBreakdownSection(level: String, skills: List<SkillBreakdown>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$level Skills",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Spacer(Modifier.height(12.dp))
            skills.forEach { skill ->
                SkillBar(skill = skill)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SkillBar(skill: SkillBreakdown) {
    val hasAttempts = skill.attemptCount > 0
    val barColor = if (hasAttempts) scoreVisualFor(skill.avgScore).toneColor else Color(0xFFE0D5CC)
    val progress = if (hasAttempts) skill.avgScore / 100f else 0f
    val scoreText = if (hasAttempts) "${skill.avgScore}%" else "Not started"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = skill.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (hasAttempts) TextDark else TextMuted,
            modifier = Modifier.width(90.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = Color(0xFFFFE0CC)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = scoreText,
            style = MaterialTheme.typography.labelSmall,
            color = if (hasAttempts) barColor else TextMuted,
            fontWeight = if (hasAttempts) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.End
        )
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
                    color = TextMuted
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
                    color = TextMuted
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${rankSummary.wordsCovered} / ${rankSummary.totalWords} words covered",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatsRow(
    lifetimeScore: Int,
    lifetimeCardsReviewed: Int,
    wordsCovered: Int,
    currentStreak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(label = "Score", value = "$lifetimeScore", modifier = Modifier.weight(1f))
        StatCard(label = "Cards", value = "$lifetimeCardsReviewed", modifier = Modifier.weight(1f))
        StatCard(label = "Words", value = "$wordsCovered", modifier = Modifier.weight(1f))
        StatCard(
            label = "Streak",
            value = "\uD83D\uDD25 $currentStreak",
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
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PlanCard(
    isPlusEntitled: Boolean,
    capturesUsedThisWeek: Int,
    freeWeeklyLimit: Int,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPlusEntitled) "Kitsune Plus" else "Free Plan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                if (isPlusEntitled) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1FA080))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            // Capture usage row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Captures this week",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = if (isPlusEntitled) "Unlimited" else "$capturesUsedThisWeek / $freeWeeklyLimit",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!isPlusEntitled && capturesUsedThisWeek >= freeWeeklyLimit) AccentOrange else TextDark
                )
            }

            if (!isPlusEntitled) {
                LinearProgressIndicator(
                    progress = { (capturesUsedThisWeek.toFloat() / freeWeeklyLimit).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (capturesUsedThisWeek >= freeWeeklyLimit) AccentOrange else Color(0xFF1FA080),
                    trackColor = Color(0xFFFFE0CC)
                )
                Button(
                    onClick = onUpgrade,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Upgrade for unlimited captures", fontWeight = FontWeight.SemiBold)
                }
            }
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
                    color = TextMuted
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
                    color = TextMuted
                )
            }
        }
    }
}
