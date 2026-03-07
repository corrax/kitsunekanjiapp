package com.kitsune.kanji.japanese.flashcards.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingManager
import com.kitsune.kanji.japanese.flashcards.data.local.EducationalGoal
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.ui.billing.InlinePaywallContent
import com.kitsune.kanji.japanese.flashcards.ui.common.deckThemeDrawnVisuals
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import kotlinx.coroutines.launch

data class OnboardingSelection(
    val learnerLevel: LearnerLevel,
    val educationalGoal: EducationalGoal,
    val topicTrackIds: Set<String>
)

@Composable
fun OnboardingScreen(
    billingManager: BillingManager,
    onCompleteFree: (OnboardingSelection) -> Unit,
    onActivated: (OnboardingSelection) -> Unit
) {
    var selectedLevel by rememberSaveable { mutableStateOf(LearnerLevel.BEGINNER_N5.name) }
    var selectedGoal by rememberSaveable { mutableStateOf(EducationalGoal.CASUAL.name) }
    var selectedTopicTrackIds by rememberSaveable {
        mutableStateOf(
            defaultTopicTrackIds(
                goal = EducationalGoal.CASUAL,
                level = LearnerLevel.BEGINNER_N5
            )
        )
    }
    var topicSelectionCustomized by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationPermissionSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationsGranted by rememberSaveable {
        mutableStateOf(notificationPermissionGranted(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }

    val learnerLevel = runCatching { LearnerLevel.valueOf(selectedLevel) }
        .getOrDefault(LearnerLevel.BEGINNER_N5)
    val educationalGoal = runCatching { EducationalGoal.valueOf(selectedGoal) }
        .getOrDefault(EducationalGoal.JLPT_OR_CLASSES)

    LaunchedEffect(educationalGoal, learnerLevel) {
        if (!topicSelectionCustomized) {
            selectedTopicTrackIds = defaultTopicTrackIds(educationalGoal, learnerLevel)
        }
    }

    val onboardingSelection = OnboardingSelection(
        learnerLevel = learnerLevel,
        educationalGoal = educationalGoal,
        topicTrackIds = selectedTopicTrackIds.toSet()
    )

    val slides = remember(educationalGoal) { slidesForGoal(educationalGoal) }

    val firstPage = 0
    val introPageStart = 1
    val reminderPage = introPageStart + slides.size
    val finalPage = reminderPage + 1
    val totalPages = finalPage + 1

    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val theme = onboardingThemeForPage(
                    page = page,
                    educationalGoal = educationalGoal,
                    selectedTopicTrackIds = selectedTopicTrackIds.toSet()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.baseColor)
                ) {
                    Image(
                        painter = painterResource(id = theme.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.18f,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(theme.overlayTop, theme.overlayBottom)
                                )
                            )
                    )

                    val slideModifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = if (page < finalPage) 120.dp else 8.dp)

                    when {
                        page == firstPage -> GoalSetupSlide(
                            learnerLevel = learnerLevel,
                            educationalGoal = educationalGoal,
                            selectedTopicTrackIds = selectedTopicTrackIds.toSet(),
                            onLearnerLevelChange = { selectedLevel = it.name },
                            onGoalChange = { selectedGoal = it.name },
                            onTopicToggle = { trackId ->
                                topicSelectionCustomized = true
                                selectedTopicTrackIds = selectedTopicTrackIds.toMutableList().apply {
                                    if (contains(trackId)) {
                                        if (size > 1) {
                                            remove(trackId)
                                        }
                                    } else {
                                        if (size < 4) {
                                            add(trackId)
                                        }
                                    }
                                }
                            },
                            modifier = slideModifier
                        )

                        page in introPageStart until reminderPage -> IntroSlide(
                            slide = slides[page - introPageStart],
                            modifier = slideModifier
                        )

                        page == reminderPage -> ReminderSlide(
                            isPermissionSupported = notificationPermissionSupported,
                            isPermissionGranted = notificationsGranted,
                            onRequestPermission = {
                                if (notificationPermissionSupported && !notificationsGranted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            modifier = slideModifier
                        )

                        else -> PlansSlide(
                            learnerLevel = learnerLevel,
                            educationalGoal = educationalGoal,
                            billingManager = billingManager,
                            selection = onboardingSelection,
                            onCompleteFree = { onCompleteFree(onboardingSelection) },
                            onActivated = { onActivated(onboardingSelection) },
                            modifier = slideModifier
                        )
                    }
                }
            }

            if (pagerState.currentPage < finalPage) {
                val controlsTheme = onboardingThemeForPage(
                    page = pagerState.currentPage,
                    educationalGoal = educationalGoal,
                    selectedTopicTrackIds = selectedTopicTrackIds.toSet()
                )
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = controlsTheme.panelColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .border(1.dp, controlsTheme.panelBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(totalPages) { index ->
                                val active = index == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .width(if (active) 24.dp else 8.dp)
                                        .height(8.dp)
                                        .background(
                                            color = if (active) controlsTheme.accent else controlsTheme.accentMuted,
                                            shape = RoundedCornerShape(50)
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5A00),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            val label = if (pagerState.currentPage == firstPage) "Continue" else "Next"
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

private data class OnboardingTheme(
    val imageRes: Int,
    val baseColor: Color,
    val overlayTop: Color,
    val overlayBottom: Color,
    val panelColor: Color,
    val panelBorder: Color,
    val accent: Color,
    val accentMuted: Color
)

private fun onboardingThemeForPage(
    page: Int,
    educationalGoal: EducationalGoal,
    selectedTopicTrackIds: Set<String>
): OnboardingTheme {
    val selectedThemeIds = selectedTopicTrackIds
        .mapNotNull { trackId ->
            deckThemeCatalog.firstOrNull { it.contentTrackId == trackId }?.id
        }
    val goalThemeIds = when (educationalGoal) {
        EducationalGoal.CASUAL -> listOf("konbini", "food", "signs")
        EducationalGoal.EVERYDAY_USE -> listOf("konbini", "transport", "adulting")
        EducationalGoal.SCHOOL_OR_WORK -> listOf("school", "work", "signs")
        EducationalGoal.JLPT_OR_CLASSES -> listOf("jlpt_n5", "jlpt_n4", "jlpt_n3")
    }
    val themeCycle = (selectedThemeIds + goalThemeIds + listOf("jlpt_n5"))
        .distinct()
        .ifEmpty { listOf("jlpt_n5") }
    val visuals = deckThemeDrawnVisuals(themeCycle[page % themeCycle.size])
    return OnboardingTheme(
        imageRes = visuals.imageRes,
        baseColor = visuals.baseColor,
        overlayTop = visuals.overlayTop,
        overlayBottom = visuals.overlayBottom,
        panelColor = visuals.panelColor,
        panelBorder = visuals.panelBorder,
        accent = visuals.accent,
        accentMuted = visuals.accentMuted
    )
}

@Composable
private fun GoalSetupSlide(
    learnerLevel: LearnerLevel,
    educationalGoal: EducationalGoal,
    selectedTopicTrackIds: Set<String>,
    onLearnerLevelChange: (LearnerLevel) -> Unit,
    onGoalChange: (EducationalGoal) -> Unit,
    onTopicToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Branded header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_playstore),
                contentDescription = "Kitsune",
                modifier = Modifier.size(44.dp)
            )
            Column {
                Text(
                    text = "Kitsune",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D1E14)
                )
                Text(
                    text = "Japanese, one card at a time",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7A6355)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Set Up Your Path",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D1E14)
        )
        Text(
            text = "Personalize your daily deck, difficulty, and learning focus.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF604A3D)
        )

        // Goal section
        SectionCard {
            EducationalGoalSelector(
                selected = educationalGoal,
                onSelected = onGoalChange
            )
        }

        // Level section
        SectionCard {
            LearnerLevelSelector(
                selected = learnerLevel,
                onSelected = onLearnerLevelChange
            )
        }

        // Topics section
        SectionCard {
            TopicPreferenceSelector(
                selectedTrackIds = selectedTopicTrackIds,
                onToggle = onTopicToggle
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFD1B3), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun IntroSlide(slide: OnboardingSlide, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Japan-themed decorative background elements
        JapanDecorations(slide.decoration)

        // Decorative watermark kanji
        Text(
            text = slide.watermark,
            fontSize = 220.sp,
            fontWeight = FontWeight.Black,
            color = Color(0x0AFF5A00),
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Themed icon above the card
            if (slide.icon != null) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0x22FF5A00), Color.Transparent)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = Color(0xFFFF5A00),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFCEAF), RoundedCornerShape(20.dp))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left accent stripe
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(160.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFFF5A00), Color(0xFFFF8C4E))
                                ),
                                RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                            )
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D1E14)
                        )
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(3.dp)
                                .background(Color(0xFFFF5A00), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                            color = Color(0xFF4D392D)
                        )
                    }
                }
            }
        }
    }
}

