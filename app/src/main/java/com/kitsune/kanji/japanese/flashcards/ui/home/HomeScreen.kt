package com.kitsune.kanji.japanese.flashcards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.entity.PackProgressStatus
import com.kitsune.kanji.japanese.flashcards.domain.model.PackProgress
import com.kitsune.kanji.japanese.flashcards.domain.model.PowerUpInventory
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeById
import java.time.LocalDate
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberDrawerState

@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartDailyDeck: () -> Unit,
    onStartExamPack: (String) -> Unit,
    onSelectPack: (String) -> Unit,
    onDismissDailyReminder: () -> Unit,
    onBrowseDecks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenUpgrade: () -> Unit
) {
    var showDailyPrompt by rememberSaveable { mutableStateOf(false) }
    val selectedPack = remember(state.selectedPackId, state.packs) {
        val selected = state.packs.firstOrNull { it.packId == state.selectedPackId }
        selected ?: state.packs.firstOrNull()
    }
    val selectedDeckTheme = remember(state.selectedDeckThemeId) {
        deckThemeById(state.selectedDeckThemeId)
    }
    val backgroundColors = remember(selectedDeckTheme.id) {
        homeBackgroundForTheme(selectedDeckTheme.id)
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.shouldShowDailyReminder) {
        if (state.shouldShowDailyReminder) {
            showDailyPrompt = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFDF9F5)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                ) {
                    Image(
                        painter = painterResource(id = selectedDeckTheme.heroRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text("Kitsune Menu", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(selectedDeckTheme.title, color = Color(0xFFFFE5D2), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF1E7))
                        .border(1.dp, Color(0xFFFFCAA8), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Rank Lv ${state.rankSummary.level} · ${state.rankSummary.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3A2419)
                    )
                    Text(
                        text = "Words covered ${state.rankSummary.wordsCovered}/${state.rankSummary.totalWords}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6A4A39)
                    )
                    val easy = state.rankSummary.easyWordScore
                    val hard = state.rankSummary.hardWordScore
                    if (easy != null || hard != null) {
                        Text(
                            text = "Easy ${easy ?: "--"} · Hard ${hard ?: "--"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6A4A39)
                        )
                    }
                }
                NavigationDrawerItem(
                    label = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("Profile")
                            Text(
                                text = "${state.rankSummary.title} · Lv ${state.rankSummary.level}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7A6356)
                            )
                        }
                    },
                    icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenProfile()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                NavigationDrawerItem(
                    label = { Text("Upgrade") },
                    icon = { Icon(imageVector = Icons.Filled.WorkspacePremium, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenUpgrade()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Image(
                    painter = painterResource(id = selectedDeckTheme.heroRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.36f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = backgroundColors
                            )
                        )
                )
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    CompositionLocalProvider(LocalContentColor provides Color(0xFF2B1D16)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                    item {
                        SeasonalHeroHeader(
                            powerUps = state.powerUps,
                            selectedDeckTitle = selectedDeckTheme.title
                        )
                    }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Open menu",
                                            tint = Color(0xFFFF5A00)
                                        )
                                    }
                                    Text(
                                        text = "Kitsune: Swipe to Learn",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedDeckTheme.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (selectedDeckTheme.id != "jlpt_n5") {
                                    Text(
                                        text = "Preview deck (content coming soon)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF8E7F73)
                                    )
                                }
                            }
                            TextButton(onClick = onBrowseDecks) {
                                Text("Change")
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StreakPanel(
                                label = "Current Streak",
                                value = state.currentStreak.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StreakPanel(
                                label = "Best Streak",
                                value = state.bestStreak.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Button(
                            onClick = onStartDailyDeck,
                            enabled = !state.isStartingDeck,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = if (state.isStartingDeck) {
                                "Preparing Daily Challenge..."
                            } else {
                                "Start Daily Challenge (15 Cards)"
                            }
                            Text(label)
                        }
                    }
                    item {
                        state.errorMessage?.let { message ->
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item {
                        Text(
                            text = "${selectedDeckTheme.title} Progression Path",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        selectedPack?.let { pack ->
                            FeaturedPackCard(
                                pack = pack,
                                themeId = selectedDeckTheme.id,
                                isLoading = state.startingPackId == pack.packId,
                                onStartExamPack = onStartExamPack
                            )
                        }
                    }
                    itemsIndexed(state.packs, key = { _, pack -> pack.packId }) { index, pack ->
                        PackTimelineItem(
                            pack = pack,
                            themeId = selectedDeckTheme.id,
                            isFirst = index == 0,
                            isLast = index == state.packs.lastIndex,
                            isLoading = state.startingPackId == pack.packId,
                            onStartExamPack = onStartExamPack
                        )
                    }
                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onBrowseDecks,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(18.dp),
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Image(
                        painter = painterResource(selectedDeckTheme.heroRes),
                        contentDescription = "Select pack",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }
            }
        }
    }

    if (showDailyPrompt) {
        AlertDialog(
            onDismissRequest = {
                showDailyPrompt = false
                onDismissDailyReminder()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CardGiftcard,
                    contentDescription = null,
                    tint = Color(0xFF8B3A2E)
                )
            },
            title = { Text("Daily Rewards Waiting") },
            text = {
                Text("Start your daily challenge to keep your streak alive and claim rewards.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDailyPrompt = false
                        onStartDailyDeck()
                    }
                ) {
                    Text("Start Daily Challenge")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDailyPrompt = false
                        onDismissDailyReminder()
                    }
                ) {
                    Text("Later")
                }
            }
        )
    }
}

private fun homeBackgroundForTheme(themeId: String): List<Color> {
    return when (themeId) {
        "food" -> listOf(Color(0x33FFF6EA), Color(0xCCFFFDF9))
        "transport" -> listOf(Color(0x33EAF2FF), Color(0xCCEFF6FF))
        "shopping" -> listOf(Color(0x33FFF3EC), Color(0xCCFFF8F3))
        "daily_life" -> listOf(Color(0x33F0F7EE), Color(0xCCF7FCF6))
        "jlpt_n4" -> listOf(Color(0x33E6F6FF), Color(0xCCF2FAFF))
        "jlpt_n3" -> listOf(Color(0x33F1E9FF), Color(0xCCF8F4FF))
        else -> listOf(Color(0x33FFF1E7), Color(0xCCFFF8F2))
    }
}

@Composable
private fun PowerUpTray(
    powerUps: List<PowerUpInventory>,
    modifier: Modifier = Modifier
) {
    var selectedPowerUpId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = powerUps.firstOrNull { it.id == selectedPowerUpId }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            powerUps.forEach { powerUp ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xE6FFFFFF))
                        .border(1.dp, Color(0xFFFFB990), RoundedCornerShape(12.dp))
                        .clickable {
                            selectedPowerUpId = if (selectedPowerUpId == powerUp.id) null else powerUp.id
                        }
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = iconForPowerUp(powerUp.id),
                            contentDescription = powerUp.title,
                            tint = Color(0xFFFF5A00),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = powerUp.count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2A211B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (selected != null) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xF2FFFFFF))
                    .border(1.dp, Color(0xFFFFB990), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${selected.title} (${selected.count})",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFF5A00),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = selected.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2A211B)
                    )
                }
            }
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
private fun SeasonalHeroHeader(
    powerUps: List<PowerUpInventory>,
    selectedDeckTitle: String
) {
    val now = remember { LocalDate.now() }
    val (seasonLabel, heroImageRes) = remember(now.monthValue) {
        when (now.monthValue) {
            12, 1, 2 -> Pair("Winter Scenery", R.drawable.hero_winter)
            3, 4, 5 -> Pair("Spring Blossoms", R.drawable.hero_spring)
            6, 7, 8 -> Pair("Summer Coastline", R.drawable.hero_summer)
            else -> Pair("Autumn Streets", R.drawable.hero_autumn)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE1C9AD), RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = heroImageRes),
            contentDescription = seasonLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x22000000), Color(0x66000000))
                    )
                )
        ) {
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text(
                text = "Today in Japan",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFFF2E6)
            )
            Text(
                text = seasonLabel,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Active deck: $selectedDeckTitle",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF3E7D9)
            )
        }

        PowerUpTray(
            powerUps = powerUps,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )
    }
}
@Composable
private fun StreakPanel(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFFFCFB4), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PackTimelineItem(
    pack: PackProgress,
    themeId: String,
    isFirst: Boolean,
    isLast: Boolean,
    isLoading: Boolean,
    onStartExamPack: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TimelineNode(
            isFirst = isFirst,
            isLast = isLast,
            status = pack.status,
            themeId = themeId
        )
        PackDeckCard(
            pack = pack,
            themeId = themeId,
            isLoading = isLoading,
            onStartExamPack = onStartExamPack,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineNode(
    isFirst: Boolean,
    isLast: Boolean,
    status: PackProgressStatus,
    themeId: String
) {
    val lineColor = themeLineColor(themeId)
    val dotColor = when (status) {
        PackProgressStatus.LOCKED -> Color(0xFFD6C6B4)
        PackProgressStatus.UNLOCKED -> Color(0xFFFF5A00)
        PackProgressStatus.PASSED -> Color(0xFF3A7A4B)
    }

    Column(
        modifier = Modifier.width(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isFirst) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .background(lineColor)
            )
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
                .border(1.dp, Color(0xFF8B6B55), CircleShape)
        )
        if (!isLast) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(88.dp)
                    .background(lineColor)
            )
        }
    }
}

