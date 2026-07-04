package com.qdash.presentation.ai

import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.domain.model.RecentActivitySummary
import com.qdash.domain.model.WalletDistributionSuggestion
import com.qdash.domain.model.LowBalanceAlertState
import com.qdash.domain.model.TransferDraftState
import com.qdash.domain.model.SelectedAccountDetailsState
import com.qdash.domain.model.QuickImpactPreviewState
import java.util.UUID

/**
 * Represents a single account balance snapshot for the WalletAccountsBalanceCard.
 */
data class AccountBalanceItem(
    val id: Long,
    val name: String,
    val typeLabel: String,
    val balance: Double,
    val currency: String,
    val color: String
)

/**
 * A snapshot of the user's full wallet (portfolio) used to render
 * WalletAccountsBalanceCard inside AI chat messages.
 */
data class WalletSnapshot(
    val totalBalance: Double,
    val currency: String,
    val accounts: List<AccountBalanceItem>
)

/**
 * Enum identifying which editable field of a draft transaction is being changed.
 */
enum class DraftField {
    AMOUNT, TYPE, NOTE, CATEGORY_ID, ACCOUNT_ID,
    TRANSFER_AMOUNT, TRANSFER_FROM_ACCOUNT_ID, TRANSFER_TO_ACCOUNT_ID, TRANSFER_NOTE,
    LOW_BALANCE_LIMIT
}

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val draftTransaction: Transaction? = null,
    // Editable draft overlay values (null = fall back to draftTransaction's value)
    val editedAmount: Double? = null,
    val editedType: TransactionType? = null,
    val editedNote: String? = null,
    val editedCategoryId: Long? = null,
    val editedAccountId: Long? = null,
    val categoryName: String? = null,
    val accountName: String? = null,
    val isConfirmed: Boolean = false,
    val isCancelled: Boolean = false,
    // Balance card snapshot — non-null only for AI balance reply messages
    val walletSnapshot: WalletSnapshot? = null,

    // AI Assistant smart cards
    val recentActivitySummary: RecentActivitySummary? = null,
    val walletDistributionSuggestion: WalletDistributionSuggestion? = null,
    val lowBalanceAlertState: LowBalanceAlertState? = null,
    val transferDraftState: TransferDraftState? = null,
    val selectedAccountDetailsState: SelectedAccountDetailsState? = null,
    val quickImpactPreviewState: QuickImpactPreviewState? = null,

    // Editable draft overlay values for transfer draft
    val editedTransferAmount: Double? = null,
    val editedTransferFromAccountId: Long? = null,
    val editedTransferToAccountId: Long? = null,
    val editedTransferNote: String? = null,
    val transferFromAccountName: String? = null,
    val transferToAccountName: String? = null,
    val isTransferConfirmed: Boolean = false,
    val isTransferCancelled: Boolean = false,

    // Editable low balance limit
    val editedLowBalanceLimit: Double? = null
)
