package com.qdash.domain.usecase.currency

import com.qdash.domain.repository.ExchangeRateRepository

/**
 * Use case to trigger real-time scraping of Algerian parallel market exchange rates
 * from forexalgerie.com and persist to Room database.
 */
class RefreshParallelRatesUseCase(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshParallelRates()
}
