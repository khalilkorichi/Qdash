package com.qdash.domain.usecase.debt

import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtType
import com.qdash.domain.repository.DebtRepository
import kotlinx.coroutines.flow.first

class UpdateDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        debtId: Long,
        title: String,
        creditorName: String,
        totalAmount: Double,
        minimumPayment: Double,
        paymentFrequency: String,
        linkedAccountId: Long?,
        priority: Int,
        notes: String?,
        color: String,
        interestRate: Double?,
        dueDate: Long?,
        debtType: DebtType
    ): Result<Unit> {
        val existingDebt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        // Fetch payments to calculate what has already been paid
        val payments = debtRepository.getPaymentsForDebt(debtId).first()
        val totalPaid = payments.sumOf { it.amount }

        if (totalAmount < totalPaid) {
            return Result.failure(
                IllegalArgumentException("المبلغ الإجمالي الجديد ($totalAmount د.ج) لا يمكن أن يكون أقل من إجمالي المبالغ المدفوعة سابقاً ($totalPaid د.ج)!")
            )
        }

        val newRemaining = totalAmount - totalPaid
        val updatedDebt = existingDebt.copy(
            title = title,
            creditorName = creditorName,
            totalAmount = totalAmount,
            remainingAmount = newRemaining,
            minimumPayment = if (debtType == DebtType.REGULAR) 0.0 else minimumPayment,
            paymentFrequency = if (debtType == DebtType.REGULAR) "NONE" else paymentFrequency,
            linkedAccountId = linkedAccountId,
            priority = if (debtType == DebtType.REGULAR) 3 else priority,
            notes = notes,
            color = color,
            interestRate = if (debtType == DebtType.REGULAR) null else interestRate,
            dueDate = dueDate,
            debtType = debtType,
            isClosed = newRemaining <= 0.0
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
