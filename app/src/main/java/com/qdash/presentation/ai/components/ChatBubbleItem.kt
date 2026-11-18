package com.qdash.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.presentation.ai.AiChatMessage
import com.qdash.presentation.ai.DraftField

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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // In RTL: Alignment.Start = Right (User side), Alignment.End = Left (AI side, matching AiMessageBubble)
    val alignment = if (isRtl) {
        if (message.isUser) Alignment.Start else Alignment.End
    } else {
        if (message.isUser) Alignment.End else Alignment.Start
    }
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
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
            border = if (message.isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.widthIn(max = 300.dp)
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
            Spacer(modifier = Modifier.height(6.dp))
            WalletAccountsBalanceCard(
                snapshot = message.walletSnapshot,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Recent Activity Summary Card
        if (message.recentActivitySummary != null) {
            Spacer(modifier = Modifier.height(6.dp))
            RecentActivitySummaryCard(
                summary = message.recentActivitySummary,
                messageId = message.id,
                onDuplicateClick = onDuplicateTransaction,
                onEditClick = onStartEditingTransaction,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Wallet Distribution Suggestion Card
        if (message.walletDistributionSuggestion != null) {
            Spacer(modifier = Modifier.height(6.dp))
            WalletDistributionSuggestionCard(
                suggestion = message.walletDistributionSuggestion,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Low Balance Alert Card
        if (message.lowBalanceAlertState != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LowBalanceAlertCard(
                state = message.lowBalanceAlertState,
                messageId = message.id,
                editedLimit = message.editedLowBalanceLimit,
                onLimitFieldChange = { _, limit -> onUpdateLowBalanceLimitField?.invoke(limit) },
                onSaveLimitClick = { _, limit -> onSaveLowBalanceLimit?.invoke(limit) },
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Transfer Draft Card
        if (message.transferDraftState != null) {
            Spacer(modifier = Modifier.height(6.dp))
            TransferDraftCard(
                draft = message.transferDraftState,
                message = message,
                accounts = accounts,
                onUpdateField = { field, value -> onUpdateDraftField?.invoke(field, value) },
                onConfirm = { onConfirmTransfer?.invoke() },
                onCancel = { onCancelTransfer?.invoke() },
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Selected Account Details Card
        if (message.selectedAccountDetailsState != null) {
            Spacer(modifier = Modifier.height(6.dp))
            SelectedAccountDetailsCard(
                state = message.selectedAccountDetailsState,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Quick Impact Preview Card
        if (message.quickImpactPreviewState != null) {
            Spacer(modifier = Modifier.height(6.dp))
            QuickImpactPreviewCard(
                state = message.quickImpactPreviewState,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        // Transaction Draft Card
        if (message.draftTransaction != null) {
            Spacer(modifier = Modifier.height(6.dp))
            TransactionDraftCard(
                draft = message.draftTransaction,
                message = message,
                accounts = accounts,
                categories = categories,
                onUpdateDraftField = onUpdateDraftField,
                onConfirmDraft = onConfirmDraft,
                onCancelDraft = onCancelDraft,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }
    }
}
