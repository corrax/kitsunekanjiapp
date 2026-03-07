package com.kitsune.kanji.japanese.flashcards.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.PendingPurchasesParams
import com.kitsune.kanji.japanese.flashcards.data.local.BillingEntitlement
import com.kitsune.kanji.japanese.flashcards.data.local.BillingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillingProduct(
    val productId: String,
    val basePlanId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriodLabel: String?,
    val hasFreeTrial: Boolean,
    val offerToken: String,
    /** Price in micros (e.g. from PricingPhase.priceAmountMicros) for discount calculation; 0 for non-subs. */
    val priceAmountMicros: Long = 0L,
    /** ISO 4217 currency code (e.g. USD, CAD, SGD) for unambiguous display. */
    val priceCurrencyCode: String = ""
)

data class BillingUiState(
    val isConnecting: Boolean = true,
    val products: List<BillingProduct> = emptyList(),
    val removeAdsProduct: BillingProduct? = null,
    val isAdsRemoved: Boolean = false,
    val entitlement: BillingEntitlement = BillingEntitlement(
        isPlusEntitled = false,
        activePlanId = null,
        lastPurchaseToken = null,
        updatedAtEpochMillis = 0L
    ),
    val message: String? = null
)

class BillingManager(
    context: Context,
    private val billingPreferences: BillingPreferences
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cachedProductDetails = mutableMapOf<String, ProductDetails>()

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val cachedRemoveAdsDetails = mutableMapOf<String, ProductDetails>()

    init {
        scope.launch {
            billingPreferences.entitlementFlow.collect { entitlement ->
                _uiState.value = _uiState.value.copy(entitlement = entitlement)
            }
        }
        scope.launch {
            billingPreferences.adsRemovedFlow.collect { adsRemoved ->
                _uiState.value = _uiState.value.copy(isAdsRemoved = adsRemoved)
            }
        }
        connect()
    }

    fun connect() {
        if (billingClient.isReady) {
            refreshProducts()
            refreshPurchases()
            return
        }

        _uiState.value = _uiState.value.copy(isConnecting = true, message = null)
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingResponseCode.OK) {
                        _uiState.value = _uiState.value.copy(isConnecting = false, message = null)
                        refreshProducts()
                        refreshPurchases()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isConnecting = false,
                            message = "Billing unavailable: ${result.debugMessage}"
                        )
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _uiState.value = _uiState.value.copy(isConnecting = true)
                }
            }
        )
    }

    fun refreshProducts() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        val productList = PLAN_PRODUCT_IDS.map { productId ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetails ->
            if (result.responseCode != BillingResponseCode.OK) {
                _uiState.value = _uiState.value.copy(
                    message = "Could not load plans: ${result.debugMessage}"
                )
                return@queryProductDetailsAsync
            }

            cachedProductDetails.clear()
            val mapped = mutableListOf<BillingProduct>()
            for (details in productDetails) {
                val offers = details.subscriptionOfferDetails.orEmpty()
                if (offers.isEmpty()) continue

                cachedProductDetails[details.productId] = details

                // Group by base plan (plus-weekly, plus-monthly, plus-yearly); prefer trial offer per plan.
                val byBasePlan = offers.groupBy { it.basePlanId }
                for ((basePlanId, planOffers) in byBasePlan) {
                    val trialOffer = planOffers.firstOrNull { offer ->
                        offer.pricingPhases.pricingPhaseList.any { phase ->
                            phase.priceAmountMicros == 0L
                        }
                    }
                    val selectedOffer = trialOffer ?: planOffers.firstOrNull() ?: continue
                    val pricingPhase = selectedOffer.pricingPhases.pricingPhaseList.lastOrNull() ?: continue

                    mapped += BillingProduct(
                        productId = details.productId,
                        basePlanId = basePlanId,
                        title = details.name,
                        description = details.description,
                        formattedPrice = pricingPhase.formattedPrice,
                        billingPeriodLabel = pricingPhase.billingPeriod,
                        hasFreeTrial = trialOffer != null,
                        offerToken = selectedOffer.offerToken,
                        priceAmountMicros = pricingPhase.priceAmountMicros,
                        priceCurrencyCode = pricingPhase.priceCurrencyCode
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                products = mapped.sortedBy { planSortKey(it.basePlanId) },
                message = null
            )
            queryRemoveAdsProduct()
        }
    }

    private fun queryRemoveAdsProduct() {
        if (!billingClient.isReady) return
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_REMOVE_ADS)
                .setProductType(ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient.queryProductDetailsAsync(params) { result, productDetails ->
            if (result.responseCode != BillingResponseCode.OK || productDetails.isEmpty()) {
                _uiState.value = _uiState.value.copy(removeAdsProduct = null)
                return@queryProductDetailsAsync
            }
            val details = productDetails.first()
            cachedRemoveAdsDetails[details.productId] = details
            val oneTimeOffer = details.oneTimePurchaseOfferDetails
            _uiState.value = _uiState.value.copy(
                removeAdsProduct = BillingProduct(
                    productId = details.productId,
                    basePlanId = "",
                    title = details.name,
                    description = details.description,
                    formattedPrice = oneTimeOffer?.formattedPrice ?: "",
                    billingPeriodLabel = null,
                    hasFreeTrial = false,
                    offerToken = "",
                    priceAmountMicros = 0L,
                    priceCurrencyCode = oneTimeOffer?.priceCurrencyCode ?: ""
                )
            )
        }
    }

    fun launchRemoveAdsPurchase(activity: Activity) {
        if (!billingClient.isReady) {
            connect()
            _uiState.value = _uiState.value.copy(message = "Connecting to billing, please try again.")
            return
        }
        val details = cachedRemoveAdsDetails[PRODUCT_REMOVE_ADS]
        if (details == null) {
            _uiState.value = _uiState.value.copy(message = "Remove ads is not available right now.")
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(message = "Could not start purchase: ${result.debugMessage}")
        }
    }

    fun launchPurchase(activity: Activity, productId: String, offerToken: String) {
        if (!billingClient.isReady) {
            connect()
            _uiState.value = _uiState.value.copy(message = "Connecting to billing, please try again.")
            return
        }

        val details = cachedProductDetails[productId]
        if (details == null) {
            _uiState.value = _uiState.value.copy(message = "Plan is not available right now.")
            return
        }
        if (offerToken.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "Offer token is missing for this plan.")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(message = "Could not start purchase: ${result.debugMessage}")
        }
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        ) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                _uiState.value = _uiState.value.copy(message = "Purchase restore failed: ${result.debugMessage}")
                return@queryPurchasesAsync
            }
            scope.launch {
                val plusPurchases = purchases.filter { it.products.contains(PRODUCT_PLUS) }
                if (plusPurchases.isEmpty()) {
                    billingPreferences.clearEntitlement()
                } else {
                    plusPurchases.forEach { processPurchase(it) }
                }
            }
        }
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) {
                scope.launch {
                    if (purchases.any { it.products.contains(PRODUCT_REMOVE_ADS) }) {
                        billingPreferences.setAdsRemoved()
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            scope.launch {
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_REMOVE_ADS)) {
                        processRemoveAdsPurchase(purchase)
                    } else {
                        processPurchase(purchase)
                    }
                }
            }
            return
        }

        if (result.responseCode == BillingResponseCode.USER_CANCELED) {
            _uiState.value = _uiState.value.copy(message = "Purchase canceled.")
            return
        }

        _uiState.value = _uiState.value.copy(message = "Purchase failed: ${result.debugMessage}")
    }

    private suspend fun processPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return
        }

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) {}
        }

        val activeProduct = purchase.products.firstOrNull() ?: PRODUCT_PLUS
        billingPreferences.setPlusEntitlement(
            planId = activeProduct,
            purchaseToken = purchase.purchaseToken
        )
        _uiState.value = _uiState.value.copy(message = "Purchase successful. Plus is now active.")
    }

    private suspend fun processRemoveAdsPurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) {}
        }
        billingPreferences.setAdsRemoved()
        _uiState.value = _uiState.value.copy(message = "Ads removed. Thank you!")
    }

    companion object {
        // Product IDs must match the in-app products / subscriptions created in Google Play Console.
        const val PRODUCT_REMOVE_ADS = "kitsune_remove_ads"
        // Single Plus subscription product with base plans: plus-weekly, plus-monthly, plus-yearly (offer weekly-trial for trial)
        const val PRODUCT_PLUS = "plus_v1"
        val PLAN_PRODUCT_IDS = listOf(PRODUCT_PLUS)
    }

    private fun planSortKey(basePlanId: String): Int {
        return when {
            basePlanId.contains("weekly", ignoreCase = true) -> 0
            basePlanId.contains("monthly", ignoreCase = true) -> 1
            basePlanId.contains("yearly", ignoreCase = true) -> 2
            else -> 9
        }
    }
}
