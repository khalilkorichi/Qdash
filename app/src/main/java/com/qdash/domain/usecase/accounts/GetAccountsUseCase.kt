package com.qdash.domain.usecase.accounts

import com.qdash.domain.model.Account
import com.qdash.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAllAccounts()
    }
}