@Composable
private fun PackDeckCard(
    pack: PackProgress,
    themeId: String,
    isLoading: Boolean,
    onStartExamPack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = themeCardBackground(themeId)
    val border = themeCardBorder(themeId)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(deckThemedPackArt(themeId = themeId, level = pack.level)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.08f,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x00FFFFFF), Color(0x22FFFFFF))
                    )
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PackImagePlaceholder(
                        status = pack.status,
                        themeId = themeId,
                        level = pack.level
                    )
                    Column {
                        Text(
                            text = "Level ${pack.level}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = pack.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (pack.status == PackProgressStatus.LOCKED) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF8E8176),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "Best ${pack.bestExamScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (pack.status != PackProgressStatus.LOCKED) {
                OutlinedButton(
                    onClick = { onStartExamPack(pack.packId) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLoading) "Preparing..." else "Start Exam Pack")
                }
            }
        }
    }
}

@Composable
private fun FeaturedPackCard(
    pack: PackProgress,
    themeId: String,
    isLoading: Boolean,
    onStartExamPack: (String) -> Unit
) {
    val background = themeCardBackground(themeId)
    val border = themeCardBorder(themeId)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(12.dp)
    ) {
        Image(
            painter = painterResource(deckThemedPackArt(themeId = themeId, level = pack.level)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.08f,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x00FFFFFF), Color(0x22FFFFFF))
                    )
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Selected Pack",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PackImagePlaceholder(status = pack.status, themeId = themeId, level = pack.level)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Level ${pack.level}: ${pack.title}", fontWeight = FontWeight.SemiBold)
                    Text("Best ${pack.bestExamScore}", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(
                onClick = { onStartExamPack(pack.packId) },
                enabled = pack.status != PackProgressStatus.LOCKED && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Preparing..." else "Start Selected Pack")
            }
        }
    }
}

