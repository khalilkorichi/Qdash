package com.example.domain.usecase.savings

import com.example.domain.model.*
import com.example.domain.repository.*
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
        
        // 1. Log transaction
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.EXPENSE,
                categoryId = 10L, // "ادخار" / Miscellaneous or other default category
                accountId = accountId,
                note = note ?: "إيداع لهدف الادخار: ${goal.name}",
                date = date
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
        
        // 1. Log income transaction
        val txId = transactionRepository.insertTransaction(
            Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                categoryId = 11L, 
                accountId = accountId,
                note = note ?: "سحب من هدف الادخار: ${goal.name}",
                date = date
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
                list.add(SavingsInsight("أنت قريب جداً من تحقيق هدف '${closest.name}' بنسبة ${(closest.currentAmount/closest.targetAmount*100).toInt()}%. El Hamdoulillah!", true, "insights"))
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
        if (goal.isCompleted) return "الهدف مكتمل بنسبة 100%!"
        
        val targetRemaining = goal.targetAmount - goal.currentAmount
        val monthlyPace = if (goal.currentAmount > 0) goal.currentAmount / 2.0 else 2000.0 // basic estimation
        
        val months = targetRemaining / maxOf(500.0, monthlyPace)
        return if (months <= 1) {
            "بالوتيرة الحالية، يمكنك إكمال هذا الهدف خلال أقل من شهر!"
        } else {
            "عند نفس الوتيرة، سيكتمل هذا الهدف في غضون ${String.format(Locale.US, "%.1f", months)} شهور تقريبًا."
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
