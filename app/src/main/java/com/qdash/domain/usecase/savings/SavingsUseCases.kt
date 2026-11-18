package com.qdash.domain.usecase.savings

import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Locale

class AddSavingsContributionUseCase(
    private val savingRepository: SavingRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()): Long {
        val goal = savingRepository.getSavingGoalById(goalId) ?: return -1L
        
        // 1. Log transaction without magic categoryId (avoids Entertainment category pollution & FK crashes)
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = null,
                accountId = accountId,
                note = note?.takeIf { it.isNotBlank() } ?: "إيداع لهدف الادخار: ${goal.name}",
                date = date,
                occurredAt = System.currentTimeMillis()
            )
        )
        
        // 2. Insert contribution
        val contribution = SavingsContribution(
            savingGoalId = goalId,
            accountId = accountId,
            amount = amount,
            type = SavingsContributionType.DEPOSIT,
            note = note,
            date = date,
            linkedTransactionId = txId
        )
        val contributionId = savingRepository.insertContribution(contribution)
        
        // 3. Update Saving Goal current level
        val newAmount = goal.currentAmount + amount
        savingRepository.updateSavingGoal(
            goal.copy(
                currentAmount = newAmount,
                isCompleted = newAmount >= goal.targetAmount
            )
        )
        
        return contributionId
    }
}

class WithdrawFromSavingsUseCase(
    private val savingRepository: SavingRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(goalId: Long, accountId: Long, amount: Double, note: String?, date: Long = System.currentTimeMillis()): Long {
        val goal = savingRepository.getSavingGoalById(goalId) ?: return -1L
        
        // 1. Log income transaction without magic Salary categoryId (avoids false Salary notifications)
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                categoryId = null,
                accountId = accountId,
                note = note?.takeIf { it.isNotBlank() } ?: "سحب من هدف الادخار: ${goal.name}",
                date = date,
                occurredAt = System.currentTimeMillis()
            )
        )
        
        // 2. Insert contribution
        val contribution = SavingsContribution(
            savingGoalId = goalId,
            accountId = accountId,
            amount = amount,
            type = SavingsContributionType.WITHDRAWAL,
            note = note,
            date = date,
            linkedTransactionId = txId
        )
        val contributionId = savingRepository.insertContribution(contribution)
        
        // 3. Update Saving Goal level
        val newAmount = maxOf(0.0, goal.currentAmount - amount)
        savingRepository.updateSavingGoal(
            goal.copy(
                currentAmount = newAmount,
                isCompleted = newAmount >= goal.targetAmount
            )
        )
        
        return contributionId
    }
}

class GetSavingsInsightsUseCase(
    private val savingRepository: SavingRepository
) {
    operator fun invoke(): Flow<List<SavingsInsight>> = flow {
        val goals = savingRepository.getAllSavingGoals().first()
        val contributions = savingRepository.getAllContributions().first()
        val list = mutableListOf<SavingsInsight>()
        
        if (goals.isEmpty()) {
            list.add(SavingsInsight("لم تفعل أي خطط للادخار بعد، ابدأ اليوم لتأمين مستقبلك المالي.", false, "info"))
        } else {
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }
            val percentage = if (totalTarget > 0) (totalSaved / totalTarget * 100).toInt() else 0
            
            list.add(SavingsInsight("أحرزت تقدمًا رائعًا بنسبة $percentage% إجماليًا نحو أهدافك الادخارية.", true, "trending_up"))
            
            val closest = goals.filter { !it.isCompleted }.maxByOrNull { if (it.targetAmount > 0) it.currentAmount / it.targetAmount else 0.0 }
            if (closest != null) {
                list.add(SavingsInsight("أنت قريب جداً من تحقيق هدف '${closest.name}' بنسبة ${(closest.currentAmount/closest.targetAmount*100).toInt()}%. ما شاء الله!", true, "insights"))
            }
            
            goals.forEach { goal ->
                val goalContribs = contributions.filter { it.savingGoalId == goal.id }
                if (goalContribs.isEmpty() && !goal.isCompleted) {
                    list.add(SavingsInsight("الهدف '${goal.name}' لم توضع به مساهمة بعد، يمكنك تخصيص قليل من المال له.", false, "warning"))
                }
            }
        }
        emit(list)
    }
}

class GetSavingsForecastUseCase(
    private val savingRepository: SavingRepository
) {
    suspend operator fun invoke(goalId: Long): String {
        val goal = savingRepository.getSavingGoalById(goalId) ?: return "لا توجد توقعات"
        if (goal.isCompleted || goal.currentAmount >= goal.targetAmount) return "الهدف مكتمل بنسبة 100%!"
        
        val targetRemaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val contribs = savingRepository.getContributionsForGoal(goalId).first()
        val netSaved = contribs.sumOf {
            if (it.type == SavingsContributionType.DEPOSIT) it.amount else -it.amount
        }
        val monthlyPace = if (contribs.isNotEmpty() && netSaved > 0.0) {
            val earliest = contribs.minOf { it.date }
            val spanDays = ((System.currentTimeMillis() - earliest).coerceAtLeast(86_400_000L)) / 86_400_000.0
            val spanMonths = (spanDays / 30.0).coerceAtLeast(1.0)
            netSaved / spanMonths
        } else {
            return "أضف أول إيداع في هذه الحصالة لحساب الموعد المتوقع لإكمال الهدف."
        }
        
        val months = targetRemaining / monthlyPace.coerceAtLeast(1.0)
        return if (months <= 1.0) {
            "بالوتيرة الحالية، يمكنك إكمال هذا الهدف خلال أقل من شهر!"
        } else {
            "بالوتيرة الفعلية (${com.qdash.core.utils.FormatterUtils.formatCurrency(monthlyPace)}/شهر)، سيكتمل الهدف خلال ${com.qdash.core.utils.FormatterUtils.convertNumerals(String.format(Locale.US, "%.1f", months))} شهر تقريباً."
        }
    }
}

class GetSavingsHistoryUseCase(
    private val savingRepository: SavingRepository
) {
    operator fun invoke(goalId: Long): Flow<List<SavingsContribution>> {
        return savingRepository.getContributionsForGoal(goalId)
    }
}
