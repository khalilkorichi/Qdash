package com.qdash.presentation.transactions

import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.domain.model.Account
import com.qdash.domain.model.AccountType
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import com.qdash.domain.model.TransactionType
import com.qdash.domain.repository.CategorizationRepository
import com.qdash.domain.usecase.transaction.GetSmartCategorySuggestionUseCase
import com.qdash.domain.usecase.transaction.LearnCategoryMappingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddTransactionViewModelTest {

    private lateinit var viewModel: AddTransactionViewModel

    private val accountA = Account(
        id = 1L,
        name = "الحساب الرئيسي",
        balance = 320.0,
        type = AccountType.CASH,
        color = "#6C63FF",
        icon = "account_balance"
    )
    private val accountB = Account(
        id = 2L,
        name = "حساب البريد",
        balance = 500.0,
        type = AccountType.CCP,
        color = "#22C55E",
        icon = "account_balance"
    )

    @Before
    fun setUp() {
        val fakeEngine = object : CategorizationEngine {
            override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion {
                return CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
            }
        }
        val fakeRepository = java.lang.reflect.Proxy.newProxyInstance(
            CategorizationRepository::class.java.classLoader,
            arrayOf(CategorizationRepository::class.java)
        ) { _, method, _ ->
            when (method.returnType) {
                Flow::class.java -> flowOf<Any>()
                List::class.java -> emptyList<Any>()
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> 1L
                else -> null
            }
        } as CategorizationRepository

        val fakeGetSmartCategorySuggestionUseCase = GetSmartCategorySuggestionUseCase(engine = fakeEngine)
        val fakeLearnCategoryMappingUseCase = LearnCategoryMappingUseCase(repository = fakeRepository)

        viewModel = AddTransactionViewModel(
            getSmartCategorySuggestionUseCase = fakeGetSmartCategorySuggestionUseCase,
            learnCategoryMappingUseCase = fakeLearnCategoryMappingUseCase
        )
    }

    @Test
    fun `test edit expense on same account restores original amount first`() {
        // Given current balance = 320 DZD (which includes original -680 DZD expense)
        // User edits the 680 DZD expense to 1000 DZD expense
        viewModel.initEditMode(
            amount = 680.0,
            type = TransactionType.EXPENSE,
            accountId = accountA.id
        )

        val accounts = listOf(accountA, accountB)
        val preview = viewModel.calculatePreviewBalances(
            accounts = accounts,
            selectedAccountId = accountA.id,
            toAccountId = null,
            type = TransactionType.EXPENSE,
            parsedAmount = 1000.0
        )

        // Base balance = 320 + 680 = 1000 DZD
        // Preview balance = 1000 - 1000 = 0 DZD
        assertEquals(0.0, preview[accountA.id]!!, 0.001)
    }

    @Test
    fun `test edit expense to same amount yields zero balance change`() {
        viewModel.initEditMode(
            amount = 680.0,
            type = TransactionType.EXPENSE,
            accountId = accountA.id
        )

        val accounts = listOf(accountA, accountB)
        val preview = viewModel.calculatePreviewBalances(
            accounts = accounts,
            selectedAccountId = accountA.id,
            toAccountId = null,
            type = TransactionType.EXPENSE,
            parsedAmount = 680.0
        )

        // Base balance = 320 + 680 = 1000 DZD
        // Preview balance = 1000 - 680 = 320 DZD (no change from current balance)
        assertEquals(320.0, preview[accountA.id]!!, 0.001)
    }

    @Test
    fun `test edit expense with account change restores original account and impacts new account`() {
        viewModel.initEditMode(
            amount = 680.0,
            type = TransactionType.EXPENSE,
            accountId = accountA.id
        )

        val accounts = listOf(accountA, accountB)
        val preview = viewModel.calculatePreviewBalances(
            accounts = accounts,
            selectedAccountId = accountB.id, // Changed to Account B
            toAccountId = null,
            type = TransactionType.EXPENSE,
            parsedAmount = 1000.0
        )

        // Account A (restored): 320 + 680 = 1000 DZD
        assertEquals(1000.0, preview[accountA.id]!!, 0.001)

        // Account B (new impact): 500 - 1000 = -500 DZD
        assertEquals(-500.0, preview[accountB.id]!!, 0.001)
    }

    @Test
    fun `test non-edit mode applies direct impact to selected account`() {
        // New transaction (non-edit mode)
        val accounts = listOf(accountA, accountB)
        val preview = viewModel.calculatePreviewBalances(
            accounts = accounts,
            selectedAccountId = accountA.id,
            toAccountId = null,
            type = TransactionType.EXPENSE,
            parsedAmount = 200.0
        )

        // 320 - 200 = 120 DZD
        assertEquals(120.0, preview[accountA.id]!!, 0.001)
    }
}
