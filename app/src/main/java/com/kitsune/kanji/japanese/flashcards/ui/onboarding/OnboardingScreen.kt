package com.kitsune.kanji.japanese.flashcards.ui.onboarding

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    var selectedGoal by rememberSaveable { mutableStateOf(EducationalGoal.JLPT_OR_CLASSES.name) }

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
    val goalsPageCount = 1
    val finalPage = goalsPageCount + slides.size
    val totalPages = finalPage + 1

    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val background = onboardingBackgroundForPage(page)
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = background.imageRes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.26f,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(background.overlay))
                    )

                    when {
                        page == firstPage -> GoalSetupSlide(
                            learnerLevel = learnerLevel,
                            educationalGoal = educationalGoal,
                            onLearnerLevelChange = { selectedLevel = it.name },
                            onGoalChange = { selectedGoal = it.name },
                            modifier = Modifier.fillMaxSize()
                        )

                        page == finalPage -> PlansSlide(
                            learnerLevel = learnerLevel,
                            educationalGoal = educationalGoal,
                            onCompleteFree = { onCompleteFree(onboardingSelection) },
                            onStartTrial = { onStartTrial(onboardingSelection) },
                            onChoosePlan = { onChoosePlan(onboardingSelection) },
                            modifier = Modifier.fillMaxSize()
                        )

                        else -> IntroSlide(
                            slide = slides[page - goalsPageCount],
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (pagerState.currentPage < finalPage) {
                TextButton(
                    onClick = { onCompleteFree(onboardingSelection) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 14.dp)
                ) {
                    Text("Skip")
                }
            }

            if (pagerState.currentPage < finalPage) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(totalPages) { index ->
                            val active = index == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .width(if (active) 24.dp else 8.dp)
                                    .height(8.dp)
                                    .background(
                                        color = if (active) Color(0xFFFF5A00) else Color(0xFFFFC5A6),
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

private data class OnboardingBackground(
    val imageRes: Int,
    val overlay: List<Color>
)

private fun onboardingBackgroundForPage(page: Int): OnboardingBackground {
    return when (page) {
        0 -> OnboardingBackground(
            imageRes = R.drawable.hero_spring,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCFFF2EA))
        )

        1 -> OnboardingBackground(
            imageRes = R.drawable.hero_summer,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCEAF7FF))
        )

        2 -> OnboardingBackground(
            imageRes = R.drawable.pack_scene_temple,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCFFEFE4))
        )

        3 -> OnboardingBackground(
            imageRes = R.drawable.pack_scene_city,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCFFF1E8))
        )

        else -> OnboardingBackground(
            imageRes = R.drawable.hero_autumn,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCFFF2E6))
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
        modifier = modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What Are Your Educational Goals?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "We use this to personalize your first track, daily challenge, and difficulty ramp.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

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

@Composable
private fun IntroSlide(slide: OnboardingSlide, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFFC7AA), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
        }
    }
}

@Composable
private fun PlansSlide(
    learnerLevel: LearnerLevel,
    educationalGoal: EducationalGoal,
    onCompleteFree: () -> Unit,
    onStartTrial: () -> Unit,
    onChoosePlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your Plan Is Ready",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = summaryForSelection(educationalGoal, learnerLevel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )

        ValueBullet("Adaptive daily deck (15-18 cards) to keep session time predictable.")
        ValueBullet("Dynamic reranking and reinforcement on weak or assisted answers.")
        ValueBullet("Vocab, grammar, and sentence training linked by shared examples.")

        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onStartTrial, modifier = Modifier.fillMaxWidth()) {
            Text("Try 3-Day Free Trial")
        }
        OutlinedButton(onClick = onChoosePlan, modifier = Modifier.fillMaxWidth()) {
            Text("See Plus Plans")
        }
        TextButton(onClick = onCompleteFree, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Free")
        }
        Text(
            text = "No payment required to start free. Cancel anytime.",
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Goal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        options.forEach { (goal, title) ->
            val active = selected == goal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color(0xFFFFEFE4) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (active) Color(0xFFFFA36D) else Color(0xFFFFD8C0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelected(goal) }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3F2A20)
                )
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Current Japanese level",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        options.forEach { (level, title) ->
            val active = selected == level
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) Color(0xFFFFEFE4) else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (active) Color(0xFFFFA36D) else Color(0xFFFFD8C0),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelected(level) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF3F2A20)
                )
            }
        }
    }
}

@Composable
private fun ValueBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "•",
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

private data class OnboardingSlide(
    val title: String,
    val body: String
)
