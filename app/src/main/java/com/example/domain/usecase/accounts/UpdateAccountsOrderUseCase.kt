package com.example.domain.usecase.accounts

import com.example.domain.model.Account
import com.example.domain.repository.AccountRepository

class UpdateAccountsOrderUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(orderedAccounts: List<Account>) {
        orderedAccounts.forEachIndexed { index, account ->
            accountRepository.updateAccount(account.copy(sortOrder = index))
        }
    }
}
