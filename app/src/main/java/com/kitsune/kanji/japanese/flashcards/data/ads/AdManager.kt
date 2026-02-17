package com.kitsune.kanji.japanese.flashcards.data.ads

import android.app.Activity
import android.content.Context
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stub implementation of AdManager.
 * AdMob SDK has been removed for initial release.
 * This class currently just proceeds immediately without showing ads.
 */
class AdManager(
    context: Context,
    private val billingPreferences: BillingPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // No-op: SDK removed
    }

    /**
     * Stub: Immediately calls [onProceed].
     */
    fun showInterstitialBeforeDailyChallenge(activity: Activity, onProceed: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.Main) { onProceed() }
        }
    }

    /**
     * Stub: Immediately calls [onProceed].
     */
    fun showInterstitialBeforeLevel(activity: Activity, onProceed: () -> Unit) {
        scope.launch {
            withContext(Dispatchers.Main) { onProceed() }
        }
    }
}
