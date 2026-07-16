package com.qdash.presentation.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("transfer_screen"),
        topBar = {
            FinTrackTopBar(title = "تحويل الأرصدة المالية")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // MAIN HERO TRANSFER HUB
            item {
                TransferFormCard(
                    accounts = uiState.accounts,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onExecuteTransfer = { fromId, toId, amt, fee, note, date, occurredAt, onSuccess ->
                        viewModel.executeTransfer(
                            fromAccountId = fromId,
                            toAccountId = toId,
                            amount = amt,
                            feeAmount = fee,
                            note = note,
                            date = date,
                            occurredAt = occurredAt,
                            onComplete = onSuccess
                        )
                    }
                )
            }

            // SECTION HEADER: RECENT TRANSFERS
            item {
                Text(
                    text = "سجل العمليات والتحويلات الأخيرة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // TIMELINE / LIST OF TRANSFERS (Cardless, Inline Design)
            if (uiState.transfers.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "لا توجد حركات تحويل مسجلة",
                        description = "يمكنك تحويل الأرصدة والسيولة المالية بين حساباتك المختلفة لتتبع الأرصدة بدقة وتسجيل الرسوم المرفقة."
                    )
                }
            } else {
                items(uiState.transfers) { record ->
                    TransferRecordItem(
                        record = record,
                        accounts = uiState.accounts
                    )
                }
            }
        }
    }
}
