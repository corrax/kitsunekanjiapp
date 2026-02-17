package com.kitsune.kanji.japanese.flashcards.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.local.EducationalGoal
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import kotlinx.coroutines.launch

data class OnboardingSelection(
    val learnerLevel: LearnerLevel,
    val educationalGoal: EducationalGoal
)

@Composable
fun OnboardingScreen(
    onCompleteFree: (OnboardingSelection) -> Unit,
    onStartTrial: (OnboardingSelection) -> Unit,
    onChoosePlan: (OnboardingSelection) -> Unit
) {
    var selectedLevel by rememberSaveable { mutableStateOf(LearnerLevel.BEGINNER_N5.name) }
    var selectedGoal by rememberSaveable { mutableStateOf(EducationalGoal.CASUAL.name) }

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
    val onboardingSelection = OnboardingSelection(
        learnerLevel = learnerLevel,
        educationalGoal = educationalGoal
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
                val theme = onboardingThemeForPage(page)
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
                                    listOf(Color(0x88FFFFFF), Color(0xD9FFFFFF))
                                )
                            )
                    )

                    val slideModifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .padding(bottom = if (page < finalPage) 132.dp else 12.dp)

                    when {
                        page == firstPage -> GoalSetupSlide(
                            learnerLevel = learnerLevel,
                            educationalGoal = educationalGoal,
                            onLearnerLevelChange = { selectedLevel = it.name },
                            onGoalChange = { selectedGoal = it.name },
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
                            onCompleteFree = { onCompleteFree(onboardingSelection) },
                            modifier = slideModifier
                        )
                    }
                }
            }

            if (pagerState.currentPage < finalPage) {
                /*
                 * Removed Skip button as per request
                 */
            }

            if (pagerState.currentPage < finalPage) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xEFFFFFFF))
                        .border(1.dp, Color(0xFFFFD0B2), RoundedCornerShape(16.dp))
                        .padding(12.dp),
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
                                    .width(if (active) 22.dp else 8.dp)
                                    .height(8.dp)
                                    .background(
                                        color = if (active) Color(0xFFFF5A00) else Color(0xFFFFD6BF),
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val label = if (pagerState.currentPage == firstPage) "Continue" else "Next"
                        Text(label)
                    }
                }
            }
        }
    }
}

private data class OnboardingTheme(
    val imageRes: Int,
    val baseColor: Color
)

private fun onboardingThemeForPage(page: Int): OnboardingTheme {
    return when (page) {
        0 -> OnboardingTheme(
            imageRes = R.drawable.hero_spring,
            baseColor = Color(0xFFFFF2E8)
        )

        1 -> OnboardingTheme(
            imageRes = R.drawable.hero_summer,
            baseColor = Color(0xFFE8F7FF)
        )

        2 -> OnboardingTheme(
            imageRes = R.drawable.pack_scene_temple,
            baseColor = Color(0xFFF2E8DD)
        )

        3 -> OnboardingTheme(
            imageRes = R.drawable.pack_scene_city,
            baseColor = Color(0xFFEAF1F5)
        )

        4 -> OnboardingTheme(
            imageRes = R.drawable.hero_autumn,
            baseColor = Color(0xFFFFEFE0)
        )

        else -> OnboardingTheme(
            imageRes = R.drawable.pack_scene_food,
            baseColor = Color(0xFFFFE8D2)
        )
    }
}

@Composable
private fun GoalSetupSlide(
    learnerLevel: LearnerLevel,
    educationalGoal: EducationalGoal,
    onLearnerLevelChange: (LearnerLevel) -> Unit,
    onGoalChange: (EducationalGoal) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Choose Your Learning Focus",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D1E14)
        )
        Text(
            text = "We use this to personalize your first track, daily challenge, and difficulty ramp.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF604A3D)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFD1B3), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EducationalGoalSelector(
                    selected = educationalGoal,
                    onSelected = onGoalChange
                )

                LearnerLevelSelector(
                    selected = learnerLevel,
                    onSelected = onLearnerLevelChange
                )
            }
        }
    }
}

