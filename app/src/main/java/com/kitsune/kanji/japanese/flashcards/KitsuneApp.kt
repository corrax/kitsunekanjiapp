package com.kitsune.kanji.japanese.flashcards

import android.app.Application
import androidx.room.Room
import com.google.android.gms.ads.MobileAds
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingManager
import com.kitsune.kanji.japanese.flashcards.BuildConfig
import com.kitsune.kanji.japanese.flashcards.data.ads.AdManager
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DailySchedulePreferences
import com.kitsune.kanji.japanese.flashcards.data.local.DeckSelectionPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.KitsuneDatabase
import com.kitsune.kanji.japanese.flashcards.data.local.OnboardingPreferences
import com.kitsune.kanji.japanese.flashcards.data.local.PowerUpPreferences
import com.kitsune.kanji.japanese.flashcards.data.notifications.DailyChallengeNotificationScheduler
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepository
import com.kitsune.kanji.japanese.flashcards.data.repository.KitsuneRepositoryImpl
import com.kitsune.kanji.japanese.flashcards.domain.ink.HandwritingScorer
import com.kitsune.kanji.japanese.flashcards.domain.ink.MlKitHandwritingScorer

class KitsuneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ENABLE_ADS) {
            MobileAds.initialize(this) {}
        }
        DailyChallengeNotificationScheduler.schedule(this)
    }

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val database: KitsuneDatabase = Room.databaseBuilder(
        application,
        KitsuneDatabase::class.java,
        "kitsune.db"
    ).fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val powerUpPreferences: PowerUpPreferences = PowerUpPreferences(application)
    val dailySchedulePreferences: DailySchedulePreferences = DailySchedulePreferences(application)
    val deckSelectionPreferences: DeckSelectionPreferences = DeckSelectionPreferences(application)
    val onboardingPreferences: OnboardingPreferences = OnboardingPreferences(application)

    val repository: KitsuneRepository = KitsuneRepositoryImpl(
        database = database,
        dao = database.kitsuneDao(),
        powerUpPreferences = powerUpPreferences,
        dailySchedulePreferences = dailySchedulePreferences,
        onboardingPreferences = onboardingPreferences
    )

    val handwritingScorer: HandwritingScorer = MlKitHandwritingScorer()
    val billingPreferences: BillingPreferences = BillingPreferences(application)
    val billingManager: BillingManager = BillingManager(
        context = application,
        billingPreferences = billingPreferences
    )

    val adManager: AdManager = AdManager(
        context = application,
        billingPreferences = billingPreferences
    )
}
