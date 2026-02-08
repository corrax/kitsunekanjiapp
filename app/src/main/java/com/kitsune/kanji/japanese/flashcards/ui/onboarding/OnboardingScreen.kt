package com.kitsune.kanji.japanese.flashcards.ui.onboarding

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
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onCompleteFree: (LearnerLevel) -> Unit,
    onStartTrial: (LearnerLevel) -> Unit,
    onChoosePlan: (LearnerLevel) -> Unit
) {
    var selectedLevel by rememberSaveable { mutableStateOf(LearnerLevel.BEGINNER_N5.name) }
    val learnerLevel = runCatching { LearnerLevel.valueOf(selectedLevel) }
        .getOrDefault(LearnerLevel.BEGINNER_N5)
    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Swipe To Learn Kanji",
                body = "Practice like cards in your hand. Swipe right for next, left to revisit, and keep momentum every day."
            ),
            OnboardingSlide(
                title = "Write, Score, Improve",
                body = "Each card uses handwriting checks, then gives fast feedback on shape and stroke quality."
            ),
            OnboardingSlide(
                title = "Daily Deck + Pack Progression",
                body = "Claim your 15-card daily deck, then clear level packs. Pass exam packs to unlock the next set."
            )
        )
    }

    val totalPages = slides.size + 1
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
                    if (page < slides.size) {
                        IntroSlide(
                            slide = slides[page],
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PlansSlide(
                            learnerLevel = learnerLevel,
                            onLearnerLevelChange = { selectedLevel = it.name },
                            onCompleteFree = { onCompleteFree(learnerLevel) },
                            onStartTrial = { onStartTrial(learnerLevel) },
                            onChoosePlan = { onChoosePlan(learnerLevel) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (pagerState.currentPage < slides.size) {
                TextButton(
                    onClick = { onCompleteFree(learnerLevel) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 14.dp)
                ) {
                    Text("Skip")
                }

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
                        Text("Next")
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
            imageRes = R.drawable.pack_scene_temple,
            overlay = listOf(Color(0xE6FFFFFF), Color(0xCCFFEFE4))
        )

        2 -> OnboardingBackground(
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
    onLearnerLevelChange: (LearnerLevel) -> Unit,
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
            text = "Choose Your Learning Path",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Start free, or unlock advanced practice and faster progression.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        LearnerLevelSelector(
            selected = learnerLevel,
            onSelected = onLearnerLevelChange
        )

        PlanCard(
            name = "Free",
            price = "$0",
            details = "1 track (JLPT N5), daily deck (15 cards), basic score breakdown."
        )
        PlanCard(
            name = "Kitsune Plus Monthly",
            price = "$4.99/mo",
            details = "All tracks, unlimited retries, advanced handwriting feedback, weekly challenge decks."
        )
        PlanCard(
            name = "Kitsune Plus Annual",
            price = "$39.99/yr",
            details = "Same as Plus Monthly, 33% lower cost, seasonal event packs included."
        )

        Spacer(modifier = Modifier.height(4.dp))
        Button(onClick = onStartTrial, modifier = Modifier.fillMaxWidth()) {
            Text("Start 7-Day Free Trial")
        }
        OutlinedButton(onClick = onChoosePlan, modifier = Modifier.fillMaxWidth()) {
            Text("See All Plan Details")
        }
        TextButton(onClick = onCompleteFree, modifier = Modifier.fillMaxWidth()) {
            Text("Continue with Free")
        }
        Text(
            text = "No payment needed for the free option.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
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
private fun PlanCard(name: String, price: String, details: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFCEB2), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(price, style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF5A00))
            }
            Text(details, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class OnboardingSlide(
    val title: String,
    val body: String
)
