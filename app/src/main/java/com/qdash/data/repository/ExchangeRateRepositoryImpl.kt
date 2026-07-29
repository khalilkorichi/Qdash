package com.qdash.data.repository

import com.qdash.data.local.dao.ExchangeRateDao
import com.qdash.data.local.entities.ExchangeRateEntity
import com.qdash.data.remote.ExchangeRateRemoteDataSource
import com.qdash.data.remote.scraper.ParallelRateScraper
import com.qdash.domain.model.CURRENCY_ARABIC_NAMES
import com.qdash.domain.model.DEFAULT_EXCHANGE_RATES
import com.qdash.domain.model.ExchangeRate
import com.qdash.domain.model.SUPPORTED_CURRENCIES
import com.qdash.domain.repository.ExchangeRateRepository
import com.qdash.domain.usecase.currency.CalculateRateTrendUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExchangeRateRepositoryImpl(
    private val dao: ExchangeRateDao,
    private val remote: ExchangeRateRemoteDataSource,
    private val parallelScraper: ParallelRateScraper = ParallelRateScraper(),
    private val calculateTrend: CalculateRateTrendUseCase = CalculateRateTrendUseCase()
) : ExchangeRateRepository {

    override fun getAllRatesFlow(): Flow<List<ExchangeRate>> =
        dao.getAllRatesFlow().map { entities ->
            SUPPORTED_CURRENCIES.mapNotNull { code ->
                entities.find { it.currencyCode == code }?.toDomain(calculateTrend)
            }
        }

    override fun getRateByCodeFlow(code: String): Flow<ExchangeRate?> =
        dao.getRateByCodeFlow(code).map { it?.toDomain(calculateTrend) }

    override suspend fun refreshOfficialRates(): Result<Unit> = withContext(Dispatchers.IO) {
        remote.fetchOfficialRates().map { freshEntities ->
            val now = System.currentTimeMillis()
            freshEntities.forEach { entity ->
                val existing = dao.getRateByCodeDirect(entity.currencyCode)
                if (existing != null) {
                    dao.updateRemoteRate(
                        code = entity.currencyCode,
                        buyRate = entity.officialBuyRate,
                        sellRate = entity.officialSellRate,
                        updatedAt = now
                    )
                } else {
                    dao.upsertRates(listOf(entity))
                }
            }
        }
    }

    override suspend fun refreshParallelRates(): Result<Unit> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        parallelScraper.scrapeRates().map { scrapedRates ->
            val toUpsert = mutableListOf<ExchangeRateEntity>()
            scrapedRates.forEach { scraped ->
                val existing = dao.getRateByCodeDirect(scraped.currencyCode)
                if (existing != null) {
                    val updated = existing.copy(
                        previousParallelBuyRate = existing.parallelBuyRate ?: existing.previousParallelBuyRate,
                        previousParallelSellRate = existing.parallelSellRate ?: existing.previousParallelSellRate,
                        parallelBuyRate = scraped.buyRate,
                        parallelSellRate = scraped.sellRate,
                        lastUpdatedAt = now
                    )
                    toUpsert.add(updated)
                }
            }
            if (toUpsert.isNotEmpty()) {
                dao.upsertRates(toUpsert)
            }
        }
    }

    override suspend fun seedDefaultRatesIfEmpty() = withContext(Dispatchers.IO) {
        val existing = dao.getAllRatesFlow().first()
        if (existing.isEmpty()) {
            val seedEntities = DEFAULT_EXCHANGE_RATES.map { rate ->
                ExchangeRateEntity(
                    currencyCode = rate.currencyCode,
                    countryFlagEmoji = rate.countryFlagEmoji,
                    officialBuyRate = rate.officialBuyRate,
                    officialSellRate = rate.officialSellRate,
                    previousOfficialBuyRate = null,
                    previousOfficialSellRate = null,
                    parallelBuyRate = rate.parallelBuyRate,
                    parallelSellRate = rate.parallelSellRate,
                    previousParallelBuyRate = null,
                    previousParallelSellRate = null,
                    source = "SEED",
                    lastUpdatedAt = System.currentTimeMillis()
                )
            }
            dao.upsertRates(seedEntities)
        }
    }
}

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun ExchangeRateEntity.toDomain(
    calculateTrend: CalculateRateTrendUseCase
): ExchangeRate {
    val offBuyTrend = calculateTrend(officialBuyRate, previousOfficialBuyRate)
    val offSellTrend = calculateTrend(officialSellRate, previousOfficialSellRate)
    val parBuyTrend = parallelBuyRate?.let { calculateTrend(it, previousParallelBuyRate) }
    val parSellTrend = parallelSellRate?.let { calculateTrend(it, previousParallelSellRate) }

    return ExchangeRate(
        currencyCode = currencyCode,
        currencyName = CURRENCY_ARABIC_NAMES[currencyCode] ?: currencyCode,
        countryFlagEmoji = countryFlagEmoji,
        officialBuyRate = officialBuyRate,
        officialSellRate = officialSellRate,
        officialBuyTrend = offBuyTrend,
        officialSellTrend = offSellTrend,
        parallelBuyRate = parallelBuyRate,
        parallelSellRate = parallelSellRate,
        parallelBuyTrend = parBuyTrend,
        parallelSellTrend = parSellTrend,
        lastUpdatedAt = lastUpdatedAt
    )
}
