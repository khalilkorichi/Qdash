package com.example.domain.usecase.salary

import com.example.domain.model.*
import com.example.domain.repository.*
import kotlinx.coroutines.flow.*

class GetSalaryManagementOverviewUseCase(
    private val incomeRepository: IncomeRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val debtRepository: DebtRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<SalaryManagementOverview> {
        return combine(
            incomeRepository.getAllIncomeSources(),
            subscriptionRepository.getActiveSubscriptions(),
            debtRepository.getActiveDebts()
        ) { sources, subs, debts ->
            Triple(sources.firstOrNull { it.type == "SALARY" && it.isActive }, subs, debts)
        }.flatMapLatest { (salary, subs, debts) ->
            if (salary == null) {
                flowOf(SalaryManagementOverview(null, emptyList(), subs, debts))
            } else {
                incomeRepository.getSalaryDelays(salary.id).map { delays ->
                    SalaryManagementOverview(salary, delays, subs, debts)
                }
            }
        }
    }
}
