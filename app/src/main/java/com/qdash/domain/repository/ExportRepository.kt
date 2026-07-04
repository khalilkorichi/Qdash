package com.qdash.domain.repository

import com.qdash.domain.model.ExportReportRequest
import com.qdash.domain.model.ExportResult

interface ExportRepository {
    suspend fun exportPdfReport(request: ExportReportRequest): ExportResult
}
