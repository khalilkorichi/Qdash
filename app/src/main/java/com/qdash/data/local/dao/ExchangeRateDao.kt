package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    /** Observe all rates — Room emits on every write automatically. */
    @Query("SELECT * FROM exchange_rates ORDER BY currencyCode ASC")
    fun getAllRatesFlow(): Flow<List<ExchangeRateEntity>>

    /** Observe a single rate by ISO code. */
    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code LIMIT 1")
    fun getRateByCodeFlow(code: String): Flow<ExchangeRateEntity?>

    /** Synchronous lookup for repository update calculations. */
    @Query("SELECT * FROM exchange_rates WHERE currencyCode = :code LIMIT 1")
    suspend fun getRateByCodeDirect(code: String): ExchangeRateEntity?

    /** Insert or replace rates from remote API or initial seed. */
    @Upsert
    suspend fun upsertRates(rates: List<ExchangeRateEntity>)

    /**
     * Update official rates from remote API.
     * Copies previous officialBuyRate / officialSellRate to compute trends.
     */
    @Query("""
        UPDATE exchange_rates
        SET previousOfficialBuyRate = officialBuyRate,
            previousOfficialSellRate = officialSellRate,
            officialBuyRate = :buyRate,
            officialSellRate = :sellRate,
            source = 'REMOTE',
            lastUpdatedAt = :updatedAt
        WHERE currencyCode = :code
    """)
    suspend fun updateRemoteRate(code: String, buyRate: Double, sellRate: Double, updatedAt: Long)
}
