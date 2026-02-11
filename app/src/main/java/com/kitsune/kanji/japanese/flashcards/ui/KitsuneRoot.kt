package com.kitsune.kanji.japanese.flashcards.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kitsune.kanji.japanese.flashcards.AppContainer
import com.kitsune.kanji.japanese.flashcards.KitsuneApp
import com.kitsune.kanji.japanese.flashcards.data.local.EducationalGoal
import com.kitsune.kanji.japanese.flashcards.data.notifications.DailyChallengeNotificationScheduler
import com.kitsune.kanji.japanese.flashcards.ui.billing.PaywallScreen
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckScreen
import com.kitsune.kanji.japanese.flashcards.ui.deck.DeckViewModel
import com.kitsune.kanji.japanese.flashcards.ui.deckbrowser.DeckBrowserScreen
import com.kitsune.kanji.japanese.flashcards.ui.home.HomeScreen
import com.kitsune.kanji.japanese.flashcards.ui.home.HomeViewModel
import com.kitsune.kanji.japanese.flashcards.ui.onboarding.OnboardingScreen
import com.kitsune.kanji.japanese.flashcards.ui.onboarding.OnboardingSelection
import com.kitsune.kanji.japanese.flashcards.data.local.LearnerLevel
import com.kitsune.kanji.japanese.flashcards.ui.profile.ProfileScreen
import com.kitsune.kanji.japanese.flashcards.ui.report.DeckReportScreen
import com.kitsune.kanji.japanese.flashcards.ui.report.DeckReportViewModel
import com.kitsune.kanji.japanese.flashcards.ui.settings.SettingsScreen
import com.kitsune.kanji.japanese.flashcards.ui.settings.SettingsViewModel
import com.kitsune.kanji.japanese.flashcards.ui.theme.KitsuneTheme
import kotlinx.coroutines.launch

private const val routeOnboarding = "onboarding"
private const val routePaywall = "paywall"
private const val routeDeckBrowser = "deck_browser"
private const val routeSettings = "settings"
private const val routeProfile = "profile"
private const val routeHome = "home"
private const val routeDeck = "deck"
private const val routeDeckReport = "deck_report"

