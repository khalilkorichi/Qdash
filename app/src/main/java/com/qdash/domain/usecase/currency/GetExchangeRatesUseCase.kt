package com.qdash.domain.usecase.currency

import com.qdash.domain.model.ExchangeRate
import com.qdash.domain.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.Flow

/**
 * Returns a hot Flow of all cached exchange rates for supported currencies.
 * The repository handles ordering and filtering; UI simply collects.
 */
class GetExchangeRatesUseCase(
    private val repository: ExchangeRateRepository
) {
    operator fun invoke(): Flow<List<ExchangeRate>> = repository.getAllRatesFlow()
}
