package com.qdash.presentation.export

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import com.qdash.domain.repository.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class ExportUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val progressText: String = "",
    val exportResult: ExportResult? = null,
    val error: String? = null
)

class ExportViewModel(
    private val exportRepository: ExportRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    init {
        loadExportData()
    }

    private fun loadExportData() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun generateReport(reportType: String, accounts: List<Long>, includeDebt: Boolean, includeSavings: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, progressText = "جاري تجميع المعاملات والمدفوعات...", exportResult = null, error = null) }
            delay(1200)
            _uiState.update { it.copy(progressText = "جاري تشكيل المخططات المالية وهياكل الإحصاء...") }
            delay(1000)
            _uiState.update { it.copy(progressText = "جاري صياغة التقرير ورصف البيانات بصفحات PDF باللغة العربية...") }
            delay(800)
            
            val request = ExportReportRequest(
                reportType = reportType,
                selectedAccounts = accounts,
                includeDebtSection = includeDebt,
                includeSavingsSection = includeSavings,
                language = "AR"
            )
            val result = exportRepository.exportPdfReport(request)
            if (result.success) {
                _uiState.update { it.copy(isLoading = false, progressText = "", exportResult = result) }
            } else {
                _uiState.update { it.copy(isLoading = false, progressText = "", error = result.message) }
            }
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(exportResult = null, error = null, isLoading = false) }
    }
}
