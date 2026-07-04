package com.qdash.presentation.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.FinancialPlan
import com.qdash.domain.model.FinancialPlanStatus
import com.qdash.domain.model.FinancialPlanType
import com.qdash.domain.repository.FinancialPlanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
                financialPlanRepository.getActivePlans().collect { activePlans ->
                    _uiState.update {
                        it.copy(
                            activePlans = activePlans,
                            plans = activePlans,
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
                    title = title,
                    type = type,
                    targetAmount = targetAmount,
                    notes = notes,
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

    fun updateProgress(id: Long, amount: Double) {
        viewModelScope.launch {
            try {
                financialPlanRepository.updateCurrentAmount(id, amount)
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
