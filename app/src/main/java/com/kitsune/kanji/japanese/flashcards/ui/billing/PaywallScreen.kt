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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.kitsune.kanji.japanese.flashcards.R
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingManager
import com.kitsune.kanji.japanese.flashcards.data.billing.BillingProduct

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    preferTrial: Boolean,
    onContinueFree: () -> Unit,
    onActivated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val state by billingManager.uiState.collectAsState()
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        billingManager.connect()
        billingManager.refreshProducts()
        billingManager.refreshPurchases()
    }

    LaunchedEffect(state.products, preferTrial) {
        if (selectedProductId != null) return@LaunchedEffect
        selectedProductId = defaultProductSelection(state.products, preferTrial)?.productId
    }

    LaunchedEffect(state.entitlement.isPlusEntitled) {
        if (state.entitlement.isPlusEntitled) {
            onActivated()
        }
    }

    val selectedProduct = state.products.firstOrNull { it.productId == selectedProductId }
        ?: defaultProductSelection(state.products, preferTrial)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.hero_spring),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.22f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xF2FFFFFF), Color(0xD9FFF0E6))
                    )
                )
        )

        if (state.isConnecting && state.products.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Study Smarter With Kitsune Plus",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Text(
                    text = "Build vocabulary, grammar, and sentence confidence with adaptive daily decks and faster reinforcement.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                BenefitBullet("Adaptive decks that target weak words and grammar patterns.")
            }
            item {
                BenefitBullet("All tracks unlocked: JLPT, school, work, and conversation.")
            }
            item {
                BenefitBullet("Priority review loops and richer assist analytics per card.")
            }

            if (state.products.isEmpty()) {
                item {
                    Text(
                        text = "Plans are temporarily unavailable. You can continue free and try again later.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(state.products, key = { it.productId }) { plan ->
                    PlanOptionCard(
                        product = plan,
                        selected = plan.productId == selectedProduct?.productId,
                        onSelect = { selectedProductId = plan.productId }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (activity != null && selectedProduct != null) {
                            billingManager.launchPurchase(
                                activity = activity,
                                productId = selectedProduct.productId,
                                preferTrialOffer = selectedProduct.hasFreeTrial
                            )
                        }
                    },
                    enabled = activity != null && selectedProduct != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(primaryCtaForPlan(selectedProduct))
                }
            }
            item {
                Text(
                    text = ctaSupportForPlan(selectedProduct),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                OutlinedButton(
                    onClick = { billingManager.refreshPurchases() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restore Purchases")
                }
            }
            item {
                TextButton(
                    onClick = onContinueFree,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No thanks, I'll use the free version")
                }
            }
            item {
                state.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private fun defaultProductSelection(products: List<BillingProduct>, preferTrial: Boolean): BillingProduct? {
    if (products.isEmpty()) return null
    val weeklyWithTrial = products.firstOrNull {
        it.productId.contains("weekly", ignoreCase = true) && it.hasFreeTrial
    }
    val weekly = products.firstOrNull { it.productId.contains("weekly", ignoreCase = true) }
    val anyTrial = products.firstOrNull { it.hasFreeTrial }
    return if (preferTrial) weeklyWithTrial ?: anyTrial ?: weekly ?: products.first() else weekly ?: products.first()
}

private fun primaryCtaForPlan(selectedProduct: BillingProduct?): String {
    if (selectedProduct == null) return "Continue with Free"
    return if (selectedProduct.hasFreeTrial) "Try Pro For $0.00" else "Upgrade to Plus"
}

private fun ctaSupportForPlan(selectedProduct: BillingProduct?): String {
    if (selectedProduct == null) return "No payment required to stay on Free."
    return if (selectedProduct.hasFreeTrial) {
        "No payment required today. Cancel anytime during trial."
    } else {
        "Billed securely through Google Play. Cancel anytime."
    }
}

@Composable
private fun BenefitBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(8.dp)
                .height(8.dp)
                .background(Color(0xFF1FA080), CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF2A211B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlanOptionCard(
    product: BillingProduct,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val isWeekly = product.productId.contains("weekly", ignoreCase = true)
    val borderColor = if (selected) Color(0xFFC67D39) else Color(0xFFD7CBC0)
    val badgeText = when {
        isWeekly && product.hasFreeTrial -> "Best for starting"
        product.productId.contains("annual", ignoreCase = true) -> "Best value"
        else -> null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(20.dp)
                            .border(2.dp, if (selected) Color(0xFFC67D39) else Color(0xFFBBAA9A), CircleShape)
                            .background(if (selected) Color(0x33C67D39) else Color.Transparent, CircleShape)
                    )
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                badgeText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF1FA080))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = planHeadline(product, isWeekly),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2A211B)
            )
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5B4D43)
            )
        }
    }
}

private fun planHeadline(product: BillingProduct, isWeekly: Boolean): String {
    return when {
        isWeekly && product.hasFreeTrial -> "Try 3 days free, then ${product.formattedPrice}/week"
        isWeekly -> "${product.formattedPrice}/week"
        product.productId.contains("annual", ignoreCase = true) -> "${product.formattedPrice}/year"
        product.productId.contains("monthly", ignoreCase = true) -> "${product.formattedPrice}/month"
        else -> product.formattedPrice
    }
}
