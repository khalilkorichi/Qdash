package com.qdash.domain.usecase.accounts

import com.qdash.domain.model.Account
import com.qdash.domain.repository.AccountRepository

class UpdateAccountsOrderUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(orderedAccounts: List<Account>) {
        orderedAccounts.forEachIndexed { index, account ->
            accountRepository.updateAccount(account.copy(sortOrder = index))
        }
    }
}