@Composable
private fun PackImagePlaceholder(
    status: PackProgressStatus,
    themeId: String,
    level: Int
) {
    val background = when (status) {
        PackProgressStatus.LOCKED -> Color(0xFFE5DDD2)
        PackProgressStatus.UNLOCKED -> Color(0xFFE8D4BC)
        PackProgressStatus.PASSED -> Color(0xFFD3E9D4)
    }

    val imageRes = remember(status, level, themeId) {
        deckThemedPackArt(themeId = themeId, level = level)
    }

    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, Color(0xFFD2BEA6), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (status == PackProgressStatus.LOCKED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFFFEAD2)
                )
            }
        }
    }
}

private fun deckThemedPackArt(themeId: String, level: Int): Int {
    return when (themeId) {
        "jlpt_n5" -> when (level % 4) {
            0 -> R.drawable.pack_scene_temple
            1 -> R.drawable.pack_scene_mountain
            2 -> R.drawable.pack_scene_food
            else -> R.drawable.pack_scene_city
        }

        "jlpt_n4" -> R.drawable.hero_summer
        "jlpt_n3" -> R.drawable.hero_autumn
        "daily_life" -> R.drawable.pack_scene_city
        "food" -> R.drawable.pack_scene_food
        "transport" -> R.drawable.pack_scene_mountain
        "shopping" -> R.drawable.pack_scene_temple
        else -> R.drawable.pack_scene_city
    }
}

private fun themeLineColor(themeId: String): Color {
    return when (themeId) {
        "food" -> Color(0xFFB86A3C)
        "transport" -> Color(0xFF5A7897)
        "shopping" -> Color(0xFF8B6B55)
        "daily_life" -> Color(0xFF7D5E4B)
        "jlpt_n4" -> Color(0xFF4D7A67)
        "jlpt_n3" -> Color(0xFF6A4F8A)
        else -> Color(0xFFB89B7A)
    }
}

private fun themeCardBackground(themeId: String): Color {
    return when (themeId) {
        "food" -> Color(0xFFFFFFFF)
        "transport" -> Color(0xFFFFFFFF)
        "shopping" -> Color(0xFFFFFFFF)
        "daily_life" -> Color(0xFFFFFFFF)
        "jlpt_n4" -> Color(0xFFFFFFFF)
        "jlpt_n3" -> Color(0xFFFFFFFF)
        else -> Color(0xFFFFFFFF)
    }
}

private fun themeCardBorder(themeId: String): Color {
    return when (themeId) {
        "food" -> Color(0xFFFFC4A1)
        "transport" -> Color(0xFFBDD3EF)
        "shopping" -> Color(0xFFFFD0B2)
        "daily_life" -> Color(0xFFFFCFB4)
        "jlpt_n4" -> Color(0xFFB9E2CF)
        "jlpt_n3" -> Color(0xFFD6C8F1)
        else -> Color(0xFFFFC8A8)
    }
}
