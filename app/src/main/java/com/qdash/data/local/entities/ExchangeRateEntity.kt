package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent cache for exchange rates.
 * Stores current and previous rates for official and parallel markets to compute trend indicators.
 * Fully isolated from manual user inputs (manual rates are stored session-only in UI state).
 */
@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,       // ISO 4217: "USD", "EUR", …
    val countryFlagEmoji: String,               // Flag emoji: "🇺🇸"
    val officialBuyRate: Double,                // DZD per 1 foreign unit (buy from bank)
    val officialSellRate: Double,               // DZD per 1 foreign unit (sell to bank)
    val previousOfficialBuyRate: Double? = null,
    val previousOfficialSellRate: Double? = null,
    val parallelBuyRate: Double? = null,
    val parallelSellRate: Double? = null,
    val previousParallelBuyRate: Double? = null,
    val previousParallelSellRate: Double? = null,
    val source: String = "REMOTE",
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val isManualOverride: Boolean = false
)
