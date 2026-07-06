package com.qdash.domain.usecase.accounts

import com.qdash.domain.model.Account
import com.qdash.domain.repository.AccountRepository

class ManageAccountUseCase(private val accountRepository: AccountRepository) {
    suspend fun createAccount(account: Account): Long {
        return accountRepository.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        accountRepository.updateAccount(account)
    }

    suspend fun setDefaultAccount(id: Long) {
        accountRepository.setDefaultAccount(id)
    }
}