private enum class SlideDecoration { SAKURA, WAVES, TORII, LANTERN }

@Composable
private fun JapanDecorations(decoration: SlideDecoration) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val decorColor = Color(0x14FF5A00)
        val decorColorLight = Color(0x0CFF5A00)

        when (decoration) {
            SlideDecoration.SAKURA -> {
                // Scattered sakura petals
                val petalColor = Color(0x18FFB3C1)
                val petalAccent = Color(0x14FF8FA3)
                listOf(
                    Offset(w * 0.12f, h * 0.10f) to 14f,
                    Offset(w * 0.85f, h * 0.08f) to 10f,
                    Offset(w * 0.06f, h * 0.55f) to 12f,
                    Offset(w * 0.92f, h * 0.48f) to 16f,
                    Offset(w * 0.78f, h * 0.78f) to 11f,
                    Offset(w * 0.20f, h * 0.82f) to 9f,
                    Offset(w * 0.55f, h * 0.06f) to 8f,
                    Offset(w * 0.40f, h * 0.90f) to 13f,
                ).forEach { (center, radius) ->
                    // 5-petal flower shape approximated with overlapping circles
                    val r = radius * density
                    for (i in 0 until 5) {
                        val angle = (i * 72.0 - 90.0) * (Math.PI / 180.0)
                        val px = center.x + (r * 0.5f * kotlin.math.cos(angle)).toFloat()
                        val py = center.y + (r * 0.5f * kotlin.math.sin(angle)).toFloat()
                        drawCircle(petalColor, r * 0.55f, Offset(px, py))
                    }
                    drawCircle(petalAccent, r * 0.25f, center)
                }
            }
            SlideDecoration.WAVES -> {
                // Seigaiha-inspired wave arcs
                val waveColor = Color(0x10FF5A00)
                val arcSpacing = 60f * density
                for (row in 0..4) {
                    val yBase = h * 0.08f + row * arcSpacing
                    val xOffset = if (row % 2 == 0) 0f else arcSpacing * 0.5f
                    var x = -arcSpacing + xOffset
                    while (x < w + arcSpacing) {
                        for (ring in 1..3) {
                            drawCircle(
                                waveColor,
                                ring * arcSpacing * 0.15f,
                                Offset(x, yBase),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.2f * density
                                )
                            )
                        }
                        x += arcSpacing
                    }
                }
            }
            SlideDecoration.TORII -> {
                // Torii gate silhouettes at different scales
                val gateColor = Color(0x12FF5A00)
                listOf(
                    Triple(w * 0.15f, h * 0.12f, 0.6f),
                    Triple(w * 0.80f, h * 0.72f, 0.8f),
                    Triple(w * 0.50f, h * 0.05f, 0.4f),
                ).forEach { (cx, cy, scale) ->
                    val s = 40f * density * scale
                    // Pillars
                    drawRect(gateColor, Offset(cx - s * 0.4f, cy), androidx.compose.ui.geometry.Size(s * 0.08f, s * 1.2f))
                    drawRect(gateColor, Offset(cx + s * 0.32f, cy), androidx.compose.ui.geometry.Size(s * 0.08f, s * 1.2f))
                    // Top beam
                    drawRect(gateColor, Offset(cx - s * 0.55f, cy - s * 0.05f), androidx.compose.ui.geometry.Size(s * 1.1f, s * 0.1f))
                    // Second beam
                    drawRect(gateColor, Offset(cx - s * 0.42f, cy + s * 0.2f), androidx.compose.ui.geometry.Size(s * 0.84f, s * 0.06f))
                }
                // Floating circles like lanterns
                listOf(
                    Offset(w * 0.08f, h * 0.45f) to 18f,
                    Offset(w * 0.90f, h * 0.30f) to 14f,
                ).forEach { (center, radius) ->
                    drawCircle(decorColorLight, radius * density, center)
                }
            }
            SlideDecoration.LANTERN -> {
                // Paper lantern shapes
                val lanternColor = Color(0x14FF5A00)
                val lanternGlow = Color(0x0AFFB366)
                listOf(
                    Triple(w * 0.10f, h * 0.15f, 0.7f),
                    Triple(w * 0.88f, h * 0.10f, 0.5f),
                    Triple(w * 0.05f, h * 0.70f, 0.6f),
                    Triple(w * 0.92f, h * 0.65f, 0.8f),
                    Triple(w * 0.75f, h * 0.85f, 0.4f),
                ).forEach { (cx, cy, scale) ->
                    val r = 20f * density * scale
                    // Glow
                    drawCircle(lanternGlow, r * 1.6f, Offset(cx, cy))
                    // Lantern body (oval)
                    drawOval(
                        lanternColor,
                        Offset(cx - r * 0.6f, cy - r),
                        androidx.compose.ui.geometry.Size(r * 1.2f, r * 2f)
                    )
                    // Top cap
                    drawRect(lanternColor, Offset(cx - r * 0.3f, cy - r * 1.1f), androidx.compose.ui.geometry.Size(r * 0.6f, r * 0.2f))
                    // String
                    drawLine(lanternColor, Offset(cx, cy - r * 1.1f), Offset(cx, cy - r * 1.6f), strokeWidth = 1f * density)
                }
            }
        }
    }
}

