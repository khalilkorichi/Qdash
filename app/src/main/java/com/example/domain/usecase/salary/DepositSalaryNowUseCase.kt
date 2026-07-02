package com.example.domain.usecase.salary

import com.example.domain.model.CategoryType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.repository.IncomeRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DepositSalaryNowUseCase(
    private val incomeRepository: IncomeRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(salaryId: Long, delayId: Long) {
        // 1. Fetch the delays to find the one we want to cancel
        val delays = incomeRepository.getSalaryDelays(salaryId).first()
        val delay = delays.firstOrNull { it.id == delayId } ?: return

        // 2. Delete/Cancel the salary delay (which restores original dates for salary & subscriptions)
        incomeRepository.deleteSalaryDelay(delayId)

        // 3. Fetch the restored salary details
        val salary = incomeRepository.getIncomeSourceById(salaryId) ?: return

        // 4. Find the income category for "salary" (containing "راتب")
        val categories = categoryRepository.getAllCategories().first()
        val salaryCat = categories.firstOrNull { it.type == CategoryType.INCOME && it.name.contains("راتب") }
        val catId = salaryCat?.id ?: categories.firstOrNull { it.type == CategoryType.INCOME }?.id ?: 11L

        // 5. Create and insert the income transaction
        val transaction = Transaction(
            amount = salary.amount,
            type = TransactionType.INCOME,
            categoryId = catId,
            accountId = salary.accountId,
            note = "إيداع راتب: ${salary.name}",
            date = System.currentTimeMillis()
        )
        transactionRepository.insertTransaction(transaction)

        // 6. Update the salary nextExpectedDate forward by 1 month from the original expected date
        val originalCal = Calendar.getInstance().apply { timeInMillis = delay.originalDate }
        val nextMonthCal = Calendar.getInstance().apply {
            timeInMillis = originalCal.timeInMillis
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, salary.dayOfMonth)
        }
        incomeRepository.updateIncomeSource(salary.copy(nextExpectedDate = nextMonthCal.timeInMillis))
    }
}
