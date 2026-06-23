package com.example.domain.usecase.accounts

import com.example.domain.model.Account
import com.example.domain.repository.AccountRepository

class DeleteAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account, confirmed: Boolean): Result<Unit> {
        if (!confirmed) {
            return Result.failure(IllegalStateException("يجب تأكيد عملية الحذف صراحةً!"))
        }
        try {
            val txCount = accountRepository.getTransactionCountForAccount(account.id)
            if (txCount > 0) {
                return Result.failure(
                    IllegalStateException("لا يمكن حذف الحساب لأنه يحتوي على $txCount معاملة مالية. يمكنك أرشفته بدلاً من ذلك.")
                )
            }
            accountRepository.deleteAccount(account)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