@Composable
private fun ReminderSlide(
    isPermissionSupported: Boolean,
    isPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (isPermissionGranted) {
        "Daily reminders enabled"
    } else {
        "Stay On Your Streak"
    }

    val body = if (!isPermissionSupported) {
        "Your Android version handles notifications automatically. We'll remind you at your chosen daily time."
    } else if (isPermissionGranted) {
        "Kitsune will nudge you when it's time to practice. Streaks are easier to keep with a gentle reminder."
    } else {
        "A daily nudge helps you stay consistent. Enable notifications so Kitsune can remind you when it's time to practice."
    }

    Box(modifier = Modifier.fillMaxSize()) {
        JapanDecorations(SlideDecoration.LANTERN)
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFCEAF), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isPermissionGranted) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.Notifications
                        },
                    contentDescription = null,
                    tint = Color(0xFFFF5A00),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D1E14)
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4D392D)
                )

                if (isPermissionSupported) {
                    Button(
                        onClick = onRequestPermission,
                        enabled = !isPermissionGranted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5A00),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE8D5C8),
                            disabledContentColor = Color(0xFF7A6355)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (isPermissionGranted) "Reminders Enabled" else "Enable Daily Reminders",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!isPermissionGranted) {
                        Text(
                            text = "You can also enable this later in Android Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7A6355)
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun PlansSlide(
    learnerLevel: LearnerLevel,
    educationalGoal: EducationalGoal,
    billingManager: BillingManager,
    selection: OnboardingSelection,
    onCompleteFree: () -> Unit,
    onActivated: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize()) {
        JapanDecorations(SlideDecoration.SAKURA)

        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_playstore),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Snap It. Learn It. Remember It.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1E14)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Point your camera at any Japanese text and Kitsune turns it into a flashcard queued straight into your daily deck.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A6355)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Feature comparison card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFFD1B3), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PlanFeatureRow(
                        feature = "Camera capture → daily deck cards",
                        freeValue = "3 / week",
                        plusValue = "Unlimited"
                    )
                    PlanFeatureRow(
                        feature = "Daily quiz decks",
                        freeValue = "\u2713",
                        plusValue = "\u2713"
                    )
                    PlanFeatureRow(
                        feature = "All topic tracks",
                        freeValue = "\u2713",
                        plusValue = "\u2713"
                    )
                    PlanFeatureRow(
                        feature = "Progress analytics",
                        freeValue = "\u2713",
                        plusValue = "\u2713"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Inline paywall: same plan options and CTA as PaywallScreen; user can subscribe or skip here
            InlinePaywallContent(
                billingManager = billingManager,
                preferTrial = true,
                onContinueFree = onCompleteFree,
                onActivated = onActivated
            )
        }

        // X at top right (drawn last so it stays on top for paywall compliance)
        IconButton(
            onClick = onCompleteFree,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                tint = Color(0xFF2D1E14)
            )
        }
    }
}

