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
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriodLabel: String?,
    val hasFreeTrial: Boolean,
    val offerToken: String
)

data class BillingUiState(
    val isConnecting: Boolean = true,
    val products: List<BillingProduct> = emptyList(),
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
    private val cachedOfferTokenByProductId = mutableMapOf<String, String>()
    private val cachedTrialOfferTokenByProductId = mutableMapOf<String, String>()

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

    init {
        scope.launch {
            billingPreferences.entitlementFlow.collect { entitlement ->
                _uiState.value = _uiState.value.copy(entitlement = entitlement)
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
            cachedOfferTokenByProductId.clear()
            cachedTrialOfferTokenByProductId.clear()
            val mapped = mutableListOf<BillingProduct>()
            for (details in productDetails) {
                val offers = details.subscriptionOfferDetails.orEmpty()
                if (offers.isEmpty()) continue

                val trialOffer = offers.firstOrNull { offer ->
                    offer.pricingPhases.pricingPhaseList.any { phase ->
                        phase.priceAmountMicros == 0L
                    }
                }
                val baseOffer = offers.firstOrNull()
                val selectedOffer = trialOffer ?: baseOffer ?: continue
                val pricingPhase = selectedOffer.pricingPhases.pricingPhaseList.lastOrNull() ?: continue

                cachedProductDetails[details.productId] = details
                cachedOfferTokenByProductId[details.productId] = selectedOffer.offerToken
                if (trialOffer != null) {
                    cachedTrialOfferTokenByProductId[details.productId] = trialOffer.offerToken
                }

                mapped += BillingProduct(
                    productId = details.productId,
                    title = details.name,
                    description = details.description,
                    formattedPrice = pricingPhase.formattedPrice,
                    billingPeriodLabel = pricingPhase.billingPeriod,
                    hasFreeTrial = trialOffer != null,
                    offerToken = selectedOffer.offerToken
                )
            }

            _uiState.value = _uiState.value.copy(
                products = mapped.sortedBy { planSortKey(it.productId) },
                message = null
            )
        }
    }

    fun launchPurchase(activity: Activity, productId: String, preferTrialOffer: Boolean) {
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

        val offerToken = if (preferTrialOffer) {
            cachedTrialOfferTokenByProductId[productId] ?: cachedOfferTokenByProductId[productId]
        } else {
            cachedOfferTokenByProductId[productId]
        }
        if (offerToken == null) {
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

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) {
                _uiState.value = _uiState.value.copy(message = "Purchase restore failed: ${result.debugMessage}")
                return@queryPurchasesAsync
            }
            scope.launch {
                if (purchases.isEmpty()) {
                    billingPreferences.clearEntitlement()
                } else {
                    purchases.forEach { processPurchase(it) }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            scope.launch {
                purchases.forEach { processPurchase(it) }
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

        val activeProduct = purchase.products.firstOrNull() ?: PLAN_PRODUCT_IDS.first()
        billingPreferences.setPlusEntitlement(
            planId = activeProduct,
            purchaseToken = purchase.purchaseToken
        )
        _uiState.value = _uiState.value.copy(message = "Purchase successful. Plus is now active.")
    }

    companion object {
        const val PRODUCT_PLUS_WEEKLY = "kitsune_plus_weekly"
        const val PRODUCT_PLUS_MONTHLY = "kitsune_plus_monthly"
        const val PRODUCT_PLUS_ANNUAL = "kitsune_plus_annual"

        val PLAN_PRODUCT_IDS = listOf(
            PRODUCT_PLUS_WEEKLY,
            PRODUCT_PLUS_MONTHLY,
            PRODUCT_PLUS_ANNUAL
        )
    }

    private fun planSortKey(productId: String): Int {
        return when {
            productId.contains("weekly", ignoreCase = true) -> 0
            productId.contains("monthly", ignoreCase = true) -> 1
            productId.contains("annual", ignoreCase = true) -> 2
            else -> 9
        }
    }
}
