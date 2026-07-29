package com.qdash.domain.usecase.currency

import com.qdash.domain.repository.ExchangeRateRepository

/**
 * Triggers a network fetch of official exchange rates and persists results to Room.
 * Called by:
 *   - [RefreshOfficialRatesWorker] (every 6 hours, background)
 *   - Manual refresh button in [OfficialMarketTab]
 *
 * Returns [Result.success] on success, [Result.failure] if the network is unavailable
 * or the API returns an error. The Worker handles retry logic.
 */
class RefreshOfficialRatesUseCase(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshOfficialRates()
}