@Composable
private fun PlanFeatureRow(feature: String, freeValue: String, plusValue: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4D392D),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = freeValue,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF7A6355),
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = plusValue,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF5A00),
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
    }
}

private fun summaryForSelection(goal: EducationalGoal, level: LearnerLevel): String {
    val goalSummary = when (goal) {
        EducationalGoal.CASUAL -> "Conversation-first"
        EducationalGoal.EVERYDAY_USE -> "Everyday Japanese"
        EducationalGoal.SCHOOL_OR_WORK -> "School & work scenarios"
        EducationalGoal.JLPT_OR_CLASSES -> "JLPT-focused progression"
    }
    val levelSummary = when (level) {
        LearnerLevel.PRE_N5 -> "starting from the very basics"
        LearnerLevel.BEGINNER_N5 -> "starting around JLPT N5"
        LearnerLevel.BEGINNER_PLUS_N4 -> "starting around JLPT N4"
        LearnerLevel.INTERMEDIATE_N3 -> "starting around JLPT N3"
        LearnerLevel.ADVANCED_N2 -> "starting around JLPT N2+"
        LearnerLevel.UNSURE -> "starting with a guided baseline"
    }
    return "$goalSummary setup, $levelSummary."
}

private fun slidesForGoal(goal: EducationalGoal): List<OnboardingSlide> {
    return when (goal) {
        EducationalGoal.CASUAL -> listOf(
            OnboardingSlide(
                title = "Pick Up Japanese Naturally",
                body = "Conversation-first decks focus on practical words and phrases you can use right away. No textbook order \u2014 just what matters most.",
                watermark = "\u8A71",
                icon = Icons.Outlined.AutoStories,
                decoration = SlideDecoration.SAKURA
            ),
            OnboardingSlide(
                title = "Daily Practice That Fits Your Pace",
                body = "Short sessions train fast comprehension so everyday dialogue feels easier. Adaptive reranking keeps challenge high without burning you out.",
                watermark = "\u65E5",
                icon = Icons.Outlined.Schedule,
                decoration = SlideDecoration.WAVES
            ),
            OnboardingSlide(
                title = "Build Habits, Not Homework",
                body = "Daily decks stay compact at 15-18 cards. Progress is automatic \u2014 Kitsune moves you forward as your scores improve.",
                watermark = "\u529B",
                icon = Icons.Outlined.TrendingUp,
                decoration = SlideDecoration.TORII
            ),
            OnboardingSlide(
                title = "Snap It, Learn It",
                body = "Spotted something at a konbini or izakaya you can\u2019t read? Tap the camera icon, take a photo \u2014 Kitsune reads the Japanese for you and adds it to your learning plan. No dictionary detour.",
                watermark = "\u64ae",
                icon = Icons.Outlined.CameraAlt,
                decoration = SlideDecoration.LANTERN
            )
        )

        EducationalGoal.EVERYDAY_USE -> listOf(
            OnboardingSlide(
                title = "Japanese for Real Life",
                body = "Core daily-life words are introduced first, then reinforced through realistic sentence use. From reading menus to catching trains.",
                watermark = "\u751F",
                icon = Icons.Outlined.AutoStories,
                decoration = SlideDecoration.TORII
            ),
            OnboardingSlide(
                title = "Handle Real Situations With Confidence",
                body = "School, work, and conversation cards rotate based on your score to target practical gaps you actually need to fill.",
                watermark = "\u8A71",
                icon = Icons.Outlined.TrendingUp,
                decoration = SlideDecoration.WAVES
            ),
            OnboardingSlide(
                title = "Grammar That Sticks",
                body = "Lower levels use guided choices, then graduate to cloze writing as you improve. Every pattern is linked to real examples.",
                watermark = "\u6587",
                icon = Icons.Outlined.Schedule,
                decoration = SlideDecoration.SAKURA
            ),
            OnboardingSlide(
                title = "Real Life Is Your Classroom",
                body = "Point your camera at a menu, a package label, or a missed delivery slip. Kitsune extracts the vocabulary and queues it straight into your daily practice \u2014 no typing required.",
                watermark = "\u8AAD",
                icon = Icons.Outlined.CameraAlt,
                decoration = SlideDecoration.LANTERN
            )
        )

        EducationalGoal.SCHOOL_OR_WORK -> listOf(
            OnboardingSlide(
                title = "Level Up for School & Work",
                body = "Focused tracks cover classroom, office, meetings, requests, and deadlines. Professional Japanese with practical drills.",
                watermark = "\u5B66",
                icon = Icons.Outlined.AutoStories,
                decoration = SlideDecoration.WAVES
            ),
            OnboardingSlide(
                title = "Professional Clarity, Faster",
                body = "Polite forms and pattern drills are reinforced with context-based sentence cards. Practice what you'll actually say.",
                watermark = "\u4ED5",
                icon = Icons.Outlined.TrendingUp,
                decoration = SlideDecoration.TORII
            ),
            OnboardingSlide(
                title = "Track What Matters",
                body = "Adaptive scoring surfaces weak points early so high-value terms get repeated sooner. Study smarter, not longer.",
                watermark = "\u9032",
                icon = Icons.Outlined.Schedule,
                decoration = SlideDecoration.SAKURA
            ),
            OnboardingSlide(
                title = "Capture What Comes Up",
                body = "See an unfamiliar term in a document, sign, or presentation? Snap it with Kitsune\u2019s camera. It turns the text into a review card ready for your next session \u2014 so nothing useful slips by.",
                watermark = "\u5199",
                icon = Icons.Outlined.CameraAlt,
                decoration = SlideDecoration.LANTERN
            )
        )

        EducationalGoal.JLPT_OR_CLASSES -> listOf(
            OnboardingSlide(
                title = "Ace Your JLPT Goals",
                body = "Progressive decks follow JLPT-aligned difficulty while still adapting to your true performance. Targeted practice across N5\u2013N2.",
                watermark = "\u8A66",
                icon = Icons.Outlined.AutoStories,
                decoration = SlideDecoration.SAKURA
            ),
            OnboardingSlide(
                title = "Sharpen Grammar & Reading",
                body = "Pattern cards and sentence checks target mistakes that typically reduce test scores. Every card builds on shared examples.",
                watermark = "\u8AAD",
                icon = Icons.Outlined.TrendingUp,
                decoration = SlideDecoration.TORII
            ),
            OnboardingSlide(
                title = "Study Smarter, Not Longer",
                body = "Shared sentence examples train vocab, grammar, and comprehension together \u2014 reducing overload and reinforcing connections.",
                watermark = "\u7D50",
                icon = Icons.Outlined.Schedule,
                decoration = SlideDecoration.WAVES
            ),
            OnboardingSlide(
                title = "Vocab in the Wild Counts Too",
                body = "Spot a kanji in a study sheet, textbook, or sign you don\u2019t recognize? Snap it. Kitsune pulls out the term and adds it to your practice queue \u2014 JLPT prep meets real-world input.",
                watermark = "\u529B",
                icon = Icons.Outlined.CameraAlt,
                decoration = SlideDecoration.LANTERN
            )
        )
    }
}

