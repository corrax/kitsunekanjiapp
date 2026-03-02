package com.kitsune.kanji.japanese.flashcards.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kitsune.kanji.japanese.flashcards.AppContainer
import com.kitsune.kanji.japanese.flashcards.KitsuneApp
import com.kitsune.kanji.japanese.flashcards.data.local.EducationalGoal
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.data.notifications.DailyChallengeNotificationScheduler
import com.kitsune.kanji.japanese.flashcards.ui.billing.PaywallScreen
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckScreen
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckViewModel
import com.kitsune.kanji.japanese.flashcards.ui.explore.ExploreScreen
import com.kitsune.kanji.japanese.flashcards.ui.explore.ExploreViewModel
import com.kitsune.kanji.japanese.flashcards.ui.learn.LearnScreen
import com.kitsune.kanji.japanese.flashcards.ui.learn.LearnViewModel
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.deckThemeCatalog
import com.kitsune.kanji.japanese.flashcards.ui.onboarding.OnboardingScreen
import com.kitsune.kanji.japanese.flashcards.ui.onboarding.OnboardingSelection
import com.kitsune.kanji.japanese.flashcards.ui.profile.ProfileTabScreen
import com.kitsune.kanji.japanese.flashcards.ui.profile.ProfileTabViewModel
import com.kitsune.kanji.japanese.flashcards.ui.report.DeckReportScreen
import com.kitsune.kanji.japanese.flashcards.ui.report.DeckReportViewModel
import com.kitsune.kanji.japanese.flashcards.ui.settings.SettingsTabScreen
import com.kitsune.kanji.japanese.flashcards.ui.settings.SettingsTabViewModel
import com.kitsune.kanji.japanese.flashcards.ui.theme.KitsuneTheme
import kotlinx.coroutines.launch

private const val routeOnboarding = "onboarding"
private const val routePaywall = "paywall"
private const val routeLearn = "learn"
private const val routeExplore = "explore"
private const val routeProfile = "profile"
private const val routeSettings = "settings"
private const val routeDeck = "deck"
private const val routeDeckReport = "deck_report"

