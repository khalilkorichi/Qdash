package com.qdash.presentation.subscriptions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

@Immutable
data class SubscriptionsUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val totalMonthlyCost: Double = 0.0,
    val foreignMonthlyTotals: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class SubscriptionsViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    private data class SubStreamPayload(
        val subs: List<Subscription>,
        val accs: List<Account>,
        val cats: List<Category>,
        val dzdMonthly: Double,
        val foreignTotals: Map<String, Double>
    )

    init {
        loadSubscriptions()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadSubscriptions(showInitialLoading = false)
            kotlinx.coroutines.delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadSubscriptions(showInitialLoading: Boolean = true) {
        loadJob?.cancel()
        if (showInitialLoading && _uiState.value.subscriptions.isEmpty()) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }
        val categoriesFlow: Flow<List<Category>> = categoryRepository?.getAllCategories() ?: flowOf(emptyList())

        loadJob = viewModelScope.launch {
            try {
                combine(
                    subscriptionRepository.getAllSubscriptions(),
                    accountRepository.getAllAccounts(),
                    categoriesFlow
                ) { subs, accs, cats ->
                    val activeSubs = subs.filter { it.isActive }
                    fun monthlyEquivalent(sub: Subscription): Double = when (sub.billingCycle.uppercase(Locale.US)) {
                        "YEARLY" -> sub.amount / 12.0
                        "WEEKLY" -> sub.amount * 4.33
                        else -> sub.amount
                    }

                    val dzdTotal = activeSubs
                        .filter { it.currency.equals("DZD", ignoreCase = true) || it.currency.isBlank() }
                        .sumOf { monthlyEquivalent(it) }

                    val foreignMap = activeSubs
                        .filter { !it.currency.equals("DZD", ignoreCase = true) && it.currency.isNotBlank() }
                        .groupBy { it.currency.uppercase(Locale.US) }
                        .mapValues { (_, list) -> list.sumOf { monthlyEquivalent(it) } }

                    SubStreamPayload(
                        subs = subs,
                        accs = accs,
                        cats = cats,
                        dzdMonthly = dzdTotal,
                        foreignTotals = foreignMap
                    )
                }
                    .flowOn(Dispatchers.Default)
                    .collect { payload ->
                        _uiState.update { current ->
                            current.copy(
                                subscriptions = payload.subs,
                                accounts = payload.accs,
                                categories = if (payload.cats.isNotEmpty()) payload.cats else current.categories,
                                totalMonthlyCost = payload.dzdMonthly,
                                foreignMonthlyTotals = payload.foreignTotals,
                                isLoading = false,
                                error = null
                            )
                        }
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
        reminderDaysBefore: Int,
        currency: String = "DZD",
        isAutoShiftableBySalary: Boolean = false
    ) {
        viewModelScope.launch {
            val resolvedCategoryId = resolveValidCategoryId(categoryId)
            val subscription = Subscription(
                name = name,
                amount = amount,
                currency = currency,
                billingCycle = billingCycle,
                nextBillingDate = nextBillingDate,
                accountId = accountId,
                categoryId = resolvedCategoryId,
                reminderDaysBefore = reminderDaysBefore,
                isAutoShiftableBySalary = isAutoShiftableBySalary
            )
            subscriptionRepository.insertSubscription(subscription)
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val resolvedCategoryId = resolveValidCategoryId(subscription.categoryId)
            subscriptionRepository.updateSubscription(subscription.copy(categoryId = resolvedCategoryId))
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(subscription)
        }
    }

    fun toggleSubscriptionActive(subscription: Subscription, active: Boolean) {
        viewModelScope.launch {
            subscriptionRepository.updateSubscription(subscription.copy(isActive = active))
        }
    }

    private suspend fun resolveValidCategoryId(preferredCategoryId: Long): Long {
        val cats = categoryRepository?.getAllCategories()?.firstOrNull() ?: _uiState.value.categories
        if (cats.isEmpty()) return preferredCategoryId
        if (cats.any { it.id == preferredCategoryId }) return preferredCategoryId
        val billsCat = cats.firstOrNull {
            it.type == CategoryType.EXPENSE &&
                    (it.name.contains("فواتير") || it.name.contains("اشتراك") || it.name.contains("إنترنت") || it.name.contains("انترنت"))
        }
        return billsCat?.id ?: cats.firstOrNull { it.type == CategoryType.EXPENSE }?.id ?: cats.first().id
    }
}
