package com.kitsune.kanji.japanese.flashcards.ui.billing

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
import com.kitsune.kanji.japanese.flashcards.BuildConfig
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingManager
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingProduct

private val AccentOrange = Color(0xFFFF5A00)
private val AccentGreen = Color(0xFF1FA080)
private val TextDark = Color(0xFF2A211B)
private val TextMuted = Color(0xFF7A6A60)
private val CardSurface = Color(0xFFFFFBF8)

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    preferTrial: Boolean,
    onContinueFree: () -> Unit,
    onActivated: () -> Unit
) {
    val state by billingManager.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(CardSurface)) {

        if (state.isConnecting && state.products.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Top
        ) {
            // ── Hero image with back button overlaid ──────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hero_spring),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient scrim so content flows naturally into card
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color(0x55000000),
                                    0.55f to Color(0x00000000),
                                    1f to CardSurface
                                )
                            )
                    )
                    // Back button
                    IconButton(
                        onClick = onContinueFree,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(4.dp)
                            .align(Alignment.TopStart)
                            .background(Color(0x99FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                }
            }

            // ── Headline ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "Learn From\nYour World",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        lineHeight = 40.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Free users get 3 camera captures a week. Plus removes the limit so you can turn anything you see into practice.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            }

            // ── Benefit bullets ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(10.dp))
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BenefitBullet("Snap menus, signs, manga, textbooks — no weekly limit.")
                    BenefitBullet("Every word you capture is queued into your next daily deck automatically.")
                    BenefitBullet("Build vocabulary from your real life, not just preset lists.")
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
            }

            // ── Inline paywall (plan selector, card, CTA, restore, skip) ───
            item {
                InlinePaywallContent(
                    billingManager = billingManager,
                    preferTrial = preferTrial,
                    onContinueFree = onContinueFree,
                    onActivated = onActivated
                )
            }
        }
    }
}

/**
 * Inline paywall content: plan selector, plan card, CTA, restore, skip.
 * Use in PaywallScreen or in onboarding so users can subscribe without navigating away.
 */
