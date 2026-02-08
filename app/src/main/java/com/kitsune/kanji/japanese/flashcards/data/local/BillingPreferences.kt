package com.kitsune.kanji.japanese.flashcards.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.billingPrefsDataStore by preferencesDataStore(name = "billing_prefs")

data class BillingEntitlement(
    val isPlusEntitled: Boolean,
    val activePlanId: String?,
    val lastPurchaseToken: String?,
    val updatedAtEpochMillis: Long
)

class BillingPreferences(private val context: Context) {
    val entitlementFlow: Flow<BillingEntitlement> =
        context.billingPrefsDataStore.data.map { prefs ->
            BillingEntitlement(
                isPlusEntitled = prefs[KEY_IS_PLUS_ENTITLED] ?: false,
                activePlanId = prefs[KEY_ACTIVE_PLAN_ID],
                lastPurchaseToken = prefs[KEY_LAST_PURCHASE_TOKEN],
                updatedAtEpochMillis = prefs[KEY_UPDATED_AT_EPOCH_MS] ?: 0L
            )
        }

    suspend fun setPlusEntitlement(planId: String, purchaseToken: String?) {
        context.billingPrefsDataStore.edit { prefs ->
            prefs[KEY_IS_PLUS_ENTITLED] = true
            prefs[KEY_ACTIVE_PLAN_ID] = planId
            if (purchaseToken != null) {
                prefs[KEY_LAST_PURCHASE_TOKEN] = purchaseToken
            }
            prefs[KEY_UPDATED_AT_EPOCH_MS] = System.currentTimeMillis()
        }
    }

    suspend fun clearEntitlement() {
        context.billingPrefsDataStore.edit { prefs ->
            prefs[KEY_IS_PLUS_ENTITLED] = false
            prefs.remove(KEY_ACTIVE_PLAN_ID)
            prefs.remove(KEY_LAST_PURCHASE_TOKEN)
            prefs[KEY_UPDATED_AT_EPOCH_MS] = System.currentTimeMillis()
        }
    }

    companion object {
        private val KEY_IS_PLUS_ENTITLED = booleanPreferencesKey("is_plus_entitled")
        private val KEY_ACTIVE_PLAN_ID = stringPreferencesKey("active_plan_id")
        private val KEY_LAST_PURCHASE_TOKEN = stringPreferencesKey("last_purchase_token")
        private val KEY_UPDATED_AT_EPOCH_MS = longPreferencesKey("updated_at_epoch_ms")
    }
}
