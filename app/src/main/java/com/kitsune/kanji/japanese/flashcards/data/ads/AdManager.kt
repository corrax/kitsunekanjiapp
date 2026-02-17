package com.kitsune.kanji.japanese.flashcards.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.kitsune.kanji.japanese.flashcards.BuildConfig
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads and shows interstitial ads before daily challenge and before starting a new level (exam pack).
 * Skips showing if user has purchased "Remove ads".
 * When [BuildConfig.ENABLE_ADS] is false, ads are disabled (e.g. until AdMob is approved).
 */
class AdManager(
    context: Context,
    private val billingPreferences: BillingPreferences
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Use test ad unit IDs for development; replace with your own in production.
    private val interstitialAdUnitIdDaily: String = "ca-app-pub-3940256099942544/1033173712"
    private val interstitialAdUnitIdLevel: String = "ca-app-pub-3940256099942544/1033173712"

    @Volatile
    private var interstitialDaily: InterstitialAd? = null

    @Volatile
    private var interstitialLevel: InterstitialAd? = null

    init {
        if (BuildConfig.ENABLE_ADS) {
            preloadInterstitials()
        }
    }

    private fun preloadInterstitials() {
        loadInterstitial(interstitialAdUnitIdDaily) { interstitialDaily = it }
        loadInterstitial(interstitialAdUnitIdLevel) { interstitialLevel = it }
    }

    private fun loadInterstitial(
        adUnitId: String,
        onLoaded: (InterstitialAd?) -> Unit
    ) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onLoaded(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    onLoaded(null)
                }
            }
        )
    }

    /**
     * Call before starting the daily challenge. If ads are removed, invokes [onProceed] immediately.
     * Otherwise shows an interstitial and invokes [onProceed] when the ad is closed or fails to show.
     */
    fun showInterstitialBeforeDailyChallenge(activity: Activity, onProceed: () -> Unit) {
        if (!BuildConfig.ENABLE_ADS) {
            scope.launch { withContext(Dispatchers.Main) { onProceed() } }
            return
        }
        scope.launch {
            val adsRemoved = withContext(Dispatchers.IO) {
                billingPreferences.isAdsRemoved()
            }
            if (adsRemoved) {
                withContext(Dispatchers.Main) { onProceed() }
                return@launch
            }
            withContext(Dispatchers.Main) {
                showInterstitial(activity, interstitialDaily, interstitialAdUnitIdDaily) {
                    interstitialDaily = null
                    preloadInterstitials()
                    onProceed()
                }
            }
        }
    }

    /**
     * Call before starting an exam pack (new level). If ads are removed, invokes [onProceed] immediately.
     * Otherwise shows an interstitial and invokes [onProceed] when the ad is closed or fails to show.
     */
    fun showInterstitialBeforeLevel(activity: Activity, onProceed: () -> Unit) {
        if (!BuildConfig.ENABLE_ADS) {
            scope.launch { withContext(Dispatchers.Main) { onProceed() } }
            return
        }
        scope.launch {
            val adsRemoved = withContext(Dispatchers.IO) {
                billingPreferences.isAdsRemoved()
            }
            if (adsRemoved) {
                withContext(Dispatchers.Main) { onProceed() }
                return@launch
            }
            withContext(Dispatchers.Main) {
                showInterstitial(activity, interstitialLevel, interstitialAdUnitIdLevel) {
                    interstitialLevel = null
                    preloadInterstitials()
                    onProceed()
                }
            }
        }
    }

    private fun showInterstitial(
        activity: Activity,
        ad: InterstitialAd?,
        adUnitId: String,
        onDone: () -> Unit
    ) {
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onDone()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    onDone()
                }

                override fun onAdShowedFullScreenContent() {
                    // Ad is showing; onDone will be called when dismissed
                }
            }
            ad.show(activity)
        } else {
            // Ad not loaded yet; proceed without blocking
            loadInterstitial(adUnitId) { }
            onDone()
        }
    }
}
