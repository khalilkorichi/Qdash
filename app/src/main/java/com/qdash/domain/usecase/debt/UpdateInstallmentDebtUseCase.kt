package com.qdash.domain.usecase.debt

import com.qdash.domain.model.InstallmentDebt
import com.qdash.domain.repository.DebtRepository
import kotlinx.coroutines.flow.first

class UpdateInstallmentDebtUseCase(
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
        icon: String,
        interestRate: Double,
        dueDate: Long?
    ): Result<Unit> {
        val existingDebt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        if (existingDebt !is InstallmentDebt) {
            return Result.failure(IllegalArgumentException("هذا الالتزام ليس ديناً مقسطاً!"))
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
            minimumPayment = minimumPayment,
            paymentFrequency = paymentFrequency,
            linkedAccountId = linkedAccountId,
            priority = priority,
            notes = notes,
            color = color,
            icon = icon,
            interestRate = interestRate,
            dueDate = dueDate,
            isClosed = newRemaining <= 0.0
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
