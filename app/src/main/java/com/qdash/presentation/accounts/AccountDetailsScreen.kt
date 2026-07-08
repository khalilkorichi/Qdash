package com.qdash.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qdash.core.ui.components.UnifiedScreenHeader
import com.qdash.ui.theme.Primary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    viewModel: AccountDetailsViewModel,
    onBack: () -> Unit,
    onEditAccount: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddAmanaSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, "حسناً", duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) { // Amana tab
                ExtendedFloatingActionButton(
                    onClick = { showAddAmanaSheet = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة أمانة") }
                )
            } else if (uiState.account != null) {
                FloatingActionButton(
                    onClick = { onEditAccount(uiState.account!!.id) },
                    containerColor = Primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الحساب")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            UnifiedScreenHeader(
                title = uiState.account?.name ?: "تفاصيل الحساب",
                subtitle = "تفاصيل الرصيد والمعاملات والأمانات",
                showBackButton = true,
                onBackClick = onBack
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                return@Scaffold
            }

            val account = uiState.account ?: return@Scaffold

            AccountBalanceCard(
                balance = account.balance,
                isActive = account.isActive,
                totalAmana = uiState.totalAmanaForAccount,
                realBalance = uiState.realBalance
            )

            // --- Tabs ---
            val tabs = listOf("المعاملات", "الأمانات")
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    )
                }
            }

            // --- Tab content ---
            when (selectedTab) {
                0 -> TransactionsTab(transactions = uiState.transactions, currentAccountId = uiState.account?.id)
                1 -> AmanasTab(
                    amanas = uiState.amanas,
                    onDeleteAmana = { viewModel.deleteAmana(it) }
                )
            }
        }
    }

    // --- Add Amana Bottom Sheet ---
    if (showAddAmanaSheet) {
        AddAmanaBottomSheet(
            isSaving = uiState.isSavingAmana,
            onDismiss = { showAddAmanaSheet = false },
            onConfirm = { name, ownerName, amount, notes ->
                viewModel.addAmana(name, ownerName, amount, notes)
                showAddAmanaSheet = false
            }
        )
    }
}