@Composable
private fun EducationalGoalSelector(
    selected: EducationalGoal,
    onSelected: (EducationalGoal) -> Unit
) {
    val options = listOf(
        EducationalGoal.CASUAL to "Casual",
        EducationalGoal.EVERYDAY_USE to "Learning for everyday use",
        EducationalGoal.SCHOOL_OR_WORK to "Studying for school or work",
        EducationalGoal.JLPT_OR_CLASSES to "Studying for JLPT or classes"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Goal")
        options.forEach { (goal, title) ->
            SelectableOnboardingRow(
                text = title,
                selected = selected == goal,
                onClick = { onSelected(goal) }
            )
        }
    }
}

@Composable
private fun LearnerLevelSelector(
    selected: LearnerLevel,
    onSelected: (LearnerLevel) -> Unit
) {
    val options = listOf(
        LearnerLevel.PRE_N5 to "Complete Beginner (Pre-N5)",
        LearnerLevel.BEGINNER_N5 to "Beginner (~JLPT N5)",
        LearnerLevel.BEGINNER_PLUS_N4 to "Beginner+ (~JLPT N4)",
        LearnerLevel.INTERMEDIATE_N3 to "Intermediate (~JLPT N3)",
        LearnerLevel.ADVANCED_N2 to "Advanced (~JLPT N2+)",
        LearnerLevel.UNSURE to "Not sure"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Current Japanese level")
        options.forEach { (level, title) ->
            SelectableOnboardingRow(
                text = title,
                selected = selected == level,
                onClick = { onSelected(level) }
            )
        }
    }
}

