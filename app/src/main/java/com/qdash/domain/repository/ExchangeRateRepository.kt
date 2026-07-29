package com.qdash.domain.repository

import com.qdash.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    /** Hot flow of all cached exchange rates from Room. */
    fun getAllRatesFlow(): Flow<List<ExchangeRate>>

    /** Hot flow for a single rate. */
    fun getRateByCodeFlow(code: String): Flow<ExchangeRate?>

    /**
     * Fetch fresh rates from the remote API and persist to Room.
     * Shifts current rates to previous rates to track trends.
     * Returns Result.failure if network is unavailable or API returns an error.
     */
    suspend fun refreshOfficialRates(): Result<Unit>

    /**
     * Fetch fresh parallel rates from forexalgerie.com scraper and persist to Room.
     * Shifts current parallel rates to previous parallel rates to track trends.
     */
    suspend fun refreshParallelRates(): Result<Unit>

    /** Seed default rates if the table is empty (first install / fresh DB). */
    suspend fun seedDefaultRatesIfEmpty()
}
