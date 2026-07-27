package com.qdash.presentation.subscriptions

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class SubscriptionsUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val totalMonthlyCost: Double = 0.0,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class SubscriptionsViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptions()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSubscriptions()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private var loadJob: kotlinx.coroutines.Job? = null

    private fun loadSubscriptions() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        loadJob = viewModelScope.launch {
            try {
                combine(
                    subscriptionRepository.getAllSubscriptions(),
                    accountRepository.getAllAccounts()
                ) { subs, accs ->
                    val total = subs.filter { it.isActive }.sumOf {
                        when (it.billingCycle.uppercase()) {
                            "YEARLY" -> it.amount / 12.0
                            "WEEKLY" -> it.amount * 4.33
                            else -> it.amount // MONTHLY
                        }
                    }
                    _uiState.value.copy(
                        subscriptions = subs,
                        accounts = accs,
                        totalMonthlyCost = total,
                        isLoading = false
                    )
                }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
                .collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun addSubscription(
        name: String,
        amount: Double,
        billingCycle: String,
        nextBillingDate: Long,
        accountId: Long,
        categoryId: Long,
        reminderDaysBefore: Int
    ) {
        viewModelScope.launch {
            val subscription = Subscription(
                name = name,
                amount = amount,
                billingCycle = billingCycle,
                nextBillingDate = nextBillingDate,
                accountId = accountId,
                categoryId = categoryId,
                reminderDaysBefore = reminderDaysBefore
            )
            subscriptionRepository.insertSubscription(subscription)
        }
    }

    fun toggleSubscriptionActive(subscription: Subscription, active: Boolean) {
        viewModelScope.launch {
            subscriptionRepository.updateSubscription(subscription.copy(isActive = active))
        }
    }
}