@Composable
private fun IntroSlide(slide: OnboardingSlide, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFCEAF), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D1E14)
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
        "Stay on your daily streak"
    }

    val body = if (!isPermissionSupported) {
        "Your Android version does not require notification permission. We will still remind you at your chosen daily time."
    } else if (isPermissionGranted) {
        "Great. Kitsune can remind you about daily practice and quizzes so streaks are easier to keep."
    } else {
        "Enable notifications so Kitsune can remind you about your daily practice and quiz goals. You can change this anytime in Settings."
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFCEAF), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isPermissionGranted) "Reminders Enabled" else "Enable Daily Reminders")
                    }
                    if (!isPermissionGranted) {
                        Text(
                            text = "You can also continue now and enable it later in Android Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
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
    onCompleteFree: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "You're All Set",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D1E14)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = summaryForSelection(educationalGoal, learnerLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEFBF7)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFD1B3), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ValueBullet("Adaptive daily deck (15-18 cards) to keep session time predictable.")
                ValueBullet("Every card supports optional assist; assisted answers are capped and recycled in daily reinforcement.")
                ValueBullet("Vocab, grammar, and sentence training linked by shared examples.")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4EA)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFD8C0), RoundedCornerShape(16.dp))
        ) {
            Text(
                text = "Good luck and have fun learning Japanese with Kitsune. Keep your daily streak going and enjoy the journey.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5A3A2A)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onCompleteFree, modifier = Modifier.fillMaxWidth()) {
            Text("Start Learning")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You can upgrade anytime later from the app menu.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private fun summaryForSelection(goal: EducationalGoal, level: LearnerLevel): String {
    val goalSummary = when (goal) {
        EducationalGoal.CASUAL -> "Conversation-first"
        EducationalGoal.EVERYDAY_USE -> "Everyday Japanese"
        EducationalGoal.SCHOOL_OR_WORK -> "School/work scenarios"
        EducationalGoal.JLPT_OR_CLASSES -> "JLPT-focused progression"
    }
    val levelSummary = when (level) {
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
                title = "Speak More Naturally in 2-3 Weeks",
                body = "Conversation-first decks focus on practical words and phrases you can use right away."
            ),
            OnboardingSlide(
                title = "Improve Listening & Reply Speed",
                body = "Short sentence checks train fast comprehension so everyday dialogue feels easier."
            ),
            OnboardingSlide(
                title = "Stay Consistent Without Burnout",
                body = "Daily sessions stay compact while adaptive reranking keeps challenge high and progress steady."
            )
        )

        EducationalGoal.EVERYDAY_USE -> listOf(
            OnboardingSlide(
                title = "Increase Everyday Vocabulary in 2-3 Weeks",
                body = "Core daily-life words are introduced first, then reinforced through realistic sentence use."
            ),
            OnboardingSlide(
                title = "Handle Real Situations With Confidence",
                body = "School, work, and conversation cards rotate based on your score to target practical gaps."
            ),
            OnboardingSlide(
                title = "Build Grammar You Actually Need",
                body = "Lower levels use guided choices, then graduate to cloze writing as you improve."
            )
        )

        EducationalGoal.SCHOOL_OR_WORK -> listOf(
            OnboardingSlide(
                title = "Master School & Work Vocabulary Faster",
                body = "Focused tracks cover classroom, office, meetings, requests, and deadlines."
            ),
            OnboardingSlide(
                title = "Improve Grammar & Professional Clarity",
                body = "Polite forms and pattern drills are reinforced with context-based sentence cards."
            ),
            OnboardingSlide(
                title = "Perform Under Real Constraints",
                body = "Adaptive scoring surfaces weak points early so high-value terms get repeated sooner."
            )
        )

        EducationalGoal.JLPT_OR_CLASSES -> listOf(
            OnboardingSlide(
                title = "Increase JLPT Vocabulary in 2-3 Weeks",
                body = "Progressive decks follow JLPT-aligned difficulty while still adapting to your true performance."
            ),
            OnboardingSlide(
                title = "Improve Grammar & Reading Accuracy",
                body = "Pattern cards and sentence checks target mistakes that typically reduce test scores."
            ),
            OnboardingSlide(
                title = "Study Smarter Across Kanji + Grammar",
                body = "Shared sentence examples train vocab, grammar, and comprehension together to reduce overload."
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Goal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D1E14)
        )
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
        LearnerLevel.BEGINNER_N5 to "Beginner (~JLPT N5)",
        LearnerLevel.BEGINNER_PLUS_N4 to "Beginner+ (~JLPT N4)",
        LearnerLevel.INTERMEDIATE_N3 to "Intermediate (~JLPT N3)",
        LearnerLevel.ADVANCED_N2 to "Advanced (~JLPT N2+)",
        LearnerLevel.UNSURE to "Not sure"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Current Japanese level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2D1E14)
        )
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
                color = if (selected) Color(0xFFFFAD79) else Color(0xFFFFD8C0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFFFF5A00),
                unselectedColor = Color(0xFFFFD8C0)
            )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF3F2A20),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

@Composable
private fun ValueBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "-",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFFF5A00),
            fontWeight = FontWeight.Bold
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
    val body: String
)