@Composable
private fun TopicPreferenceSelector(
    selectedTrackIds: Set<String>,
    onToggle: (String) -> Unit
) {
    val options = remember {
        deckThemeCatalog
            .mapNotNull { theme ->
                val trackId = theme.contentTrackId ?: return@mapNotNull null
                TopicPreferenceOption(trackId = trackId, label = theme.title)
            }
            .distinctBy { it.trackId }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("What do you want to read first?")
        Text(
            text = "Pick 1\u20134 real-life scenarios. We\u2019ll build your daily decks around them.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF7A6355)
        )
        options.forEach { option ->
            val selected = option.trackId in selectedTrackIds
            SelectableTopicRow(
                text = option.label,
                selected = selected,
                onClick = { onToggle(option.trackId) }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(Color(0xFFFF5A00), RoundedCornerShape(2.dp))
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D1E14)
        )
    }
}

@Composable
private fun SelectableOnboardingRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFFF0E5) else Color(0xFFFFFFFF))
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFFF8C4E) else Color(0xFFE8D5C8),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFFF5A00),
                unselectedColor = Color(0xFFD5C4B8)
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color(0xFF2D1E14) else Color(0xFF5A4A3E),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
private fun SelectableTopicRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFFF0E5) else Color(0xFFFFFFFF))
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFFF8C4E) else Color(0xFFE8D5C8),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFFFF5A00),
                uncheckedColor = Color(0xFFD5C4B8),
                checkmarkColor = Color.White
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Color(0xFF2D1E14) else Color(0xFF5A4A3E),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
private fun ValueBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(Color(0xFFFF5A00), CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3F2A20),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun notificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private data class OnboardingSlide(
    val title: String,
    val body: String,
    val watermark: String = "",
    val icon: ImageVector? = null,
    val decoration: SlideDecoration = SlideDecoration.SAKURA
)

