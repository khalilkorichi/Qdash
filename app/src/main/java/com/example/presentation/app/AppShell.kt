package com.example.presentation.app

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
import com.example.core.di.AppContainer
import com.example.presentation.ViewModelFactory
import com.example.presentation.components.radialmenu.AddActionFabContainer
import com.example.presentation.navigation.FinTrackNavGraph
import com.example.presentation.navigation.LocalNavController
import com.example.presentation.navigation.Screen
import com.example.presentation.navigation.mainBottomNavScreens
import com.example.presentation.settings.SettingsViewModel
import com.example.presentation.update.UpdatesViewModel
import com.example.presentation.update.UpdateUiState
import com.example.presentation.update.UpdateBottomBar

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
    startDestination: String
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Shared Updates View Model for global background updating
    val updatesViewModel: UpdatesViewModel = viewModel(factory = factory)
    val updateUiState by updatesViewModel.uiState.collectAsState()
    val aiChatViewModel: com.example.presentation.ai.AiChatViewModel = viewModel(factory = factory)
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
                    scope = scope
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
                        com.example.presentation.ai.components.FloatingAiBubble(
                            onClick = { aiChatViewModel.toggleMiniChat() }
                        )
                    } else {
                        com.example.presentation.ai.components.MiniChatOverlay(
                            messages = aiChatState.messages,
                            suggestions = aiChatState.suggestions,
                            isLoading = aiChatState.isLoading,
                            inputText = aiChatState.inputText,
                            onInputTextChange = { aiChatViewModel.setInputText(it) },
                            onSendMessage = { aiChatViewModel.sendMessage() },
                            onSuggestionClick = { aiChatViewModel.selectSuggestion(it) },
                            onConfirmDraft = { aiChatViewModel.confirmDraft(it) },
                            onCancelDraft = { aiChatViewModel.cancelDraft(it) },
                            onFullScreenClick = {
                                aiChatViewModel.toggleMiniChat()
                                navController.navigate(Screen.AiChat.route)
                            },
                            onCloseClick = { aiChatViewModel.toggleMiniChat() },
                            selectedModelId = aiChatState.selectedModelId,
                            models = aiChatState.models,
                            onModelSelected = { aiChatViewModel.selectModel(it) }
                        )
                    }
                }
            }
        }
    }
}
