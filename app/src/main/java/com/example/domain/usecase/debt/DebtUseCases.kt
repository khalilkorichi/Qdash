package com.example.domain.usecase.debt

import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Locale

class AddDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debt: Debt): Long {
        return debtRepository.insertDebt(debt)
    }
}

class RecordDebtPaymentUseCase(
    private val debtRepository: DebtRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(debtId: Long, accountId: Long, amount: Double, paymentType: DebtPaymentType, note: String?, paymentDate: Long = System.currentTimeMillis()): Long {
        val debt = debtRepository.getDebtById(debtId) ?: return -1L
        
        // 1. Log transaction
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = DebtConstants.DEBT_EXPENSE_CATEGORY_ID, // maps to "منزلي" / domestic or bills
                accountId = accountId,
                note = note ?: "تسديد دين: ${debt.title}",
                date = paymentDate
            )
        )
        
        // 2. Insert Payment Record
        val payment = DebtPayment(
            debtId = debtId,
            accountId = accountId,
            amount = amount,
            paymentDate = paymentDate,
            paymentType = paymentType,
            note = note,
            linkedTransactionId = txId
        )
        val paymentId = debtRepository.insertPayment(payment)
        
        // 3. Update debt remaining amount
        val newRemaining = maxOf(0.0, debt.remainingAmount - amount)
        debtRepository.updateDebt(
            debt.copy(
                remainingAmount = newRemaining,
                isClosed = newRemaining <= 0.0
            )
        )
        
        return paymentId
    }
}

class GetDebtPlanUseCase {
    operator fun invoke(debts: List<Debt>, strategy: String): List<Debt> {
        return when (strategy.lowercase()) {
            "snowball" -> debts.sortedBy { it.remainingAmount }
            "avalanche" -> debts.sortedByDescending { it.interestRate ?: 0.0 }
            else -> debts.sortedBy { it.priority }
        }
    }
}

class CompareDebtStrategiesUseCase {
    operator fun invoke(debts: List<Debt>): List<DebtStrategyResult> {
        val activeDebts = debts.filter { !it.isClosed }
        val installments = activeDebts.filter { it.debtType == DebtType.INSTALLMENT }
        if (installments.isEmpty()) return emptyList()
        
        val totalDebt = installments.sumOf { it.remainingAmount }
        if (totalDebt <= 0.0) return emptyList()

        // 1. Snowball strategy
        val snowballMonths = totalDebt / maxOf(DebtConstants.DEFAULT_SNOWBALL_DIVISOR, installments.sumOf { it.minimumPayment })
        val snowballResult = DebtStrategyResult(
            strategyName = "كرة الثلج (الأصغر ديناً أولاً)",
            durationInMonths = snowballMonths,
            estimatedDebtFreeDate = System.currentTimeMillis() + (snowballMonths * 30.0 * 24.0 * 60.0 * 60.0 * 1000.0).toLong(),
            monthlyPaymentNeeded = installments.sumOf { it.minimumPayment },
            paymentScheduleSummary = "سداد أصغر الديون أولاً يمنحك دافعاً نفسياً وسيطرة أسرع عبر غلق ملفات الديون تدريجياً. (الحسابات لا تشمل الديون العادية)"
        )

        // 2. Avalanche strategy
        val avalancheMonths = totalDebt / maxOf(DebtConstants.DEFAULT_AVALANCHE_DIVISOR, installments.sumOf { it.minimumPayment })
        val avalancheResult = DebtStrategyResult(
            strategyName = "سيل العرم / الانهيار الجبلي (الأعلى تكلفة أولاً)",
            durationInMonths = avalancheMonths,
            estimatedDebtFreeDate = System.currentTimeMillis() + (avalancheMonths * 30.0 * 24.0 * 60.0 * 60.0 * 1000.0).toLong(),
            monthlyPaymentNeeded = installments.sumOf { it.minimumPayment },
            paymentScheduleSummary = "التركيز على القروض الأعلى في نسبة الفوائد أو التكاليف أولاً يقلل إجمالي ما تدفعه مستقبلاً. (الحسابات لا تشمل الديون العادية)"
        )

        return listOf(snowballResult, avalancheResult)
    }
}

class GetDebtInsightsUseCase(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(): Flow<List<String>> = flow {
        val debts = debtRepository.getAllDebts().first().filter { !it.isClosed }
        val list = mutableListOf<String>()
        
        if (debts.isEmpty()) {
            list.add("الحمد لله، لا توجد ديون معلقة أو التزامات متأخرة حالياً.")
        } else {
            val regulars = debts.filter { it.debtType == DebtType.REGULAR }
            val totalRemaining = debts.sumOf { it.remainingAmount }
            list.add("إجمالي المبالغ المطلوبة للسداد: ${String.format(Locale.getDefault(), "%,.1f", totalRemaining)} د.ج.")
            
            if (regulars.isNotEmpty()) {
                list.add("لديك ${regulars.size} التزام(ات) دين عادي مستحقة السداد.")
            }

            val highPriority = debts.minByOrNull { it.priority }
            if (highPriority != null) {
                list.add("نوصي بوضع الأولوية لتسوية '${highPriority.title}' للدائن '${highPriority.creditorName}'.")
            }
            
            val closest = debts.minByOrNull { it.remainingAmount }
            if (closest != null && closest != highPriority) {
                list.add("يمكنك تصفية وغلق '${closest.title}' سريعاً لتقليص عدد الدائنين وتصفية ذهنك.")
            }
        }
        emit(list)
    }
}

class CloseDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(id: Long) {
        val debt = debtRepository.getDebtById(id)
        if (debt != null) {
            debtRepository.updateDebt(debt.copy(remainingAmount = 0.0, isClosed = true))
        }
    }
}
