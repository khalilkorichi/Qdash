package com.qdash.domain.usecase.debt

import com.qdash.domain.model.*
import com.qdash.domain.repository.*
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
        
        // 1. Log transaction (categoryId = null avoids FK crashes if user deleted seeded category)
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = null,
                accountId = accountId,
                note = note?.takeIf { it.isNotBlank() } ?: "تسديد دين: ${debt.title}",
                date = paymentDate,
                occurredAt = System.currentTimeMillis()
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
        
        // 3. Update debt remaining amount (epsilon check avoids IEEE-754 residual centimes)
        val newRemaining = maxOf(0.0, debt.remainingAmount - amount)
        val isNowClosed = newRemaining <= 0.005
        val finalRemaining = if (isNowClosed) 0.0 else newRemaining
        val updatedDebt = when (debt) {
            is RegularDebt -> debt.copy(remainingAmount = finalRemaining, isClosed = isNowClosed)
            is InstallmentDebt -> debt.copy(remainingAmount = finalRemaining, isClosed = isNowClosed)
        }
        debtRepository.updateDebt(updatedDebt)
        
        return paymentId
    }
}

class GetDebtPlanUseCase {
    operator fun invoke(debts: List<Debt>, strategy: String): List<Debt> {
        val activeDebts = debts.filter { !it.isClosed }
        return when (strategy.lowercase(Locale.US)) {
            "snowball" -> activeDebts.sortedWith(
                compareBy<Debt> { it.remainingAmount }.thenByDescending { it.interestRate }
            )
            "avalanche" -> activeDebts.sortedWith(
                compareByDescending<Debt> { it.interestRate }.thenBy { it.remainingAmount }
            )
            else -> activeDebts.sortedWith(
                compareBy<Debt> { it.priority }.thenBy { it.dueDate ?: Long.MAX_VALUE }.thenBy { it.id }
            )
        }
    }
}

class CompareDebtStrategiesUseCase {
    private data class SimDebt(
        val id: Long,
        var balance: Double,
        val annualRatePercent: Double,
        val minMonthlyPayment: Double
    )

    operator fun invoke(debts: List<Debt>): List<DebtStrategyResult> {
        val myActiveDebts = debts.filter { !it.isClosed && it.direction == DebtDirection.OWED_BY_ME && it.remainingAmount > 0.005 }
        if (myActiveDebts.isEmpty()) return emptyList()

        val totalDebt = myActiveDebts.sumOf { it.remainingAmount }
        if (totalDebt <= 0.0) return emptyList()

        val baseMinSum = myActiveDebts.sumOf { debt ->
            when (debt) {
                is InstallmentDebt -> debt.minimumPayment.coerceAtLeast(500.0)
                is RegularDebt -> (debt.remainingAmount / 6.0).coerceAtLeast(1000.0)
            }
        }
        // Add a realistic 15% acceleration buffer so extra cash cascades across debts (Snowball vs Avalanche)
        val monthlyBudget = (baseMinSum * 1.15).coerceAtLeast(DebtConstants.DEFAULT_AVALANCHE_DIVISOR)

        val (snowMonths, snowInterest) = simulatePayoff(myActiveDebts, monthlyBudget, avalanche = false)
        val (avalMonths, avalInterest) = simulatePayoff(myActiveDebts, monthlyBudget, avalanche = true)

        val now = System.currentTimeMillis()
        val msPerMonth = 30.4375 * 24.0 * 60.0 * 60.0 * 1000.0
        val interestSaved = (snowInterest - avalInterest).coerceAtLeast(0.0)

        val snowballResult = DebtStrategyResult(
            strategyName = "كرة الثلج (الأصغر رصيداً أولاً)",
            totalInterestCharged = snowInterest,
            durationInMonths = snowMonths,
            estimatedDebtFreeDate = now + (snowMonths * msPerMonth).toLong(),
            monthlyPaymentNeeded = monthlyBudget,
            paymentScheduleSummary = "تغلق أصغر الديون سريعاً لتحرير السيولة وتحقيق زخم نفسي مستمر."
        )

        val avalancheSummary = if (interestSaved >= 10.0) {
            "تركز على الديون الأعلى فائدة أولاً وتوفر عليك حوالي ${com.qdash.core.utils.FormatterUtils.formatCurrency(interestSaved)} من إجمالي التكلفة."
        } else {
            "تركز على الديون الأعلى تكلفة أولاً؛ وبما أن فوائد ديونك متساوية تتطابق المدة مع كرة الثلج."
        }

        val avalancheResult = DebtStrategyResult(
            strategyName = "الانهيار الجليدي (الأعلى فائدة أولاً)",
            totalInterestCharged = avalInterest,
            durationInMonths = avalMonths,
            estimatedDebtFreeDate = now + (avalMonths * msPerMonth).toLong(),
            monthlyPaymentNeeded = monthlyBudget,
            paymentScheduleSummary = avalancheSummary
        )

        return listOf(snowballResult, avalancheResult)
    }

