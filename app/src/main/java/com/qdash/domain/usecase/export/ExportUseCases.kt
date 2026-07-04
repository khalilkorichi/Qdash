package com.qdash.domain.usecase.export

import com.qdash.domain.model.ExportReportRequest
import com.qdash.domain.model.ExportResult
import com.qdash.domain.repository.ExportRepository

class ExportMonthlyPdfReportUseCase(private val repository: ExportRepository) {
    suspend operator fun invoke(selectedAccounts: List<Long>, language: String): ExportResult {
        return repository.exportPdfReport(
            ExportReportRequest(
                reportType = "MONTHLY_STATEMENT",
                selectedAccounts = selectedAccounts,
                language = language,
                includeDebtSection = false,
                includeSavingsSection = false
            )
        )
    }
}

class ExportAnalyticsPdfUseCase(private val repository: ExportRepository) {
    suspend operator fun invoke(language: String): ExportResult {
        return repository.exportPdfReport(
            ExportReportRequest(
                reportType = "ANALYTICS_REPORT",
                language = language,
                includeCharts = true,
                includeDebtSection = true,
                includeSavingsSection = true
            )
        )
    }
}

class ExportSavingsPdfUseCase(private val repository: ExportRepository) {
    suspend operator fun invoke(language: String): ExportResult {
        return repository.exportPdfReport(
            ExportReportRequest(
                reportType = "SAVINGS_STATUS_REPORT",
                language = language,
                includeSavingsSection = true,
                includeDebtSection = false,
                includeTransactions = false
            )
        )
    }
}

class ExportDebtPdfUseCase(private val repository: ExportRepository) {
    suspend operator fun invoke(language: String): ExportResult {
        return repository.exportPdfReport(
            ExportReportRequest(
                reportType = "DEBT_REPAYMENT_PLAN_REPORT",
                language = language,
                includeDebtSection = true,
                includeSavingsSection = false,
                includeTransactions = false
            )
        )
    }
}

class ExportAccountStatementPdfUseCase(private val repository: ExportRepository) {
    suspend operator fun invoke(accountId: Long, language: String): ExportResult {
        return repository.exportPdfReport(
            ExportReportRequest(
                reportType = "ACCOUNT_STATEMENT",
                selectedAccounts = listOf(accountId),
                language = language,
                includeDebtSection = false,
                includeSavingsSection = false
            )
        )
    }
}
