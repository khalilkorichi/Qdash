package com.qdash.domain.usecase.debt

import com.qdash.domain.repository.DebtRepository

class ForgiveDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtId: Long): Result<Unit> {
        val debt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        val originalNotes = debt.notes
        val updatedNotes = if (originalNotes.isNullOrBlank()) {
            "تم الإعفاء من الدين"
        } else {
            "$originalNotes\n[تم الإعفاء من الدين]"
        }

        val updatedDebt = debt.copy(
            remainingAmount = 0.0,
            isClosed = true,
            notes = updatedNotes
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