private data class TopicPreferenceOption(
    val trackId: String,
    val label: String
)

private fun defaultTopicTrackIds(
    goal: EducationalGoal,
    level: LearnerLevel
): List<String> {
    return when (goal) {
        EducationalGoal.CASUAL -> when (level) {
            LearnerLevel.PRE_N5 -> listOf("foundations", "konbini_core", "signs_core")
            else -> listOf("konbini_core", "food_core", "signs_core")
        }

        EducationalGoal.EVERYDAY_USE -> when (level) {
            LearnerLevel.PRE_N5 -> listOf("foundations", "konbini_core", "signs_core")
            else -> listOf("konbini_core", "transport_core", "adulting_core")
        }

        EducationalGoal.SCHOOL_OR_WORK -> when (level) {
            LearnerLevel.INTERMEDIATE_N3,
            LearnerLevel.ADVANCED_N2 -> listOf("work", "school", "conversation")

            LearnerLevel.PRE_N5,
            LearnerLevel.BEGINNER_N5,
            LearnerLevel.BEGINNER_PLUS_N4,
            LearnerLevel.UNSURE -> listOf("school", "work", "conversation")
        }

        EducationalGoal.JLPT_OR_CLASSES -> when (level) {
            LearnerLevel.PRE_N5 -> listOf("foundations", "jlpt_n5_core")
            LearnerLevel.BEGINNER_N5,
            LearnerLevel.UNSURE -> listOf("jlpt_n5_core", "jlpt_n4_core")
            LearnerLevel.BEGINNER_PLUS_N4 -> listOf("jlpt_n4_core", "jlpt_n5_core")
            LearnerLevel.INTERMEDIATE_N3,
            LearnerLevel.ADVANCED_N2 -> listOf("jlpt_n3_core", "jlpt_n4_core")
        }
    }
}
