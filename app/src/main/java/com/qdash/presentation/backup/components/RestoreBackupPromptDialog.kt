package com.qdash.presentation.backup.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qdash.R
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.designsystem.components.AppDialog
import com.qdash.ui.designsystem.components.AppLoadingState
import com.qdash.ui.designsystem.tokens.ShapeTokens

@Composable
fun RestoreBackupPromptDialog(
    modifiedTime: Long,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedDate = FormatterUtils.formatDate(modifiedTime)
    
    if (isLoading) {
        Dialog(onDismissRequest = {}) {
            Card(
                shape = ShapeTokens.Xl,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AppLoadingState()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.restoring_data),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    } else {
        AppDialog(
            onDismissRequest = onDismiss,
            title = stringResource(id = R.string.restore_backup_title),
            text = stringResource(id = R.string.restore_backup_message, formattedDate),
            confirmButtonText = stringResource(id = R.string.restore_confirm),
            dismissButtonText = stringResource(id = R.string.restore_skip),
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    }
}
