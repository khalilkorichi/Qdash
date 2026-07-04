package com.qdash.presentation.ai.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.presentation.ai.AiChatMessage
import com.qdash.presentation.ai.DraftField
import com.qdash.ui.designsystem.tokens.ColorTokens

@Composable
fun ChatBubbleItem(
    message: AiChatMessage,
    accounts: List<Account> = emptyList(),
    categories: List<Category> = emptyList(),
    onConfirmDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    onUpdateDraftField: ((DraftField, Any) -> Unit)? = null,
    onConfirmTransfer: (() -> Unit)? = null,
    onCancelTransfer: (() -> Unit)? = null,
    onSaveLowBalanceLimit: ((Double) -> Unit)? = null,
    onUpdateLowBalanceLimitField: ((Double) -> Unit)? = null,
    onDuplicateTransaction: ((Transaction) -> Unit)? = null,
    onStartEditingTransaction: ((String, Transaction) -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bgColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                MarkdownMessageText(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        // Wallet / Portfolio balance card
        if (message.walletSnapshot != null) {
            Spacer(modifier = Modifier.height(4.dp))
            WalletAccountsBalanceCard(
                snapshot = message.walletSnapshot,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent Activity Summary Card
        if (message.recentActivitySummary != null) {
            Spacer(modifier = Modifier.height(4.dp))
            RecentActivitySummaryCard(
                summary = message.recentActivitySummary,
                messageId = message.id,
                onDuplicateClick = onDuplicateTransaction,
                onEditClick = onStartEditingTransaction,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Wallet Distribution Suggestion Card
        if (message.walletDistributionSuggestion != null) {
            Spacer(modifier = Modifier.height(4.dp))
            WalletDistributionSuggestionCard(
                suggestion = message.walletDistributionSuggestion,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Low Balance Alert Card
        if (message.lowBalanceAlertState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            LowBalanceAlertCard(
                state = message.lowBalanceAlertState,
                messageId = message.id,
                editedLimit = message.editedLowBalanceLimit,
                onLimitFieldChange = { msgId, limit -> onUpdateLowBalanceLimitField?.invoke(limit) },
                onSaveLimitClick = { msgId, limit -> onSaveLowBalanceLimit?.invoke(limit) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Transfer Draft Card
        if (message.transferDraftState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TransferDraftCard(
                draft = message.transferDraftState,
                message = message,
                accounts = accounts,
                onUpdateField = { field, value -> onUpdateDraftField?.invoke(field, value) },
                onConfirm = { onConfirmTransfer?.invoke() },
                onCancel = { onCancelTransfer?.invoke() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Selected Account Details Card
        if (message.selectedAccountDetailsState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            SelectedAccountDetailsCard(
                state = message.selectedAccountDetailsState,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Quick Impact Preview Card
        if (message.quickImpactPreviewState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            QuickImpactPreviewCard(
                state = message.quickImpactPreviewState,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Transaction Draft Card
        if (message.draftTransaction != null) {
            Spacer(modifier = Modifier.height(4.dp))
            TransactionDraftCard(
                draft = message.draftTransaction,
                message = message,
                accounts = accounts,
                categories = categories,
                onUpdateDraftField = onUpdateDraftField,
                onConfirmDraft = onConfirmDraft,
                onCancelDraft = onCancelDraft,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
