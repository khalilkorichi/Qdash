package com.qdash.presentation.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.domain.model.ExchangeRate
import com.qdash.domain.model.MarketType
import com.qdash.domain.model.TradeDirection
import com.qdash.domain.usecase.currency.ConvertCurrencyUseCase
import com.qdash.domain.usecase.currency.GetExchangeRatesUseCase
import com.qdash.domain.usecase.currency.RefreshOfficialRatesUseCase
import com.qdash.domain.usecase.currency.RefreshParallelRatesUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI State definitions ──────────────────────────────────────────────────────

sealed class OfficialRatesUiState {
    object Idle : OfficialRatesUiState()
    object Loading : OfficialRatesUiState()
    data class Success(
        val rates: List<ExchangeRate>,
        val lastUpdated: Long?
    ) : OfficialRatesUiState()
    data class Error(val message: String) : OfficialRatesUiState()
}

/** Parallel market ui state object */
object ParallelRatesUiState

data class ConverterUiState(
    val fromCurrency: String = "USD",
    val toCurrency: String = "DZD",
    val amount: String = "",
    val result: Double = 0.0,
    val amountInWords: String? = null,
    val selectedMarketType: MarketType = MarketType.OFFICIAL,
    val selectedTradeDirection: TradeDirection = TradeDirection.BUY,
    val manualRate: String = "",
    val useManualRate: Boolean = false,
    val availableRates: List<ExchangeRate> = emptyList(),
    val isRefreshing: Boolean = false,
    val isRefreshingParallel: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class CurrencyExchangeViewModel(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val refreshOfficialRatesUseCase: RefreshOfficialRatesUseCase,
    private val refreshParallelRatesUseCase: RefreshParallelRatesUseCase,
    private val preferencesManager: com.qdash.core.preferences.PreferencesManager
) : ViewModel() {

    // ── Number System Preference ────────────────────────────────────────────────
    val useWesternNumerals: StateFlow<Boolean> = preferencesManager.dashboardConfigUpdates
        .map { preferencesManager.useWesternNumerals }
        .onStart { emit(preferencesManager.useWesternNumerals) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), preferencesManager.useWesternNumerals)

    // ── Official Rates State ──────────────────────────────────────────────────

    private val _officialRatesState = MutableStateFlow<OfficialRatesUiState>(OfficialRatesUiState.Idle)
    val officialRatesState: StateFlow<OfficialRatesUiState> = _officialRatesState.asStateFlow()

    private var officialRatesLoaded = false

    /** Called by LaunchedEffect when the Official Market tab becomes active. */
    fun loadOfficialRates() {
        if (officialRatesLoaded) return
        officialRatesLoaded = true
        _officialRatesState.value = OfficialRatesUiState.Loading
        viewModelScope.launch {
            getExchangeRatesUseCase()
                .catch { e ->
                    _officialRatesState.value = OfficialRatesUiState.Error(e.message ?: "خطأ غير متوقع")
                }
                .collect { rates ->
                    _officialRatesState.value = OfficialRatesUiState.Success(
                        rates = rates,
                        lastUpdated = rates.maxOfOrNull { it.lastUpdatedAt }?.takeIf { it > 0L }
                    )
                }
        }
    }

    /** Manual refresh from the UI button in the official market tab. */
    fun refreshOfficialRates() {
        viewModelScope.launch {
            val current = _officialRatesState.value
            _officialRatesState.value = OfficialRatesUiState.Loading
            refreshOfficialRatesUseCase()
                .onFailure {
                    _officialRatesState.value = current
                }
        }
    }

    /** Manual refresh from the UI button in the parallel market tab. */
    fun refreshParallelRatesManually() {
        viewModelScope.launch {
            _converterState.update { it.copy(isRefreshingParallel = true) }
            refreshParallelRatesUseCase()
            _converterState.update { it.copy(isRefreshingParallel = false) }
        }
    }

    // ── Converter State ───────────────────────────────────────────────────────

    private val _converterState = MutableStateFlow(ConverterUiState())
    val converterState: StateFlow<ConverterUiState> = _converterState.asStateFlow()

    /** Called by LaunchedEffect when the Converter tab becomes active. */
    fun loadConverterRates() {
        viewModelScope.launch {
            getExchangeRatesUseCase()
                .catch { /* ignore — rates may be empty on first launch */ }
                .collect { rates ->
                    _converterState.update { it.copy(availableRates = rates) }
                    recalculate()
                }
        }
    }

    fun onAmountChange(amount: String) {
        _converterState.update { it.copy(amount = amount) }
        recalculate()
    }

    fun onFromCurrencyChange(code: String) {
        _converterState.update { it.copy(fromCurrency = code) }
        recalculate()
    }

    fun onToCurrencyChange(code: String) {
        _converterState.update { it.copy(toCurrency = code) }
        recalculate()
    }

    fun swapCurrencies() {
        _converterState.update { state ->
            state.copy(
                fromCurrency = state.toCurrency,
                toCurrency = state.fromCurrency,
                amount = if (state.result > 0.0) state.result.toString() else state.amount
            )
        }
        recalculate()
    }

    fun onMarketTypeSelected(type: MarketType) {
        _converterState.update { it.copy(selectedMarketType = type) }
        recalculate()
    }

    fun onTradeDirectionSelected(direction: TradeDirection) {
        _converterState.update { it.copy(selectedTradeDirection = direction) }
        recalculate()
    }

    fun onManualRateChange(rate: String) {
        val parsed = rate.toDoubleOrNull()
        val valid = rate.isNotEmpty() && parsed != null && parsed > 0.0
        _converterState.update {
            it.copy(
                manualRate = rate,
                useManualRate = valid
            )
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _converterState.value
        val amountDouble = state.amount.replace(",", "").toDoubleOrNull() ?: 0.0
        val manualRate = if (state.useManualRate) state.manualRate.toDoubleOrNull() else null

        val result = convertCurrencyUseCase(
            amount = amountDouble,
            fromCode = state.fromCurrency,
            toCode = state.toCurrency,
            rates = state.availableRates,
            marketType = state.selectedMarketType,
            tradeDirection = state.selectedTradeDirection,
            rateType = if (state.useManualRate && manualRate != null) ConvertCurrencyUseCase.RateType.MANUAL else null,
            manualRate = manualRate
        )

        // Compute Arabic words in Centimes for DZD result
        val wordsText = if (state.toCurrency == "DZD" && result > 0.0)
            AmountConversionEngine.getAlgerianColloquialWords(result)
        else null

        _converterState.update { it.copy(result = result, amountInWords = wordsText) }
    }
}