private data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun KitsuneRoot() {
    KitsuneTheme(darkTheme = false) {
        val context = LocalContext.current.applicationContext as KitsuneApp
        val appContainer = context.appContainer
        val scope = rememberCoroutineScope()
        var startDestination by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            runCatching {
                appContainer.repository.initialize()
            }.onFailure {
                // Continue to route resolution; downstream screens already handle load errors.
            }
            startDestination = if (appContainer.onboardingPreferences.shouldShowOnboarding()) {
                routeOnboarding
            } else {
                routeLearn
            }
        }

        if (startDestination == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@KitsuneTheme
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val tabRoutes = setOf(routeLearn, routeExplore, routeProfile, routeSettings)
        val showBottomBar = currentRoute in tabRoutes

        val tabs = remember {
            listOf(
                TabItem(routeLearn, "Learn", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
                TabItem(routeExplore, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
                TabItem(routeProfile, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
                TabItem(routeSettings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
            )
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = checkNotNull(startDestination),
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(routeOnboarding) {
                    OnboardingScreen(
                        onCompleteFree = { selection ->
                            scope.launch {
                                persistOnboardingSelection(appContainer, selection)
                                appContainer.onboardingPreferences.setOnboardingCompleted()
                                navController.navigate(routeLearn) {
                                    popUpTo(routeOnboarding) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onStartTrial = { selection ->
                            scope.launch {
                                persistOnboardingSelection(appContainer, selection)
                                navController.navigate("$routePaywall?trial=true")
                            }
                        },
                        onChoosePlan = { selection ->
                            scope.launch {
                                persistOnboardingSelection(appContainer, selection)
                                navController.navigate("$routePaywall?trial=false")
                            }
                        }
                    )
                }

                composable(
                    route = "$routePaywall?trial={trial}",
                    arguments = listOf(
                        navArgument("trial") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) {
                    val preferTrial = it.arguments?.getBoolean("trial") ?: false
                    PaywallScreen(
                        billingManager = appContainer.billingManager,
                        preferTrial = preferTrial,
                        onContinueFree = {
                            scope.launch {
                                appContainer.onboardingPreferences.setOnboardingCompleted()
                                navController.navigate(routeLearn) {
                                    popUpTo(routeOnboarding) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onActivated = {
                            scope.launch {
                                appContainer.onboardingPreferences.setOnboardingCompleted()
                                navController.navigate(routeLearn) {
                                    popUpTo(routeOnboarding) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                // --- Tab: Learn ---
                composable(routeLearn) {
                    val viewModel: LearnViewModel = viewModel(
                        factory = LearnViewModel.factory(
                            repository = appContainer.repository,
                            deckSelectionPreferences = appContainer.deckSelectionPreferences,
                            billingPreferences = appContainer.billingPreferences
                        )
                    )
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    val activity = LocalContext.current as? Activity

                    LaunchedEffect(Unit) {
                        viewModel.openDeckEvents.collect { deckRunId ->
                            navController.navigate("$routeDeck/$deckRunId")
                        }
                    }

                    LearnScreen(
                        state = state,
                        onStartDailyDeck = viewModel::startDailyDeck,
                        onStartExamPack = { packId ->
                            activity?.let { act ->
                                appContainer.adManager.showInterstitialBeforeLevel(act) {
                                    viewModel.startExamPack(packId)
                                }
                            } ?: viewModel.startExamPack(packId)
                        },
                        onThemeSelected = viewModel::onThemeSelected,
                        onRefresh = viewModel::refreshHome
                    )
                }

                // --- Tab: Explore ---
                composable(routeExplore) {
                    val viewModel: ExploreViewModel = viewModel(
                        factory = ExploreViewModel.factory(
                            repository = appContainer.repository,
                            deckSelectionPreferences = appContainer.deckSelectionPreferences
                        )
                    )
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    val activity = LocalContext.current as? Activity

                    LaunchedEffect(Unit) {
                        viewModel.openDeckEvents.collect { deckRunId ->
                            navController.navigate("$routeDeck/$deckRunId")
                        }
                    }

                    ExploreScreen(
                        state = state,
                        onTopicSelected = viewModel::onTopicSelected,
                        onTopicSelectionToggled = viewModel::onTopicSelectionToggled,
                        onStartExamPack = { packId ->
                            activity?.let { act ->
                                appContainer.adManager.showInterstitialBeforeLevel(act) {
                                    viewModel.startExamPack(packId)
                                }
                            } ?: viewModel.startExamPack(packId)
                        }
                    )
                }

                // --- Tab: Profile ---
                composable(routeProfile) {
                    val viewModel: ProfileTabViewModel = viewModel(
                        factory = ProfileTabViewModel.factory(
                            repository = appContainer.repository,
                            deckSelectionPreferences = appContainer.deckSelectionPreferences
                        )
                    )
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    ProfileTabScreen(
                        state = state,
                        onOpenRunReport = { runId ->
                            navController.navigate("$routeDeckReport/$runId")
                        },
                        onOpenUpgrade = { navController.navigate("$routePaywall?trial=false") }
                    )
                }

                // --- Tab: Settings ---
                composable(routeSettings) {
                    val viewModel: SettingsTabViewModel = viewModel(
                        factory = SettingsTabViewModel.factory(
                            dailySchedulePreferences = appContainer.dailySchedulePreferences,
                            onboardingPreferences = appContainer.onboardingPreferences,
                            deckSelectionPreferences = appContainer.deckSelectionPreferences
                        )
                    )
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    SettingsTabScreen(
                        state = state,
                        onLearnerLevelChange = viewModel::updateLearnerLevel,
                        onResetTimeChange = viewModel::updateResetTime,
                        onReminderTimeChange = viewModel::updateReminderTime,
                        onSave = {
                            viewModel.save {
                                DailyChallengeNotificationScheduler.schedule(context)
                            }
                        },
                        onResetDefaults = {
                            viewModel.resetToLocaleDefaults {
                                DailyChallengeNotificationScheduler.schedule(context)
                            }
                        },
                        onOpenUpgrade = { navController.navigate("$routePaywall?trial=false") }
                    )
                }

                // --- Pushed: Deck (from Explore exam packs) ---
                composable(
                    route = "$routeDeck/{deckRunId}",
                    arguments = listOf(navArgument("deckRunId") { type = NavType.StringType })
                ) {
                    val deckRunId = checkNotNull(it.arguments?.getString("deckRunId"))
                    val viewModel: DeckViewModel = viewModel(
                        factory = DeckViewModel.factory(
                            repository = appContainer.repository,
                            handwritingScorer = appContainer.handwritingScorer,
                            onboardingPreferences = appContainer.onboardingPreferences
                        )
                    )
                    LaunchedEffect(deckRunId) {
                        viewModel.initialize(deckRunId)
                    }
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    DeckScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onPrevious = viewModel::goPrevious,
                        onNext = viewModel::goNext,
                        onSubmitCard = { sample, typedAnswer, selectedChoice, assists ->
                            viewModel.submitCurrentCard(
                                sample = sample,
                                typedAnswer = typedAnswer,
                                selectedChoice = selectedChoice,
                                requestedAssists = assists
                            )
                        },
                        onDeckSubmitted = { runId ->
                            navController.navigate("$routeDeckReport/$runId?fromSubmit=true") {
                                popUpTo(routeLearn) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onSubmitDeck = viewModel::submitDeck,
                        onDismissGestureOverlay = viewModel::dismissGestureHelp,
                        onAssistEnabled = viewModel::onAssistEnabledForCurrentCard
                    )
                }

                // --- Pushed: Deck Report ---
                composable(
                    route = "$routeDeckReport/{deckRunId}?fromSubmit={fromSubmit}",
                    arguments = listOf(
                        navArgument("deckRunId") { type = NavType.StringType },
                        navArgument("fromSubmit") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) {
                    val deckRunId = checkNotNull(it.arguments?.getString("deckRunId"))
                    val fromSubmit = it.arguments?.getBoolean("fromSubmit") ?: false
                    val viewModel: DeckReportViewModel = viewModel(
                        factory = DeckReportViewModel.factory(
                            repository = appContainer.repository
                        )
                    )
                    LaunchedEffect(deckRunId) {
                        viewModel.initialize(deckRunId)
                    }
                    val state = viewModel.uiState.collectAsStateWithLifecycle().value
                    DeckReportScreen(
                        state = state,
                        onBack = {
                            if (fromSubmit) {
                                navController.navigate(routeLearn) {
                                    popUpTo(routeLearn) { inclusive = false }
                                    launchSingleTop = true
                                }
                            } else {
                                navController.popBackStack()
                            }
                        },
                        onBackToHome = {
                            navController.navigate(routeLearn) {
                                popUpTo(routeLearn) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

private suspend fun persistOnboardingSelection(
    appContainer: AppContainer,
    selection: OnboardingSelection
) {
    appContainer.onboardingPreferences.setLearnerLevel(selection.learnerLevel)
    appContainer.onboardingPreferences.setEducationalGoal(selection.educationalGoal)
    val (themeId, trackId) = preferredDeckForSelection(selection)
    appContainer.deckSelectionPreferences.setSelectedThemeId(themeId)
    appContainer.deckSelectionPreferences.setSelectedTrackId(trackId)
    val topicTrackIds = selection.topicTrackIds.ifEmpty { setOf(trackId) }
    appContainer.deckSelectionPreferences.setSelectedTopicTrackIds(topicTrackIds)
}

private fun preferredDeckForSelection(selection: OnboardingSelection): Pair<String, String> {
    val selectedTopicTrackId = selection.topicTrackIds.firstOrNull()
    if (!selectedTopicTrackId.isNullOrBlank()) {
        val matchingTheme = deckThemeCatalog.firstOrNull { it.contentTrackId == selectedTopicTrackId }
        if (matchingTheme != null) {
            return matchingTheme.id to selectedTopicTrackId
        }
    }
    if (selection.learnerLevel == LearnerLevel.PRE_N5) {
        return "foundations" to "foundations"
    }
    return when (selection.educationalGoal) {
        EducationalGoal.CASUAL,
        EducationalGoal.EVERYDAY_USE -> "conversation" to "conversation"

        EducationalGoal.SCHOOL_OR_WORK -> {
            when (selection.learnerLevel) {
                LearnerLevel.INTERMEDIATE_N3,
                LearnerLevel.ADVANCED_N2 -> "work" to "work"

                LearnerLevel.PRE_N5,
                LearnerLevel.BEGINNER_N5,
                LearnerLevel.BEGINNER_PLUS_N4,
                LearnerLevel.UNSURE -> "school" to "school"
            }
        }

        EducationalGoal.JLPT_OR_CLASSES -> when (selection.learnerLevel) {
            LearnerLevel.PRE_N5,
            LearnerLevel.BEGINNER_N5,
            LearnerLevel.UNSURE -> "jlpt_n5" to "jlpt_n5_core"

            LearnerLevel.BEGINNER_PLUS_N4 -> "jlpt_n4" to "jlpt_n4_core"

            LearnerLevel.INTERMEDIATE_N3,
            LearnerLevel.ADVANCED_N2 -> "jlpt_n3" to "jlpt_n3_core"
        }
    }
}