@Composable
fun InlinePaywallContent(
    billingManager: BillingManager,
    preferTrial: Boolean,
    onContinueFree: () -> Unit,
    onActivated: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    val state by billingManager.uiState.collectAsState()
    var selectedOfferToken by remember { mutableStateOf<String?>(null) }
    var showUnavailableError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingManager.connect()
        billingManager.refreshProducts()
        billingManager.refreshPurchases()
    }

    LaunchedEffect(state.products, preferTrial) {
        if (selectedOfferToken != null) return@LaunchedEffect
        selectedOfferToken = defaultProductSelection(state.products, preferTrial)?.offerToken
    }

    LaunchedEffect(state.entitlement.isPlusEntitled) {
        if (state.entitlement.isPlusEntitled) {
            onActivated()
        }
    }

    val selectedProduct = state.products.firstOrNull { it.offerToken == selectedOfferToken }
        ?: defaultProductSelection(state.products, preferTrial)

    if (state.isConnecting && state.products.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (state.products.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PlanSelectorButtonGroup(
                products = state.products,
                selectedOfferToken = selectedOfferToken,
                onPlanSelected = {
                    selectedOfferToken = it
                    showUnavailableError = false
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        if (state.products.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val plan = selectedProduct ?: state.products.first()
            val discountLabel = discountVsPreviousPlan(state.products, plan)
            PlanOptionCard(
                product = plan,
                selected = true,
                onSelect = { },
                discountLabel = discountLabel,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (state.products.isEmpty()) {
                        showUnavailableError = true
                    } else if (activity != null && selectedProduct != null) {
                        billingManager.launchPurchase(
                            activity = activity,
                            productId = selectedProduct.productId,
                            offerToken = selectedProduct.offerToken
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = primaryCtaForPlan(selectedProduct),
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            if (showUnavailableError) {
                Text(
                    text = "Plans are temporarily unavailable. Please try again later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = ctaSupportForPlan(selectedProduct),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        TextButton(
            onClick = { billingManager.refreshPurchases() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Restore Purchases",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (BuildConfig.ENABLE_ADS) {
            if (state.isAdsRemoved) {
                Text(
                    text = "Ads removed — thank you!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            } else {
                state.removeAdsProduct?.let { removeAdsProduct ->
                    TextButton(
                        onClick = {
                            if (activity != null) {
                                billingManager.launchRemoveAdsPurchase(activity)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Remove ads forever — ${formatPriceWithCurrency(removeAdsProduct.formattedPrice, removeAdsProduct.priceCurrencyCode)}",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = onContinueFree,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "No thanks, I'll stay on the free plan",
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun defaultProductSelection(products: List<BillingProduct>, preferTrial: Boolean): BillingProduct? {
    if (products.isEmpty()) return null
    val weeklyWithTrial = products.firstOrNull {
        it.basePlanId.contains("weekly", ignoreCase = true) && it.hasFreeTrial
    }
    val weekly = products.firstOrNull { it.basePlanId.contains("weekly", ignoreCase = true) }
    val anyTrial = products.firstOrNull { it.hasFreeTrial }
    return if (preferTrial) weeklyWithTrial ?: anyTrial ?: weekly ?: products.first() else weekly ?: products.first()
}

private fun primaryCtaForPlan(selectedProduct: BillingProduct?): String {
    if (selectedProduct == null) return "Upgrade to Plus"
    val isWeeklyWithTrial = selectedProduct.basePlanId.contains("weekly", ignoreCase = true) && selectedProduct.hasFreeTrial
    return if (isWeeklyWithTrial) "Try 3 days free" else "Upgrade to Plus"
}

private fun ctaSupportForPlan(selectedProduct: BillingProduct?): String {
    if (selectedProduct == null) return "Billed securely through Google Play. Cancel anytime."
    return if (selectedProduct.hasFreeTrial) {
        "No payment required today. Cancel anytime during your trial."
    } else {
        "Billed securely through Google Play. Cancel anytime."
    }
}

@Composable
private fun BenefitBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .background(AccentGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Returns e.g. "Save 20% vs weekly" or null for weekly / when not computable. */
private fun discountVsPreviousPlan(products: List<BillingProduct>, product: BillingProduct): String? {
    val idx = products.indexOfFirst { it.offerToken == product.offerToken }.takeIf { it >= 0 } ?: return null
    if (idx == 0) return null
    val prev = products[idx - 1]
    val prevMicrosPerMonth = microsPerMonth(prev)
    val currMicrosPerMonth = microsPerMonth(product)
    if (prevMicrosPerMonth <= 0 || currMicrosPerMonth <= 0) return null
    val savePercent = ((1 - currMicrosPerMonth.toDouble() / prevMicrosPerMonth) * 100).toInt().coerceIn(1, 99)
    val vsLabel = when {
        prev.basePlanId.contains("weekly", ignoreCase = true) -> "weekly"
        prev.basePlanId.contains("monthly", ignoreCase = true) -> "monthly"
        else -> "previous plan"
    }
    return "Save $savePercent% vs $vsLabel"
}

private fun microsPerMonth(product: BillingProduct): Long {
    val micros = product.priceAmountMicros
    if (micros <= 0L) return 0L
    return when {
        product.basePlanId.contains("weekly", ignoreCase = true) -> micros * 52 / 12
        product.basePlanId.contains("monthly", ignoreCase = true) -> micros
        product.basePlanId.contains("yearly", ignoreCase = true) -> micros / 12
        else -> 0L
    }
}

@Composable
private fun PlanSelectorButtonGroup(
    products: List<BillingProduct>,
    selectedOfferToken: String?,
    onPlanSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F0EB)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        products.forEachIndexed { index, product ->
            val isSelected = product.offerToken == selectedOfferToken
            val isFirst = index == 0
            val isLast = index == products.lastIndex
            val segmentShape = when {
                isFirst && isLast -> RoundedCornerShape(12.dp)
                isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 12.dp)
                isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp, bottomStart = 0.dp)
                else -> RoundedCornerShape(0.dp)
            }
            val discountStr = discountVsPreviousPlan(products, product)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) AccentOrange else Color.Transparent,
                        segmentShape
                    )
                    .clickable { onPlanSelected(product.offerToken) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            product.basePlanId.contains("weekly", ignoreCase = true) -> "Weekly"
                            product.basePlanId.contains("monthly", ignoreCase = true) -> "Monthly"
                            product.basePlanId.contains("yearly", ignoreCase = true) -> "Yearly"
                            else -> product.basePlanId
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextDark
                    )
                    if (discountStr != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = discountStr.replaceFirst("Save ", "").replace(" vs ", "\nvs "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else AccentGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFDDD0C8))
                )
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    product: BillingProduct,
    selected: Boolean,
    onSelect: () -> Unit,
    discountLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val isWeekly = product.basePlanId.contains("weekly", ignoreCase = true)
    val isAnnual = product.basePlanId.contains("yearly", ignoreCase = true)
    val borderColor = if (selected) AccentOrange else Color(0xFFDDD0C8)
    val bgColor = if (selected) Color(0xFFFFF4EE) else Color.White
    val badgeText = when {
        isWeekly && product.hasFreeTrial -> "3 days free"
        isAnnual -> "Best value"
        else -> null
    }
    val badgeColor = if (isAnnual) AccentGreen else AccentOrange

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (selected) 3.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Radio dot
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(2.dp, if (selected) AccentOrange else Color(0xFFBBAA9A), CircleShape)
                    .background(if (selected) AccentOrange.copy(alpha = 0.15f) else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AccentOrange, CircleShape)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = planHeadline(product, isWeekly),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    lineHeight = 22.sp
                )
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                if (discountLabel != null) {
                    Text(
                        text = discountLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = AccentGreen
                    )
                }
            }

            badgeText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeColor)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/** Unambiguous currency prefix for $‑based and other multi‑symbol currencies (ISO 4217). */
private fun currencyDisplayPrefix(code: String): String? = when (code.uppercase()) {
    "USD" -> "US$"
    "CAD" -> "CA$"
    "SGD" -> "S$"
    "AUD" -> "A$"
    "NZD" -> "NZ$"
    "HKD" -> "HK$"
    "TWD" -> "NT$"
    "MXN" -> "MX$"
    else -> null
}

/** Price string with unambiguous symbol when applicable (e.g. CA$, S$, US$). */
private fun formatPriceWithCurrency(formattedPrice: String, currencyCode: String): String {
    if (formattedPrice.isBlank()) return formattedPrice
    val prefix = currencyDisplayPrefix(currencyCode) ?: return formattedPrice
    val amountPart = formattedPrice.replace(Regex("^[^\\d\\u00a0.,]+"), "").trim()
    return if (amountPart.isNotEmpty()) "$prefix$amountPart" else formattedPrice
}

/** Plan headline; uses [formatPriceWithCurrency] so dollar currencies show as CA$, S$, etc. */
private fun planHeadline(product: BillingProduct, isWeekly: Boolean): String {
    val price = formatPriceWithCurrency(product.formattedPrice, product.priceCurrencyCode)
    return when {
        isWeekly && product.hasFreeTrial -> "Try free, then $price/week"
        isWeekly -> "$price/week"
        product.basePlanId.contains("yearly", ignoreCase = true) -> "$price/year"
        product.basePlanId.contains("monthly", ignoreCase = true) -> "$price/month"
        else -> price
    }
}
