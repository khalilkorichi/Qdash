package com.qdash.domain.usecase.salary

import com.qdash.domain.model.*
import com.qdash.domain.repository.SalaryDistributionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GetSalaryDistributionUseCase(
    private val repository: SalaryDistributionRepository
) {
    fun forSalary(salaryId: Long): Flow<Pair<SalaryDistribution?, List<SalaryEnvelope>>> {
        return repository.getDistributionForSalary(salaryId).flatMapLatest { distribution ->
            if (distribution == null) {
                flowOf(Pair(null, emptyList()))
            } else {
                repository.getEnvelopesForDistribution(distribution.id).map { envelopes ->
                    Pair(distribution, envelopes)
                }
            }
        }
    }
}
