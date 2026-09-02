package com.qdash.domain.usecase.debt

import com.qdash.domain.model.copyDebt
import com.qdash.domain.repository.DebtRepository

class ForgiveDebtUseCase(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debtId: Long): Result<Unit> {
        val debt = debtRepository.getDebtById(debtId)
            ?: return Result.failure(IllegalArgumentException("الدين غير موجود في قاعدة البيانات!"))

        val originalNotes = debt.notes
        val noteText = if (debt.direction == com.qdash.domain.model.DebtDirection.OWED_TO_ME) {
            "تم الإعفاء من السلفة / مسامحة المدين"
        } else {
            "تم الإعفاء من الدين"
        }
        val updatedNotes = if (originalNotes.isNullOrBlank()) {
            noteText
        } else {
            "$originalNotes\n[$noteText]"
        }

        val updatedDebt = debt.copyDebt(
            remainingAmount = 0.0,
            isClosed = true,
            notes = updatedNotes
        )

        debtRepository.updateDebt(updatedDebt)
        return Result.success(Unit)
    }
}
