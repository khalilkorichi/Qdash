package com.example.presentation.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.core.preferences.PreferencesManager

@Immutable
data class ExpenseTrendPoint(val label: String, val amount: Double)

@Immutable
data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val incomeChangePercent: Double = 0.0,
    val expenseChangePercent: Double = 0.0,
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
    val error: String? = null,
    val showBalances: Boolean = true,
    val showWalletReminder: Boolean = false,
    val visibleSections: List<String> = emptyList(),
    val accountBalancesVisibility: Map<Long, Boolean> = emptyMap(),
    val aiTimeSuggestion: ContextAwareSuggestion? = null,
    val proactiveInsights: List<String> = emptyList()
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val incomeRepository: IncomeRepository,
    private val templateRepository: TransactionTemplateRepository,
    private val preferencesManager: PreferencesManager,
    private val aiRepository: com.example.domain.repository.AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HomeUiState(
            visibleSections = preferencesManager.dashboardSectionsOrder.split(",")
                .filter { preferencesManager.isSectionVisible(it) },
            showBalances = preferencesManager.showBalanceTotal,
            showWalletReminder = preferencesManager.walletSetupSkipped && !preferencesManager.walletSetupReminderDismissed
        )
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _chartPeriod = MutableStateFlow("WEEK")

    private val dashboardConfigChangedTrigger = preferencesManager.dashboardConfigUpdates
        .onStart { emit(Unit) }

    private val dashboardFlow = combine(
        accountRepository.getAllAccounts(),
        _chartPeriod.flatMapLatest { period ->
            val startOfPreviousMonth = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val startDate = minOf(startOfPreviousMonth, calculateStartDateForPeriod(period))
            transactionRepository.getTransactionsByDateRange(startDate, Long.MAX_VALUE)
        },
        transactionRepository.getRecentTransactions(4),
        categoryRepository.getAllCategories(),
        subscriptionRepository.getAllSubscriptions(),
        templateRepository.getPinnedTemplates(),
        _chartPeriod,
        dashboardConfigChangedTrigger
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
            
            // Precalculate timestamps for current month
            val currentMonthStartCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val currentMonthStart = currentMonthStartCal.timeInMillis
            val currentMonthEnd = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val currentMonthEndVal = currentMonthEnd.timeInMillis

            // Precalculate timestamps for previous month
            val prevMonthStartCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val prevMonthStart = prevMonthStartCal.timeInMillis
            val prevMonthEnd = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val prevMonthEndVal = prevMonthEnd.timeInMillis
            
            var monthlyIn = 0.0
            var monthlyOut = 0.0
            var prevMonthIn = 0.0
            var prevMonthOut = 0.0
            
            transactions.forEach { tx ->
                val date = tx.date
                if (date in currentMonthStart..currentMonthEndVal) {
                    if (tx.kind == TransactionKind.INCOME || tx.kind == TransactionKind.SALARY) {
                        monthlyIn += tx.amount
                    } else if (tx.kind == TransactionKind.EXPENSE || tx.kind == TransactionKind.SAVINGS_WITHDRAWAL) {
                        monthlyOut += tx.amount
                    }
                } else if (date in prevMonthStart..prevMonthEndVal) {
                    if (tx.kind == TransactionKind.INCOME || tx.kind == TransactionKind.SALARY) {
                        prevMonthIn += tx.amount
                    } else if (tx.kind == TransactionKind.EXPENSE || tx.kind == TransactionKind.SAVINGS_WITHDRAWAL) {
                        prevMonthOut += tx.amount
                    }
                }
            }

            val incomeDiff = monthlyIn - prevMonthIn
            val incomeChange = if (prevMonthIn > 0.0) (incomeDiff / prevMonthIn) * 100.0 else if (monthlyIn > 0.0) 100.0 else 0.0

            val expenseDiff = monthlyOut - prevMonthOut
            val expenseChange = if (prevMonthOut > 0.0) (expenseDiff / prevMonthOut) * 100.0 else if (monthlyOut > 0.0) 100.0 else 0.0

            // Budget usage calculation across category limits (optimized)
            val expenseCategoriesWithLimits = categories.filter { it.type == CategoryType.EXPENSE && it.budgetLimit != null }
            var totalLimit = 0.0
            var totalSpentOnLimits = 0.0
            
            if (expenseCategoriesWithLimits.isNotEmpty()) {
                val limitCategoryIds = expenseCategoriesWithLimits.map { it.id }.toSet()
                totalLimit = expenseCategoriesWithLimits.sumOf { it.budgetLimit ?: 0.0 }
                
                transactions.forEach { tx ->
                    if (tx.type == TransactionType.EXPENSE && tx.categoryId in limitCategoryIds) {
                        if (tx.date in currentMonthStart..currentMonthEndVal) {
                            totalSpentOnLimits += tx.amount
                        }
                    }
                }
            }
            
            val budgetRatio = if (totalLimit > 0) (totalSpentOnLimits / totalLimit).toFloat() else 0f

            // Compute Trend Data
            val trendPoints = mutableListOf<ExpenseTrendPoint>()
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            
            if (period == "WEEK") {
                val format = SimpleDateFormat("EEE", Locale("ar"))
                val baseCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = baseCal.timeInMillis
                val oneDayMs = 24 * 60 * 60 * 1000L
                for (i in 6 downTo 0) {
                    val dayStart = todayStart - i * oneDayMs
                    val dayEnd = dayStart + oneDayMs - 1
                    val sum = expenses.filter { it.date in dayStart..dayEnd }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(java.util.Date(dayStart)), sum))
                }
            } else if (period == "MONTH") {
                val format = SimpleDateFormat("dd", Locale("ar"))
                val baseCal = Calendar.getInstance()
                val nowMs = baseCal.timeInMillis
                val oneDayMs = 24 * 60 * 60 * 1000L
                for (i in 29 downTo 0 step 5) {
                    val targetTime = nowMs - i * oneDayMs
                    val startTime = targetTime - 5 * oneDayMs
                    val sum = expenses.filter { it.date in startTime..targetTime }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(java.util.Date(targetTime)), sum))
                }
            } else if (period == "YEAR") {
                val format = SimpleDateFormat("MMM", Locale("ar"))
                val baseCal = Calendar.getInstance()
                for (i in 11 downTo 0) {
                    val c = Calendar.getInstance().apply {
                        timeInMillis = baseCal.timeInMillis
                        add(Calendar.MONTH, -i)
                    }
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
                    
                    val sum = expenses.filter { it.date in startTs..endTs }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(c.time), sum))
                }
            } else { // DAY
                val format = SimpleDateFormat("HH:mm", Locale("ar"))
                val baseCal = Calendar.getInstance()
                val nowMs = baseCal.timeInMillis
                val fourHoursMs = 4L * 60 * 60 * 1000L
                for (i in 5 downTo 0) {
                    val blockEnd = nowMs - i * fourHoursMs
                    val blockStart = blockEnd - fourHoursMs
                    val sum = expenses.filter { it.date in blockStart..blockEnd }.sumOf { it.amount }
                    trendPoints.add(ExpenseTrendPoint(format.format(java.util.Date(blockEnd)), sum))
                }
            }

            HomeUiState(
                totalBalance = totalBal,
                monthlyIncome = monthlyIn,
                monthlyExpense = monthlyOut,
                incomeChangePercent = incomeChange,
                expenseChangePercent = expenseChange,
                accounts = accounts,
                recentTransactions = recentTransactions,
                categories = categories,
                upcomingSubscriptions = subscriptions.filter { it.isActive }.sortedBy { it.nextBillingDate },
                budgetUsagePercent = budgetRatio.coerceIn(0f, 1f),
                chartPeriod = period,
                expenseTrendData = trendPoints,
                pinnedTemplates = pinnedTemplates,
                isLoading = false,
                showBalances = preferencesManager.showBalanceTotal,
                showWalletReminder = preferencesManager.walletSetupSkipped && !preferencesManager.walletSetupReminderDismissed,
                visibleSections = preferencesManager.dashboardSectionsOrder.split(",").filter { preferencesManager.isSectionVisible(it) },
                accountBalancesVisibility = accounts.associate { it.id to preferencesManager.getShowBalanceAcc(it.id) }
            )
        }
    }

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                dashboardFlow.collect { state ->
                    _uiState.value = state.copy(
                        aiTimeSuggestion = _uiState.value.aiTimeSuggestion,
                        proactiveInsights = _uiState.value.proactiveInsights
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
        processAutomaticSalaryAndRenewals()
        fetchAiInsightsAndSuggestions()
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
            fetchAiInsightsAndSuggestions()
            kotlinx.coroutines.delay(600)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun fetchAiInsightsAndSuggestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val accounts = accountRepository.getAllAccounts().first()
                val categories = categoryRepository.getAllCategories().first()
                val recentTxs = transactionRepository.getRecentTransactions(20).first()
                
                // Fetch context suggestion
                launch {
                    try {
                        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                        val dayOfWeek = SimpleDateFormat("EEEE", Locale("ar")).format(java.util.Date())
                        val categoriesListStr = categories.map { "${it.id}: ${it.name} (${it.type.name})" }.joinToString("\n")
                        val recentTxsStr = recentTxs.map { 
                            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(java.util.Date(it.date))
                            "Date: $dateStr, Type: ${it.type.name}, Category ID: ${it.categoryId}, Amount: ${it.amount} DZD, Note: ${it.note}"
                        }.joinToString("\n")

                        val prompt = """
                            You are a context-aware smart assistant for an Algerian financial app. Suggest a transaction template (draft) that the user is most likely to record right now.
                            Current local time information: Hour: $currentHour (0-23), Day of week: $dayOfWeek.
                            Available categories in database:
                            $categoriesListStr

                            User's recent transaction history:
                            $recentTxsStr

                            Based on the current time and history, determine the most relevant transaction they might want to log. If no strong pattern exists, use the hour of the day to suggest a common transaction (e.g., morning coffee/commute, afternoon lunch, evening groceries/bills, night recharge).
                            Return exactly a valid JSON object in this format (do not wrap in markdown unless it is standard json):
                            {
                              "title": "Short Title (Arabic)",
                              "suggestionText": "Short friendly suggestion question in Arabic",
                              "note": "Default note/description in Arabic",
                              "defaultAmount": Double,
                              "targetKeyword": "A keyword from the category name that best matches this transaction",
                              "iconName": "One of: LightMode, Coffee, ShoppingCart, NightsStay, AutoAwesome"
                            }
                        """.trimIndent()

                        val response = aiRepository.generateResponse(prompt, "gemini-2.5-flash")
                        val replyText = response.replyText
                        val cleanJson = replyText.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
                        val json = JSONObject(cleanJson)
                        
                        val aiSuggestion = ContextAwareSuggestion(
                            title = json.getString("title"),
                            suggestionText = json.getString("suggestionText"),
                            note = json.getString("note"),
                            defaultAmount = json.getDouble("defaultAmount"),
                            targetKeyword = json.getString("targetKeyword"),
                            iconName = json.getString("iconName")
                        )
                        _uiState.update { it.copy(aiTimeSuggestion = aiSuggestion) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Fetch insights
                launch {
                    try {
                        val totalBal = accounts.sumOf { it.balance }
                        val currentMonthStartCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val currentMonthStart = currentMonthStartCal.timeInMillis
                        val allTransactions = transactionRepository.getAllTransactions().first()
                        val thisMonthTxs = allTransactions.filter { it.date >= currentMonthStart }
                        val thisMonthExpense = thisMonthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val thisMonthIncome = thisMonthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                        val recentTxsStr = recentTxs.map { 
                            val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(java.util.Date(it.date))
                            "Date: $dateStr, Type: ${it.type.name}, Category ID: ${it.categoryId}, Amount: ${it.amount} DZD, Note: ${it.note}"
                        }.joinToString("\n")

                        val prompt = """
                            You are an advanced AI financial analyst for an Algerian budget and savings app.
                            Analyze the user's financial status:
                            - Current Total Balance: $totalBal DZD
                            - This Month Income: $thisMonthIncome DZD
                            - This Month Expense: $thisMonthExpense DZD
                            - Recent transactions:
                            $recentTxsStr

                            Provide exactly 2 or 3 highly personalized, actionable insights, savings tips, or behavior predictions in Arabic based on their actual spending patterns.
                            Ensure they are friendly, realistic for Algeria, and directly refer to their numbers or categories. Keep each tip to one concise sentence. Use emojis.
                            
                            Return exactly a valid JSON array of strings, for example:
                            [
                              "💡 لاحظت أنك تنفق بكثرة على ...",
                              "📉 بناءً على وتيرة صرفك الحالية ..."
                            ]
                        """.trimIndent()

                        val response = aiRepository.generateResponse(prompt, "gemini-2.5-flash")
                        val replyText = response.replyText
                        val cleanJson = replyText.substringAfter("[").substringBeforeLast("]").let { "[$it]" }
                        val jsonArray = JSONArray(cleanJson)
                        val insights = mutableListOf<String>()
                        for (i in 0 until jsonArray.length()) {
                            insights.add(jsonArray.getString(i))
                        }
                        if (insights.isNotEmpty()) {
                            _uiState.update { it.copy(proactiveInsights = insights) }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun toggleShowBalances() {
        val nextVal = !preferencesManager.showBalanceTotal
        preferencesManager.showBalanceTotal = nextVal
        _uiState.update { it.copy(showBalances = nextVal) }
    }

    fun toggleAccountBalanceVisibility(accountId: Long) {
        val nextVal = !preferencesManager.getShowBalanceAcc(accountId)
        preferencesManager.setShowBalanceAcc(accountId, nextVal)
        _uiState.update { state ->
            val updatedMap = state.accountBalancesVisibility.toMutableMap().apply {
                put(accountId, nextVal)
            }
            state.copy(accountBalancesVisibility = updatedMap)
        }
    }

    fun dismissWalletReminder() {
        preferencesManager.walletSetupReminderDismissed = true
        _uiState.update { it.copy(showWalletReminder = false) }
    }
}
