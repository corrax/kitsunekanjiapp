package com.kitsune.kanji.japanese.flashcards.ui.billing

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(Unit) {
        billingManager.connect()
        billingManager.refreshProducts()
        billingManager.refreshPurchases()
    }

    LaunchedEffect(state.entitlement.isPlusEntitled) {
        if (state.entitlement.isPlusEntitled) {
            onActivated()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.hero_spring),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.2f,
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Kitsune Plus",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Text(
                    text = "Unlock all tracks, deeper handwriting feedback, and challenge decks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                PlanBonusCard(
                    title = "Free Plan Bonuses",
                    accent = Color(0xFF2A211B),
                    bonuses = listOf(
                        "Starter power-ups: Kitsune Charm x1, Fude Hint x2, Radical Lens x1",
                        "Daily challenge rewards include extra power-up drops"
                    )
                )
            }
            items(state.products, key = { it.productId }) { plan ->
                PlanOptionCard(
                    product = plan,
                    preferTrial = preferTrial,
                    onBuy = {
                        if (activity != null) {
                            billingManager.launchPurchase(
                                activity = activity,
                                productId = plan.productId,
                                preferTrialOffer = preferTrial
                            )
                        }
                    }
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
                Button(
                    onClick = onContinueFree,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue with Free")
                }
            }
            item {
                TextButton(
                    onClick = onContinueFree,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not now")
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
        }
    }
}

@Composable
private fun PlanOptionCard(
    product: BillingProduct,
    preferTrial: Boolean,
    onBuy: () -> Unit
) {
    val bonusCopy = if (product.productId.contains("annual", ignoreCase = true)) {
        listOf(
            "Weekly bonus bundle: Kitsune Charm x2, Fude Hint x3, Radical Lens x2",
            "Seasonal event packs with extra bonus drops"
        )
    } else {
        listOf(
            "Weekly bonus bundle: Kitsune Charm x1, Fude Hint x2, Radical Lens x1",
            "Extra power-up rewards from challenge streaks"
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFC9A9), RoundedCornerShape(16.dp)),
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
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = product.formattedPrice,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFF5A00)
                )
            }
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall
            )
            bonusCopy.forEach { bonus ->
                Text(
                    text = "+ $bonus",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2A211B)
                )
            }
            if (product.hasFreeTrial) {
                Text(
                    text = "Includes a free trial offer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            val cta = if (preferTrial && product.hasFreeTrial) "Start Trial" else "Choose Plan"
            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth()) {
                Text(cta)
            }
        }
    }
}

@Composable
private fun PlanBonusCard(
    title: String,
    accent: Color,
    bonuses: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFC9A9), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            bonuses.forEach { bonus ->
                Text(
                    text = "+ $bonus",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2A211B)
                )
            }
        }
    }
}
