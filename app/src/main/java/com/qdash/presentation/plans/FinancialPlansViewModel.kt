package com.qdash.presentation.plans

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.domain.model.FinancialPlanType
import com.qdash.domain.repository.FinancialPlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class FinancialPlansUiState(
    val plans: List<FinancialPlan> = emptyList(),
    val activePlans: List<FinancialPlan> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FinancialPlansViewModel(
    private val financialPlanRepository: FinancialPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialPlansUiState())
    val uiState: StateFlow<FinancialPlansUiState> = _uiState.asStateFlow()

    init {
        loadPlans()
    }

    private fun loadPlans() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                financialPlanRepository.getAllPlans().collect { allPlans ->
                    val activeOnly = allPlans.filter { it.status == FinancialPlanStatus.ACTIVE }
                    _uiState.update {
                        it.copy(
                            activePlans = activeOnly,
                            plans = allPlans,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun addPlan(
        title: String,
        type: FinancialPlanType,
        targetAmount: Double,
        notes: String? = null,
        color: String = "#6C63FF",
        icon: String = "flag"
    ) {
        viewModelScope.launch {
            try {
                val plan = FinancialPlan(
                    title = title.trim(),
                    type = type,
                    targetAmount = targetAmount,
                    notes = notes?.trim()?.ifBlank { null },
                    color = color,
                    icon = icon,
                    status = FinancialPlanStatus.ACTIVE
                )
                financialPlanRepository.insertPlan(plan)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun updatePlan(plan: FinancialPlan) {
        viewModelScope.launch {
            try {
                val resolvedStatus = when {
                    plan.status == FinancialPlanStatus.PAUSED || plan.status == FinancialPlanStatus.CANCELLED -> plan.status
                    plan.targetAmount > 0 && plan.currentAmount >= plan.targetAmount -> FinancialPlanStatus.COMPLETED
                    else -> FinancialPlanStatus.ACTIVE
                }
                financialPlanRepository.updatePlan(
                    plan.copy(
                        title = plan.title.trim(),
                        notes = plan.notes?.trim()?.ifBlank { null },
                        status = resolvedStatus
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun updateProgress(id: Long, amount: Double) {
        viewModelScope.launch {
            try {
                val safeAmount = amount.coerceAtLeast(0.0)
                financialPlanRepository.updateCurrentAmount(id, safeAmount)
                val existing = financialPlanRepository.getPlanById(id)
                if (existing != null) {
                    if (existing.targetAmount > 0 && safeAmount >= existing.targetAmount && existing.status == FinancialPlanStatus.ACTIVE) {
                        financialPlanRepository.updateStatus(id, FinancialPlanStatus.COMPLETED)
                    } else if (safeAmount < existing.targetAmount && existing.status == FinancialPlanStatus.COMPLETED) {
                        financialPlanRepository.updateStatus(id, FinancialPlanStatus.ACTIVE)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun deletePlan(plan: FinancialPlan) {
        viewModelScope.launch {
            try {
                financialPlanRepository.deletePlan(plan)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun updateStatus(id: Long, status: FinancialPlanStatus) {
        viewModelScope.launch {
            try {
                financialPlanRepository.updateStatus(id, status)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
