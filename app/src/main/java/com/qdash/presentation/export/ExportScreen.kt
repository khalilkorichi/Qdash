package com.qdash.presentation.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.FinTrackTopBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // User choices
    var selectedReportType by remember { mutableStateOf("MONTHLY") }
    val selectedAccounts = remember { mutableStateListOf<Long>() }
    var includeDebtSection by remember { mutableStateOf(true) }
    var includeSavingsSection by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty() && selectedAccounts.isEmpty()) {
            selectedAccounts.addAll(uiState.accounts.map { it.id })
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("export_screen"),
        topBar = {
            FinTrackTopBar(title = "تصدير التقارير المالية وكشوف الحسابات")
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.exportResult != null) {
                ExportSuccessView(
                    result = uiState.exportResult!!,
                    onClearResult = { viewModel.clearResult() }
                )
            } else if (uiState.isLoading) {
                ExportLoadingView(
                    progressText = uiState.progressText
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    item {
                        ReportTypeSelectorSection(
                            selectedReportType = selectedReportType,
                            onReportTypeSelected = { selectedReportType = it }
                        )
                    }

                    item {
                        AccountSelectorSection(
                            accounts = uiState.accounts,
                            selectedAccounts = selectedAccounts,
                            onToggleAccount = { id ->
                                if (selectedAccounts.contains(id)) {
                                    selectedAccounts.remove(id)
                                } else {
                                    selectedAccounts.add(id)
                                }
                            }
                        )
                    }

                    item {
                        ExportOptionsSection(
                            includeSavingsSection = includeSavingsSection,
                            onIncludeSavingsChanged = { includeSavingsSection = it },
                            includeDebtSection = includeDebtSection,
                            onIncludeDebtChanged = { includeDebtSection = it },
                            isExportEnabled = selectedAccounts.isNotEmpty(),
                            onExportClick = {
                                viewModel.generateReport(
                                    reportType = selectedReportType,
                                    accounts = selectedAccounts.toList(),
                                    includeDebt = includeDebtSection,
                                    includeSavings = includeSavingsSection
                                )
                            },
                            error = uiState.error
                        )
                    }
                }
            }
        }
    }
}
