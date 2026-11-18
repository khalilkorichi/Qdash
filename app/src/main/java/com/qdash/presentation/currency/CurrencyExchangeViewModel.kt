package com.qdash.presentation.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.FormatterUtils
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
    val isRefreshingParallel: Boolean = false,
    val refreshError: String? = null
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
    private var converterRatesLoaded = false
    private var autoParallelRefreshTriggered = false

    /** Called by LaunchedEffect when the Official Market or Parallel Market tab becomes active. */
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
                    maybeTriggerInitialParallelRefresh(rates)
                }
        }
    }

    private fun maybeTriggerInitialParallelRefresh(rates: List<ExchangeRate>) {
        if (!autoParallelRefreshTriggered && rates.isNotEmpty() && rates.all { it.parallelBuyRate == null }) {
            autoParallelRefreshTriggered = true
            refreshParallelRatesManually()
        }
    }

    /** Manual refresh from the UI button in the official market tab. */
    fun refreshOfficialRates() {
        if (_converterState.value.isRefreshing) return
        viewModelScope.launch {
            val current = _officialRatesState.value
            if (current !is OfficialRatesUiState.Success) {
                _officialRatesState.value = OfficialRatesUiState.Loading
            }
            _converterState.update { it.copy(isRefreshing = true, refreshError = null) }
            refreshOfficialRatesUseCase()
                .onFailure { e ->
                    if (current !is OfficialRatesUiState.Success) {
                        _officialRatesState.value = current
                    }
                    _converterState.update {
                        it.copy(refreshError = e.localizedMessage ?: "تعذّر تحديث الأسعار الرسمية. تحقق من اتصال الإنترنت.")
                    }
                }
            _converterState.update { it.copy(isRefreshing = false) }
        }
    }

    /** Manual refresh from the UI button in the parallel market tab. */
    fun refreshParallelRatesManually() {
        if (_converterState.value.isRefreshingParallel) return
        viewModelScope.launch {
            _converterState.update { it.copy(isRefreshingParallel = true, refreshError = null) }
            refreshParallelRatesUseCase()
                .onFailure { e ->
                    _converterState.update {
                        it.copy(refreshError = e.localizedMessage ?: "تعذّر تحديث أسعار السوق الموازي. تحقق من اتصال الإنترنت.")
                    }
                }
            _converterState.update { it.copy(isRefreshingParallel = false) }
        }
    }

    fun clearRefreshError() {
        _converterState.update { it.copy(refreshError = null) }
    }

    // ── Converter State ───────────────────────────────────────────────────────

    private val _converterState = MutableStateFlow(ConverterUiState())
    val converterState: StateFlow<ConverterUiState> = _converterState.asStateFlow()

    /** Called by LaunchedEffect when the Converter tab becomes active. */
    fun loadConverterRates() {
        if (converterRatesLoaded) return
        converterRatesLoaded = true
        viewModelScope.launch {
            getExchangeRatesUseCase()
                .catch { /* ignore — rates may be empty on first launch */ }
                .collect { rates ->
                    _converterState.update { it.copy(availableRates = rates) }
                    maybeTriggerInitialParallelRefresh(rates)
                    recalculate()
                }
        }
    }

    fun onAmountChange(amount: String) {
        val normalized = FormatterUtils.convertNumerals(amount, useWestern = true)
            .filter { it.isDigit() || it == '.' || it == ',' }
        _converterState.update { it.copy(amount = normalized) }
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
        val normalized = FormatterUtils.convertNumerals(rate, useWestern = true)
        val parsed = AmountConversionEngine.parseFlexibleAmount(normalized)
        val valid = normalized.isNotEmpty() && parsed != null && parsed > 0.0
        _converterState.update {
            it.copy(
                manualRate = normalized,
                useManualRate = valid
            )
        }
        recalculate()
    }

    private fun isSelectedParallelRateMissing(state: ConverterUiState): Boolean {
        if (state.selectedMarketType != MarketType.PARALLEL || state.useManualRate) return false
        if (state.fromCurrency == state.toCurrency) return false
        val codesToCheck = listOf(state.fromCurrency, state.toCurrency).filter { it != "DZD" }
        return codesToCheck.any { code ->
            val rateObj = state.availableRates.find { it.currencyCode == code }
            rateObj == null || rateObj.parallelBuyRate == null
        }
    }

    private fun recalculate() {
        val state = _converterState.value
        val amountDouble = AmountConversionEngine.parseFlexibleAmount(state.amount) ?: 0.0
        val manualRate = if (state.useManualRate) AmountConversionEngine.parseFlexibleAmount(state.manualRate) else null

        if (isSelectedParallelRateMissing(state)) {
            _converterState.update { it.copy(result = 0.0, amountInWords = null) }
            return
        }

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
