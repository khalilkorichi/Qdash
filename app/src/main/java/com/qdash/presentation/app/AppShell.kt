package com.qdash.presentation.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qdash.core.di.AppContainer
import com.qdash.presentation.ViewModelFactory
import com.qdash.presentation.components.radialmenu.AddActionFabContainer
import com.qdash.presentation.navigation.FinTrackNavGraph
import com.qdash.presentation.navigation.LocalNavController
import com.qdash.presentation.navigation.Screen
import com.qdash.domain.model.Transaction
import com.qdash.presentation.navigation.mainBottomNavScreens
import com.qdash.presentation.settings.SettingsViewModel
import com.qdash.presentation.update.UpdatesViewModel
import com.qdash.presentation.update.UpdateUiState
import com.qdash.presentation.update.UpdateBottomBar

/**
 * FinTrackAppShell — isolated composable that owns the NavController and
 * observes currentBackStackEntry. This prevents any recomposition 
 * propagating up to MainActivity's setContent or KdachTheme.
 */
@Composable
internal fun FinTrackAppShell(
    container: AppContainer,
    factory: ViewModelFactory,
    settingsViewModel: SettingsViewModel,
    startDestination: String,
    isFirstLaunch: Boolean
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Shared Updates View Model for global background updating
    val updatesViewModel: UpdatesViewModel = viewModel(factory = factory)
    val updateUiState by updatesViewModel.uiState.collectAsState()

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updatesViewModel.checkForUpdatesThrottled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val aiChatViewModel: com.qdash.presentation.ai.AiChatViewModel = viewModel(factory = factory)
    var isUpdateDismissed by remember(updateUiState) { mutableStateOf(false) }
    val showUpdateBar = remember(updateUiState) {
        updateUiState !is UpdateUiState.Idle &&
        updateUiState !is UpdateUiState.Checking &&
        updateUiState !is UpdateUiState.NoUpdate
    }
    val isUpdateBarVisible = showUpdateBar && !isUpdateDismissed

    // currentRoute is now observed INSIDE the shell, not at root level.
    // Only the shell and its children recompose on navigation changes.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isBottomScreen = remember(currentRoute) {
        mainBottomNavScreens.any { it.route == currentRoute }
    }

    CompositionLocalProvider(
        LocalNavController provides navController
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier.fillMaxSize()
            ) { _ ->
                FinTrackNavGraph(
                    navController = navController,
                    factory = factory,
                    container = container,
                    settingsViewModel = settingsViewModel,
                    startDestination = startDestination,
                    updatesViewModel = updatesViewModel,
                    aiChatViewModel = aiChatViewModel,
                    scope = scope,
                    isFirstLaunch = isFirstLaunch
                )
            }

            // ── Global In-App Update Bottom Bar ──────────────────────────────
            val context = LocalContext.current
            AnimatedVisibility(
                visible = isBottomScreen && isUpdateBarVisible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 92.dp)
            ) {
                UpdateBottomBar(
                    uiState = updateUiState,
                    onUpdateClick = { updatesViewModel.downloadUpdate(it) },
                    onPauseClick = { updatesViewModel.pauseDownload(it) },
                    onResumeClick = { updatesViewModel.resumeDownload(it) },
                    onInstallClick = { info, file ->
                        updatesViewModel.triggerSafetyBackupAndInstall(context, info, file)
                    },
                    onDismiss = { isUpdateDismissed = true },
                    onNavigateToUpdates = { navController.navigate(Screen.Updates.route) }
                )
            }

            // ── Notched Bottom Navigation Bar ────────────────────────────────
            FinTrackBottomNavBar(
                navController = navController,
                currentRoute = currentRoute,
                isVisible = isBottomScreen,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // ── Global FAB Overlay ────────────────────────────────────────────
            AnimatedVisibility(
                visible = isBottomScreen,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                AddActionFabContainer(
                    onAddExpense = {
                        navController.navigate(Screen.AddTransaction.createRoute("EXPENSE"))
                    },
                    onAddIncome = {
                        navController.navigate(Screen.AddTransaction.createRoute("INCOME"))
                    },
                    onTransfer = { navController.navigate(Screen.Transfer.route) },
                    onAddSaving = { navController.navigate(Screen.Savings.route) },
                    onDebtPayment = { navController.navigate(Screen.Debts.route) },
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                )
            }

            // ── Global AI Chat Floating Bubble Overlay (Bottom-Left) ──────────
            val aiChatState by aiChatViewModel.uiState.collectAsState()

            val aiBubbleBottomPadding by animateDpAsState(
                targetValue = if (isBottomScreen && isUpdateBarVisible) 172.dp else 96.dp,
                animationSpec = tween(300),
                label = "ai_bubble_bottom_padding"
            )

            AnimatedVisibility(
                visible = isBottomScreen,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = aiBubbleBottomPadding, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomEnd // Left side in RTL
                ) {
                    if (!aiChatState.isMiniChatOpen) {
                        com.qdash.presentation.ai.components.FloatingAiBubble(
                            onClick = { navController.navigate(Screen.AiVoice.route) }
                        )
                    } else {
                        com.qdash.presentation.ai.components.MiniChatOverlay(
                            messages = aiChatState.messages,
                            suggestions = aiChatState.suggestions,
                            isLoading = aiChatState.isLoading,
                            inputText = aiChatState.inputText,
                            onInputTextChange = { aiChatViewModel.setInputText(it) },
                            onSendMessage = { aiChatViewModel.sendMessage() },
                            onSuggestionClick = { aiChatViewModel.selectSuggestion(it) },
                            onConfirmDraft = { aiChatViewModel.confirmDraft(it) },
                            onCancelDraft = { aiChatViewModel.cancelDraft(it) },
                            onUpdateDraftField = { msgId, field, value ->
                                aiChatViewModel.updateDraftField(msgId, field, value)
                            },
                            onConfirmTransfer = { aiChatViewModel.confirmTransfer(it) },
                            onCancelTransfer = { aiChatViewModel.cancelTransfer(it) },
                            onSaveLowBalanceLimit = { msgId, limit -> aiChatViewModel.saveLowBalanceLimit(msgId, limit) },
                            onUpdateLowBalanceLimitField = { msgId, limit -> aiChatViewModel.updateLowBalanceLimitField(msgId, limit) },
                            onDuplicateTransaction = { aiChatViewModel.duplicateTransaction(it) },
                            onStartEditingTransaction = { msgId, tx -> aiChatViewModel.startEditingTransaction(msgId, tx) },
                            onFullScreenClick = {
                                aiChatViewModel.toggleMiniChat()
                                navController.navigate(Screen.AiVoice.route)
                            },
                            onCloseClick = { aiChatViewModel.toggleMiniChat() },
                            selectedModelId = aiChatState.selectedModelId,
                            models = aiChatState.models,
                            onModelSelected = { aiChatViewModel.selectModel(it) },
                            accounts = aiChatState.accounts,
                            categories = aiChatState.categories,
                            error = aiChatState.error,
                            onRetryClick = { aiChatViewModel.retryLastMessage() },
                            onDismissErrorClick = { aiChatViewModel.dismissError() }
                        )
                    }
                }
            }
        }
    }
}
