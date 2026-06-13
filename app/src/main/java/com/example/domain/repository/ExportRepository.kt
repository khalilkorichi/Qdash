package com.example.domain.repository

import com.example.domain.model.ExportReportRequest
import com.example.domain.model.ExportResult

interface ExportRepository {
    suspend fun exportPdfReport(request: ExportReportRequest): ExportResult
}
