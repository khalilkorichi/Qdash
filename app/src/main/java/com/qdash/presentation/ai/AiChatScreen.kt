package com.qdash.presentation.ai

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.qdash.ui.designsystem.tokens.ColorTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiChatViewModel,
    onBack: () -> Unit,
    onVoiceInput: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    initialMessage: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background == ColorTokens.BackgroundDark
    val backgroundColor = if (isDark) ColorTokens.BackgroundDark else ColorTokens.BackgroundLight

    LaunchedEffect(initialMessage) {
        if (!initialMessage.isNullOrBlank()) {
            viewModel.preFillMessage(initialMessage)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isAiTyping) {
        val targetIndex = uiState.messages.size + if (uiState.isAiTyping) 1 else 0
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex - 1)
        }
    }

    val keyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = keyboardHeight > 0.dp
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && uiState.messages.isNotEmpty()) {
            val targetIndex = uiState.messages.size + if (uiState.isAiTyping) 1 else 0
            if (targetIndex > 0) {
                listState.animateScrollToItem(targetIndex - 1)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ChatHistoryDrawer(
                    sessions = uiState.sessions,
                    currentSession = uiState.currentSessionTitle,
                    onNewSession = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onSelectSession = { session ->
                        viewModel.selectSession(session)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteSession = viewModel::deleteSession,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = backgroundColor,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    AiChatTopBar(
                        isTyping = uiState.isAiTyping,
                        onBack = onBack,
                        onOpenHistory = { scope.launch { drawerState.open() } },
                        onClearChat = viewModel::clearChat
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(paddingValues)
                ) {
                    AiModelSelector(
                        selectedModelId = uiState.selectedModelId,
                        models = uiState.models,
                        availability = uiState.modelAvailability,
                        onRefreshAvailability = viewModel::checkModelAvailability,
                        onModelSelected = viewModel::selectModel
                    )
                    ErrorRetryBanner(
                        error = uiState.error,
                        onRetry = viewModel::retryLastMessage,
                        onDismiss = viewModel::dismissError,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.messages.isEmpty() && uiState.error == null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            AiChatEmptyState(
                                onQuickActionClick = viewModel::preFillMessage,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.messages.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            MessagesLazyColumn(
                                messages = uiState.messages,
                                accounts = uiState.accounts,
                                categories = uiState.categories,
                                isAiTyping = uiState.isAiTyping,
                                listState = listState,
                                onViewDetails = onNavigateToTransactions,
                                onConfirmDraft = viewModel::confirmDraft,
                                onCancelDraft = viewModel::cancelDraft,
                                onUpdateDraftField = viewModel::updateDraftField,
                                onConfirmTransfer = viewModel::confirmTransfer,
                                onCancelTransfer = viewModel::cancelTransfer,
                                onSaveLowBalanceLimit = viewModel::saveLowBalanceLimit,
                                onUpdateLowBalanceLimitField = viewModel::updateLowBalanceLimitField,
                                onDuplicateTransaction = viewModel::duplicateTransaction,
                                onStartEditingTransaction = viewModel::startEditingTransaction
                            )
                        }
                    }
                    ChatInputBar(
                        text = uiState.inputText,
                        onTextChange = viewModel::setInputText,
                        onSend = viewModel::sendMessage,
                        onMicClick = onVoiceInput,
                        modifier = Modifier.imePadding()
                    )
                }
            }
        }
    }
}

internal fun AiChatMessage.hasSmartAiContent(): Boolean {
    return !isUser && (
        draftTransaction != null ||
            walletSnapshot != null ||
            recentActivitySummary != null ||
            walletDistributionSuggestion != null ||
            lowBalanceAlertState != null ||
            transferDraftState != null ||
            selectedAccountDetailsState != null ||
            quickImpactPreviewState != null
        )
}
