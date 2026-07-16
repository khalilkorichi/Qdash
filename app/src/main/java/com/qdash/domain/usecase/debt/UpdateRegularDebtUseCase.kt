package com.qdash.domain.usecase.debt

import com.qdash.domain.model.RegularDebt
import com.qdash.domain.repository.DebtRepository
import kotlinx.coroutines.flow.first

class UpdateRegularDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        debtId: Long,
        title: String,
        creditorName: String,
        totalAmount: Double,
        dueDate: Long?,
        linkedAccountId: Long?,
        notes: String?,
        color: String,
        icon: String
    ): Result<Unit> {
        val existingDebt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        if (existingDebt !is RegularDebt) {
            return Result.failure(IllegalArgumentException("هذا الالتزام ليس ديناً عادياً!"))
        }

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
            dueDate = dueDate,
            linkedAccountId = linkedAccountId,
            notes = notes,
            color = color,
            icon = icon,
            isClosed = newRemaining <= 0.0
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