@Composable
fun KitsuneRoot() {
    KitsuneTheme(darkTheme = false) {
        val context = LocalContext.current.applicationContext as KitsuneApp
        val appContainer = context.appContainer
        val scope = rememberCoroutineScope()
        var startDestination by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            startDestination = if (appContainer.onboardingPreferences.shouldShowOnboarding()) {
                routeOnboarding
            } else {
                routeHome
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
        NavHost(
            navController = navController,
            startDestination = checkNotNull(startDestination)
        ) {
            composable(routeOnboarding) {
                OnboardingScreen(
                    onCompleteFree = { selection ->
                        scope.launch {
                            persistOnboardingSelection(appContainer, selection)
                            appContainer.onboardingPreferences.setOnboardingCompleted()
                            navController.navigate(routeHome) {
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
                            navController.navigate(routeHome) {
                                popUpTo(routeOnboarding) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onActivated = {
                        scope.launch {
                            appContainer.onboardingPreferences.setOnboardingCompleted()
                            navController.navigate(routeHome) {
                                popUpTo(routeOnboarding) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(routeHome) {
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(
                        repository = appContainer.repository,
                        deckSelectionPreferences = appContainer.deckSelectionPreferences
                    )
                )
                val lifecycleOwner = LocalLifecycleOwner.current
                val state = viewModel.uiState.collectAsStateWithLifecycle().value

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshHome()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.openDeckEvents.collect { deckRunId ->
                        navController.navigate("$routeDeck/$deckRunId")
                    }
                }

                HomeScreen(
                    state = state,
                    onStartDailyDeck = viewModel::startDailyDeck,
                    onStartExamPack = viewModel::startExamPack,
                    onSelectPack = viewModel::selectPack,
                    onDismissDailyReminder = viewModel::dismissDailyReminder,
                    onBrowseDecks = { navController.navigate(routeDeckBrowser) },
                    onOpenSettings = { navController.navigate(routeSettings) },
                    onOpenProfile = { navController.navigate(routeProfile) },
                    onOpenUpgrade = { navController.navigate("$routePaywall?trial=false") }
                )
            }

            composable(routeDeckBrowser) {
                val homeEntry = remember(it) {
                    navController.getBackStackEntry(routeHome)
                }
                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = homeEntry,
                    factory = HomeViewModel.factory(
                        repository = appContainer.repository,
                        deckSelectionPreferences = appContainer.deckSelectionPreferences
                    )
                )
                val homeState = homeViewModel.uiState.collectAsStateWithLifecycle().value
                DeckBrowserScreen(
                    selectedThemeId = homeState.selectedDeckThemeId,
                    onBack = { navController.popBackStack() },
                    onSelectTheme = { themeId, trackId ->
                        homeViewModel.selectDeck(themeId = themeId, trackId = trackId)
                        navController.popBackStack()
                    }
                )
            }

            composable(routeSettings) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(appContainer.dailySchedulePreferences)
                )
                val state = viewModel.uiState.collectAsStateWithLifecycle().value
                SettingsScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
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
                    }
                )
            }

            composable(routeProfile) {
                val homeEntry = remember(it) {
                    navController.getBackStackEntry(routeHome)
                }
                val homeViewModel: HomeViewModel = viewModel(
                    viewModelStoreOwner = homeEntry,
                    factory = HomeViewModel.factory(
                        repository = appContainer.repository,
                        deckSelectionPreferences = appContainer.deckSelectionPreferences
                    )
                )
                val homeState = homeViewModel.uiState.collectAsStateWithLifecycle().value
                ProfileScreen(
                    rankSummary = homeState.rankSummary,
                    lifetimeScore = homeState.lifetimeScore,
                    lifetimeCardsReviewed = homeState.lifetimeCardsReviewed,
                    recentRuns = homeState.recentRuns,
                    onBack = { navController.popBackStack() },
                    onOpenUpgrade = { navController.navigate("$routePaywall?trial=false") },
                    onOpenRunReport = { runId ->
                        navController.navigate("$routeDeckReport/$runId")
                    }
                )
            }

            composable(
                route = "$routeDeck/{deckRunId}",
                arguments = listOf(navArgument("deckRunId") { type = NavType.StringType })
            ) {
                val deckRunId = checkNotNull(it.arguments?.getString("deckRunId"))
                val viewModel: DeckViewModel = viewModel(
                    factory = DeckViewModel.factory(
                        repository = appContainer.repository,
                        handwritingScorer = appContainer.handwritingScorer
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
                    onUsePowerUp = viewModel::usePowerUp,
                    onDeckSubmitted = { runId ->
                        navController.navigate("$routeDeckReport/$runId")
                    },
                    onSubmitDeck = viewModel::submitDeck
                )
            }

            composable(
                route = "$routeDeckReport/{deckRunId}",
                arguments = listOf(navArgument("deckRunId") { type = NavType.StringType })
            ) {
                val deckRunId = checkNotNull(it.arguments?.getString("deckRunId"))
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
                    onBack = { navController.popBackStack() },
                    onBackToHome = {
                        navController.navigate(routeHome) {
                            popUpTo(routeHome) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
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
}

private fun preferredDeckForSelection(selection: OnboardingSelection): Pair<String, String> {
    return when (selection.educationalGoal) {
        EducationalGoal.CASUAL,
        EducationalGoal.EVERYDAY_USE -> "conversation" to "conversation"

        EducationalGoal.SCHOOL_OR_WORK -> {
            when (selection.learnerLevel) {
                LearnerLevel.INTERMEDIATE_N3,
                LearnerLevel.ADVANCED_N2 -> "work" to "work"

                LearnerLevel.BEGINNER_N5,
                LearnerLevel.BEGINNER_PLUS_N4,
                LearnerLevel.UNSURE -> "school" to "school"
            }
        }

        EducationalGoal.JLPT_OR_CLASSES -> "jlpt_n5" to "jlpt_n5_core"
    }
}
