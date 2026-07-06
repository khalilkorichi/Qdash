package com.qdash.presentation.ai

import androidx.lifecycle.viewModelScope
import com.qdash.domain.model.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Extension methods for AiChatViewModel to handle draft edits, transfers, and balance limits.
 * Extracted to keep AiChatViewModel.kt under the SIZE-001 500-line threshold.
 */

fun AiChatViewModel.confirmDraft(messageId: String) {
    viewModelScope.launch {
        val targetMsg = _uiState.value.messages.find { it.id == messageId && it.draftTransaction != null }
        if (targetMsg != null && targetMsg.draftTransaction != null) {
            val baseDraft = targetMsg.draftTransaction
            val finalTransaction = baseDraft.copy(
                amount = targetMsg.editedAmount ?: baseDraft.amount,
                type = targetMsg.editedType ?: baseDraft.type,
                note = targetMsg.editedNote ?: baseDraft.note,
                categoryId = targetMsg.editedCategoryId ?: baseDraft.categoryId,
                accountId = targetMsg.editedAccountId ?: baseDraft.accountId
            )
            transactionRepository.insertTransaction(finalTransaction)
            _uiState.update { state ->
                state.copy(messages = state.messages.map { msg ->
                    if (msg.id == messageId) msg.copy(isConfirmed = true) else msg
                })
            }
            generateProactiveInsights()
        }
    }
}

fun AiChatViewModel.cancelDraft(messageId: String) {
    val messages = _uiState.value.messages.map { msg ->
        if (msg.id == messageId) {
            msg.copy(isCancelled = true)
        } else {
            msg
        }
    }
    _uiState.update { it.copy(messages = messages) }
}

fun AiChatViewModel.updateDraftField(messageId: String, field: DraftField, value: Any) {
    _uiState.update { state ->
        state.copy(messages = state.messages.map { msg ->
            if (msg.id != messageId) return@map msg
            when (field) {
                DraftField.AMOUNT -> msg.copy(editedAmount = (value as? Double))
                DraftField.TYPE -> msg.copy(editedType = (value as? TransactionType))
                DraftField.NOTE -> msg.copy(editedNote = (value as? String))
                DraftField.CATEGORY_ID -> {
                    val catId = (value as? Long)
                    val catName = _uiState.value.categories.find { it.id == catId }?.name
                    msg.copy(editedCategoryId = catId, categoryName = catName ?: msg.categoryName)
                }
                DraftField.ACCOUNT_ID -> {
                    val accId = (value as? Long)
                    val accName = _uiState.value.accounts.find { it.id == accId }?.name
                    msg.copy(editedAccountId = accId, accountName = accName ?: msg.accountName)
                }
                DraftField.TRANSFER_AMOUNT -> msg.copy(editedTransferAmount = (value as? Double))
                DraftField.TRANSFER_FROM_ACCOUNT_ID -> {
                    val accId = (value as? Long)
                    val name = _uiState.value.accounts.find { it.id == accId }?.name
                    msg.copy(editedTransferFromAccountId = accId, transferFromAccountName = name ?: msg.transferFromAccountName)
                }
                DraftField.TRANSFER_TO_ACCOUNT_ID -> {
                    val accId = (value as? Long)
                    val name = _uiState.value.accounts.find { it.id == accId }?.name
                    msg.copy(editedTransferToAccountId = accId, transferToAccountName = name ?: msg.transferToAccountName)
                }
                DraftField.TRANSFER_NOTE -> msg.copy(editedTransferNote = (value as? String))
                DraftField.LOW_BALANCE_LIMIT -> msg.copy(editedLowBalanceLimit = (value as? Double))
            }
        })
    }
}

fun AiChatViewModel.confirmTransfer(messageId: String) {
    viewModelScope.launch {
        val targetMsg = _uiState.value.messages.find { it.id == messageId && it.transferDraftState != null }
        if (targetMsg != null && targetMsg.transferDraftState != null) {
            val draft = targetMsg.transferDraftState
            val finalAmount = targetMsg.editedTransferAmount ?: draft.amount
            val finalFrom = targetMsg.editedTransferFromAccountId ?: draft.fromAccountId
            val finalTo = targetMsg.editedTransferToAccountId ?: draft.toAccountId
            val finalNote = targetMsg.editedTransferNote ?: draft.note
            
            val req = TransferRequest(
                fromAccountId = finalFrom,
                toAccountId = finalTo,
                amount = finalAmount,
                note = finalNote,
                date = System.currentTimeMillis()
            )
            val success = transferBetweenAccountsUseCase(req)
            if (success) {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(isTransferConfirmed = true) else msg
                    })
                }
                generateProactiveInsights()
            }
        }
    }
}

fun AiChatViewModel.cancelTransfer(messageId: String) {
    _uiState.update { state ->
        state.copy(messages = state.messages.map { msg ->
            if (msg.id == messageId) msg.copy(isTransferCancelled = true) else msg
        })
    }
}

fun AiChatViewModel.saveLowBalanceLimit(messageId: String, limit: Double) {
    viewModelScope.launch {
        preferencesManager.lowBalanceLimit = limit
        val newState = evaluateLowBalanceAlertsUseCase(limit)
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(
                        lowBalanceAlertState = newState,
                        editedLowBalanceLimit = null
                    )
                } else msg
            })
        }
    }
}

fun AiChatViewModel.updateLowBalanceLimitField(messageId: String, limit: Double) {
    _uiState.update { state ->
        state.copy(messages = state.messages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(editedLowBalanceLimit = limit)
            } else msg
        })
    }
}

fun AiChatViewModel.duplicateTransaction(transaction: Transaction) {
    viewModelScope.launch {
        val copy = transaction.copy(id = 0, date = System.currentTimeMillis())
        transactionRepository.insertTransaction(copy)
        generateProactiveInsights()
    }
}

fun AiChatViewModel.startEditingTransaction(messageId: String, transaction: Transaction) {
    _uiState.update { state ->
        state.copy(messages = state.messages.map { msg ->
            if (msg.id == messageId) {
                msg.copy(
                    draftTransaction = transaction,
                    editedAmount = transaction.amount,
                    editedType = transaction.type,
                    editedNote = transaction.note,
                    editedCategoryId = transaction.categoryId,
                    editedAccountId = transaction.accountId,
                    isConfirmed = false,
                    isCancelled = false
                )
            } else msg
        })
    }
}
