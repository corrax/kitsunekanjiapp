package com.kitsune.kanji.japanese.flashcards.ui.learn

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.remember
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
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals
import com.kitsune.kanji.japanese.flashcards.ui.common.scoreVisualFor
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.DeckThemeOption

private val WarmBackground = Color(0xFFFFF8F1)
private val WarmSurface = Color(0xFFFFFFFF)
private val AccentOrange = Color(0xFFFF5A00)
private val TextDark = Color(0xFF2D1E14)
private val TextMuted = Color(0xFF7A5C48)
private val BorderLight = Color(0xFFEDD9C8)

@Composable
fun LearnScreen(
    state: LearnHubUiState,
    onStartDailyDeck: () -> Unit,
    onStartExamPack: (String) -> Unit,
    onThemeSelected: (DeckThemeOption) -> Unit,
    onRefresh: () -> Unit
) {
    val themes = state.availableThemes

    val initialPage = remember(themes, state.selectedThemeId) {
        themes.indexOfFirst { it.id == state.selectedThemeId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = initialPage) { themes.size }

    // When user swipes to a new page, notify ViewModel to load that theme's packs
    LaunchedEffect(pagerState.currentPage) {
        val theme = themes.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (theme.id != state.selectedThemeId) {
            onThemeSelected(theme)
        }
    }

    // If ViewModel changes selectedThemeId externally, scroll pager to match
    LaunchedEffect(state.selectedThemeId) {
        val targetIndex = themes.indexOfFirst { it.id == state.selectedThemeId }.coerceAtLeast(0)
        if (pagerState.currentPage != targetIndex && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Profile Header ──
            item {
                ProfileHeaderSection(
                    state = state,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ── Daily Challenge Card ──
            item {
                DailyChallengeFeaturedCard(
                    state = state,
                    onStart = onStartDailyDeck,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── "Your Decks" header ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Decks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Swipe to browse",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Horizontal Pager of deck categories ──
            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val theme = themes.getOrNull(page)
                    if (theme != null) {
                        DeckCategoryCard(
                            theme = theme,
                            packs = if (theme.id == state.selectedThemeId) state.packs else emptyList(),
                            isLoadingPacks = state.isLoading && theme.id == state.selectedThemeId,
                            startingPackId = state.startingPackId,
                            onPackSelected = onStartExamPack,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── Pager dot indicators ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    themes.forEachIndexed { index, _ ->
                        val isSelected = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isSelected) 8.dp else 5.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AccentOrange else BorderLight)
                        )
                    }
                }
            }
        }

        // Error snack
        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2D1E14))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(
    state: LearnHubUiState,
    modifier: Modifier = Modifier
) {
    val rank = state.rankSummary
    val wordsProgress = if (rank.totalWords > 0) {
        (rank.wordsCovered.toFloat() / rank.totalWords).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Level badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${rank.level}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Rank info + progress
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rank.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lv ${rank.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (rank.totalWords > 0) {
                    LinearProgressIndicator(
                        progress = { wordsProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentOrange,
                        trackColor = BorderLight
                    )
                    Text(
                        text = "${rank.wordsCovered} / ${rank.totalWords} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Streak
            if (state.currentStreak > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${state.currentStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyChallengeFeaturedCard(
    state: LearnHubUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRun = state.dailyActiveRun
    val hasProgress = activeRun != null && activeRun.cardsReviewed > 0
    val isStarted = state.hasStartedDailyChallenge

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Hero image background
            Image(
                painter = painterResource(R.drawable.hero_spring),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.22f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF5A00).copy(alpha = 0.85f), Color(0xFF8B2800).copy(alpha = 0.92f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Today,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Daily Challenge",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                // Title + subtitle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (hasProgress) "Resume where you left off" else "Today's Practice",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (hasProgress && activeRun != null) {
                        Text(
                            text = "${activeRun.cardsReviewed} / ${activeRun.totalCards} cards done",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        LinearProgressIndicator(
                            progress = {
                                (activeRun.cardsReviewed.toFloat() / activeRun.totalCards.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    } else if (!isStarted) {
                        Text(
                            text = "Adaptive cards based on your level",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Start / Resume button
                Button(
                    onClick = onStart,
                    enabled = !state.isStartingDeck,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = AccentOrange
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.wrapContentHeight()
                ) {
                    if (state.isStartingDeck) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentOrange,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when {
                            state.isStartingDeck -> "Starting…"
                            hasProgress -> "Resume"
                            isStarted -> "Continue"
                            else -> "Start Challenge"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckCategoryCard(
    theme: DeckThemeOption,
    packs: List<PackProgress>,
    isLoadingPacks: Boolean,
    startingPackId: String?,
    onPackSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visuals = deckThemeDrawnVisuals(theme.id)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Theme banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                Image(
                    painter = painterResource(theme.heroRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.28f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(visuals.accent.copy(alpha = 0.75f), visuals.accent.copy(alpha = 0.9f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = theme.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${theme.difficulty} · ${theme.levels.size} Levels",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // Pack list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (isLoadingPacks) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = visuals.accent,
                            strokeWidth = 2.dp
                        )
                    }
                } else if (packs.isEmpty()) {
                    // No packs loaded yet — show level names from catalog
                    theme.levels.forEach { level ->
                        PackPlaceholderRow(
                            levelName = level.name,
                            accent = visuals.accent
                        )
                    }
                } else {
                    packs.forEach { pack ->
                        PackRow(
                            pack = pack,
                            accent = visuals.accent,
                            isStarting = pack.packId == startingPackId,
                            onClick = {
                                if (pack.status != PackProgressStatus.LOCKED) {
                                    onPackSelected(pack.packId)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackRow(
    pack: PackProgress,
    accent: Color,
    isStarting: Boolean,
    onClick: () -> Unit
) {
    val isLocked = pack.status == PackProgressStatus.LOCKED
    val hasScore = pack.bestExamScore > 0
    val scoreVisual = if (hasScore) scoreVisualFor(pack.bestExamScore) else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLocked && !isStarting, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Level number badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isLocked) Color(0xFFE8DDD6)
                    else accent.copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFB0A090),
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "${pack.level}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }

        // Pack title
        Text(
            text = pack.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isLocked) FontWeight.Normal else FontWeight.SemiBold,
            color = if (isLocked) TextMuted else TextDark,
            modifier = Modifier.weight(1f)
        )

        // Right side: score or active run or loading
        when {
            isStarting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = accent,
                    strokeWidth = 2.dp
                )
            }
            pack.activeRun != null -> {
                val progress = pack.activeRun.cardsReviewed.toFloat() /
                    pack.activeRun.totalCards.coerceAtLeast(1)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "In progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(60.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.2f)
                    )
                }
            }
            scoreVisual != null -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = scoreVisual.toneColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${pack.bestExamScore}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreVisual.toneColor
                    )
                }
            }
            !isLocked -> {
                Text(
                    text = "Try it",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Divider line
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(BorderLight)
    )
}

@Composable
private fun PackPlaceholderRow(levelName: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.3f))
            )
        }
        Text(
            text = levelName,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(BorderLight)
    )
}