    private fun simulatePayoff(
        debts: List<Debt>,
        monthlyBudget: Double,
        avalanche: Boolean
    ): Pair<Double, Double> {
        val items = debts.map { d ->
            val minPay = when (d) {
                is InstallmentDebt -> d.minimumPayment.coerceAtLeast(500.0)
                is RegularDebt -> (d.remainingAmount / 6.0).coerceAtLeast(1000.0)
            }
            SimDebt(
                id = d.id,
                balance = d.remainingAmount,
                annualRatePercent = d.interestRate.coerceAtLeast(0.0),
                minMonthlyPayment = minPay
            )
        }

        var months = 0
        var totalInterest = 0.0
        val maxMonths = 360

        while (items.any { it.balance > 0.01 } && months < maxMonths) {
            months++
            var remainingBudget = monthlyBudget

            // 1. Accrue monthly interest
            for (item in items) {
                if (item.balance > 0.01 && item.annualRatePercent > 0.0) {
                    val interest = item.balance * (item.annualRatePercent / 100.0 / 12.0)
                    item.balance += interest
                    totalInterest += interest
                }
            }

            // 2. Pay minimum on every open debt
            for (item in items) {
                if (item.balance > 0.01 && remainingBudget > 0.0) {
                    val pay = minOf(item.balance, item.minMonthlyPayment, remainingBudget)
                    item.balance -= pay
                    remainingBudget -= pay
                }
            }

            // 3. Direct all remaining / freed snowball budget to priority target(s)
            val ordered = if (avalanche) {
                items.filter { it.balance > 0.01 }.sortedWith(compareByDescending<SimDebt> { it.annualRatePercent }.thenBy { it.balance })
            } else {
                items.filter { it.balance > 0.01 }.sortedWith(compareBy<SimDebt> { it.balance }.thenByDescending { it.annualRatePercent })
            }

            for (target in ordered) {
                if (remainingBudget <= 0.01) break
                val extra = minOf(target.balance, remainingBudget)
                target.balance -= extra
                remainingBudget -= extra
            }
        }

        return months.toDouble() to totalInterest
    }
}

class GetDebtInsightsUseCase(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(): Flow<List<String>> = flow {
        val allDebts = debtRepository.getAllDebts().first().filter { !it.isClosed }
        val myDebts = allDebts.filter { it.direction == DebtDirection.OWED_BY_ME }
        val lentDebts = allDebts.filter { it.direction == DebtDirection.OWED_TO_ME }
        val list = mutableListOf<String>()

        if (allDebts.isEmpty()) {
            list.add("الحمد لله، لا توجد ديون معلقة أو التزامات متأخرة حالياً.")
        } else {
            if (myDebts.isNotEmpty()) {
                val totalRemaining = myDebts.sumOf { it.remainingAmount }
                list.add("إجمالي المبالغ المطلوبة منك للسداد: ${String.format(Locale.getDefault(), "%,.1f", totalRemaining)} د.ج.")
            }
            if (lentDebts.isNotEmpty()) {
                val totalLent = lentDebts.sumOf { it.remainingAmount }
                list.add("لديك ${lentDebts.size} سلفة مستحقة لك عند الغير بإجمالي: ${String.format(Locale.getDefault(), "%,.1f", totalLent)} د.ج.")
            }

            val regulars = myDebts.filterIsInstance<RegularDebt>()
            val installments = myDebts.filterIsInstance<InstallmentDebt>()

            val highPriority = installments.minByOrNull { it.priority }
            if (highPriority != null) {
                list.add("نوصي بوضع الأولوية لتسوية '${highPriority.title}' للدائن '${highPriority.creditorName}'.")
            }

            val closest = myDebts.minByOrNull { it.remainingAmount }
            if (closest != null && (highPriority == null || closest.id != highPriority.id)) {
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
            val updatedDebt = when (debt) {
                is RegularDebt -> debt.copy(remainingAmount = 0.0, isClosed = true)
                is InstallmentDebt -> debt.copy(remainingAmount = 0.0, isClosed = true)
            }
            debtRepository.updateDebt(updatedDebt)
        }
    }
}
