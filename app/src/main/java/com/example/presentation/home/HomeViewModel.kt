package com.example.presentation.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@Immutable
data class ExpenseTrendPoint(val label: String, val amount: Double)

@Immutable
data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val upcomingSubscriptions: List<Subscription> = emptyList(),
    val budgetUsagePercent: Float = 0f,
    val chartPeriod: String = "WEEK", // "DAY", "WEEK", "MONTH", "YEAR"
    val expenseTrendData: List<ExpenseTrendPoint> = emptyList(),
    val pinnedTemplates: List<TransactionTemplate> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val incomeRepository: IncomeRepository,
    private val templateRepository: TransactionTemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _chartPeriod = MutableStateFlow("WEEK")

    private val dashboardFlow = combine(
        accountRepository.getAllAccounts(),
        _chartPeriod.flatMapLatest { period ->
            val startDate = calculateStartDateForPeriod(period)
            transactionRepository.getTransactionsByDateRange(startDate, Long.MAX_VALUE)
        },
        transactionRepository.getRecentTransactions(10),
        categoryRepository.getAllCategories(),
        subscriptionRepository.getAllSubscriptions(),
        templateRepository.getPinnedTemplates(),
        _chartPeriod
    ) { array ->
        val accounts = array[0] as List<Account>
        val transactions = array[1] as List<Transaction>
        val recentTransactions = array[2] as List<Transaction>
        val categories = array[3] as List<Category>
        val subscriptions = array[4] as List<Subscription>
        val pinnedTemplates = array[5] as List<TransactionTemplate>
        val period = array[6] as String

        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val totalBal = accounts.sumOf { it.balance }
            
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            
            val txCal = Calendar.getInstance()
            
            var monthlyIn = 0.0
            var monthlyOut = 0.0
            
            transactions.forEach { tx ->
                txCal.timeInMillis = tx.date
                val m = txCal.get(Calendar.MONTH)
                val y = txCal.get(Calendar.YEAR)
                if (m == currentMonth && y == currentYear) {
                    if (tx.type == TransactionType.INCOME) {
                        monthlyIn += tx.amount
                    } else if (tx.type == TransactionType.EXPENSE) {
                        monthlyOut += tx.amount
                    }
                }
            }

            // Budget usage calculation across category limits (optimized)
            val expenseCategoriesWithLimits = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }
            var totalLimit = 0.0
            var totalSpentOnLimits = 0.0
            
            if (expenseCategoriesWithLimits.isNotEmpty()) {
                val limitCategoryIds = expenseCategoriesWithLimits.map { it.id }.toSet()
                totalLimit = expenseCategoriesWithLimits.sumOf { it.budgetLimit ?: 0.0 }
                
                transactions.forEach { tx ->
                    if (tx.type == TransactionType.EXPENSE && tx.categoryId in limitCategoryIds) {
                        txCal.timeInMillis = tx.date
                        if (txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear) {
                            totalSpentOnLimits += tx.amount
                        }
                    }
                }
            }
            
            val budgetRatio = if (totalLimit > 0) (totalSpentOnLimits / totalLimit).toFloat() else 0f

            // Compute Trend Data
            val trendPoints = mutableListOf<ExpenseTrendPoint>()
            
            if (period == "WEEK") {
                val format = SimpleDateFormat("EEE", Locale("ar"))
                for (i in 6 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val dayStart = c.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    val dayEnd = c.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
                    val sum = transactions.filter { it.type == TransactionType.EXPENSE && it.date in dayStart..dayEnd }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(c.time), sum))
                }
            } else if (period == "MONTH") {
                val format = SimpleDateFormat("dd", Locale("ar"))
                for (i in 29 downTo 0 step 5) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.DAY_OF_YEAR, -i)
                    val sum = transactions.filter { 
                        it.type == TransactionType.EXPENSE && 
                        it.date >= c.timeInMillis - (5L*24*60*60*1000) && 
                        it.date <= c.timeInMillis 
                    }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(c.time), sum))
                }
            } else if (period == "YEAR") {
                val format = SimpleDateFormat("MMM", Locale("ar"))
                for (i in 11 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.MONTH, -i)
                    
                    // Precalculate start & end timestamps of target month
                    val monthStartCal = (c.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val startTs = monthStartCal.timeInMillis
                    
                    val monthEndCal = (c.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val endTs = monthEndCal.timeInMillis
                    
                    val sum = transactions.filter {
                        it.type == TransactionType.EXPENSE && it.date in startTs..endTs
                    }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(c.time), sum))
                }
            } else { // DAY
                val format = SimpleDateFormat("HH:mm", Locale("ar"))
                for (i in 5 downTo 0) {
                    val c = Calendar.getInstance()
                    c.add(Calendar.HOUR_OF_DAY, -(i * 4))
                    val blockEnd = c.timeInMillis
                    val blockStart = blockEnd - (4L * 60 * 60 * 1000)
                    val sum = transactions.filter {
                        it.type == TransactionType.EXPENSE && it.date in blockStart..blockEnd
                    }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(c.time), sum))
                }
            }

            HomeUiState(
                totalBalance = totalBal,
                monthlyIncome = monthlyIn,
                monthlyExpense = monthlyOut,
                accounts = accounts,
                recentTransactions = recentTransactions,
                categories = categories,
                upcomingSubscriptions = subscriptions.filter { it.isActive }.sortedBy { it.nextBillingDate },
                budgetUsagePercent = budgetRatio.coerceIn(0f, 1f),
                chartPeriod = period,
                expenseTrendData = trendPoints,
                pinnedTemplates = pinnedTemplates,
                isLoading = false
            )
        }
    }

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                dashboardFlow.collect { state ->
                    _uiState.value = state
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
        processAutomaticSalaryAndRenewals()
    }

    private fun calculateStartDateForPeriod(period: String): Long {
        val cal = Calendar.getInstance()
        val startOfCurrentMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val chartStart = when (period) {
            "DAY" -> {
                cal.add(Calendar.HOUR_OF_DAY, -24)
                cal.timeInMillis
            }
            "WEEK" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            "YEAR" -> {
                cal.add(Calendar.MONTH, -12)
                cal.timeInMillis
            }
            else -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
        }
        return minOf(startOfCurrentMonth, chartStart)
    }

    fun setChartPeriod(period: String) {
        _chartPeriod.value = period
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun processAutomaticSalaryAndRenewals() {
        viewModelScope.launch {
            try {
                // Process automatic salaries if day is reached
                incomeRepository.getActiveIncomeSources().firstOrNull()?.forEach { salary ->
                    val cal = Calendar.getInstance()
                    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
                    
                    // If the current day matches or exceeds the configured dayOfMonth AND its nextExpectedDate is prior/now
                    if (salary.isActive && salary.type == "SALARY" && currentDay >= salary.dayOfMonth) {
                        val salaryExpectedCal = Calendar.getInstance().apply { timeInMillis = salary.nextExpectedDate }
                        val currentMonth = cal.get(Calendar.MONTH)
                        val currentYear = cal.get(Calendar.YEAR)
                        
                        if (salaryExpectedCal.get(Calendar.MONTH) == currentMonth && salaryExpectedCal.get(Calendar.YEAR) == currentYear) {
                            // Calculate current month start and end dates
                            val startOfMonth = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val endOfMonth = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis

                            // Check if we already inserted salary for this month by querying database directly
                            val alreadyInserted = transactionRepository.isTransactionAlreadyInserted(
                                type = TransactionType.INCOME,
                                note = "راتب تلقائي: ${salary.name}",
                                startDate = startOfMonth,
                                endDate = endOfMonth
                            )
                            
                            if (!alreadyInserted) {
                                // Find salary category
                                val categories = categoryRepository.getAllCategories().first()
                                val salaryCat = categories.firstOrNull { it.type == CategoryType.INCOME && it.name.contains("راتب") }
                                val catId = salaryCat?.id ?: 11L // Default or salary ID
                                
                                val transaction = Transaction(
                                    amount = salary.amount,
                                    type = TransactionType.INCOME,
                                    categoryId = catId,
                                    accountId = salary.accountId,
                                    note = "راتب تلقائي: ${salary.name}",
                                    date = System.currentTimeMillis()
                                )
                                
                                // Insert salary transaction
                                transactionRepository.insertTransaction(transaction)
                                
                                // Update salary nextExpectedDate forward by 1 month
                                val nextMonthCal = Calendar.getInstance().apply {
                                    add(Calendar.MONTH, 1)
                                    set(Calendar.DAY_OF_MONTH, salary.dayOfMonth)
                                }
                                incomeRepository.updateIncomeSource(salary.copy(nextExpectedDate = nextMonthCal.timeInMillis))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun quickDeleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(id)
        }
    }
}
