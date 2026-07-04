package com.qdash.presentation.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.core.ui.components.EmptyStateView
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.core.ui.components.SubscriptionItem
import com.qdash.ui.theme.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val Primary = MaterialTheme.colorScheme.primary
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = uiState.isRefreshing
    val pullRefreshState = rememberPullToRefreshState()

    var showAddSubDialog by remember { mutableStateOf(false) }

    // Add Subscription variables
    var subName by remember { mutableStateOf("") }
    var subAmount by remember { mutableStateOf("") }
    var subCycle by remember { mutableStateOf("MONTHLY") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var reminderDays by remember { mutableStateOf("3") }

    LaunchedEffect(uiState.accounts) {
        if (uiState.accounts.isNotEmpty()) {
            selectedAccountId = uiState.accounts.first().id
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("subscriptions_screen")
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FinTrackTopBar(
                    title = "إدارة الاشتراكات والتكاليف الثابتة",
                    showBackButton = true,
                    onBackClick = onBack
                )
            }

            // Cost Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Primary, Color(0xFF8B5CF6))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "تكلفة اشتراكاتك الرقمية الشهرية",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${uiState.totalMonthlyCost.toInt()} دج / شهر",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick add button
            item {
                Button(
                    onClick = { showAddSubDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("أضف اشتراكاً جديداً", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (uiState.subscriptions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "لا اشتراكات نشطة مضافة!",
                        description = "أضف خدمات البث، التخزين السحابي أو باقات الإنترنت لمراقبة التكاليف الثابتة.",
                        icon = Icons.Default.Receipt,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                items(uiState.subscriptions, key = { it.id }) { sub ->
                    val accName = uiState.accounts.firstOrNull { it.id == sub.accountId }?.name ?: "غير محدد"
                    SubscriptionItem(
                        subscription = sub,
                        onToggleActive = { active ->
                            viewModel.toggleSubscriptionActive(sub, active)
                        },
                        accountName = accName
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
        } // end PullToRefreshBox
    }

    // Dialog Add Subscription
    if (showAddSubDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showAddSubDialog = false },
                title = { Text("تسجيل اشتراك جديد", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subscription_name_input"),
                            placeholder = { Text("اسم الخدمة (مثال: نيتفليكس العائلي)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = subAmount,
                            onValueChange = { subAmount = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subscription_amount_input"),
                            placeholder = { Text("مبلغ وجدول فوترة الاشتراك (دج)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Pick billing cycle
                        Text("دورة الفوترة والدفع:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("MONTHLY" to "شهري", "YEARLY" to "سنوي", "WEEKLY" to "أسبوعي").forEach { (cycle, label) ->
                                val isSelected = subCycle == cycle
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { subCycle = cycle }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Pick connected account
                        Text("يُخصم تلقائياً من حساب:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uiState.accounts.forEach { acc ->
                                val isSelected = selectedAccountId == acc.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { selectedAccountId = acc.id }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = acc.name,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reminderDays,
                            onValueChange = { reminderDays = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("أرسل تذكير تفتيش قبل (أيام): 3", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = subAmount.toDoubleOrNull() ?: 0.0
                            val rem = reminderDays.toIntOrNull() ?: 3
                            if (subName.isNotBlank() && amt > 0) {
                                viewModel.addSubscription(
                                    name = subName,
                                    amount = amt,
                                    billingCycle = subCycle,
                                    nextBillingDate = System.currentTimeMillis() + (25L * 24 * 60 * 60 * 1000), // Default next renewal in 25 days
                                    accountId = selectedAccountId ?: 1L,
                                    categoryId = 4L, // Index of "فواتير" default Category
                                    reminderDaysBefore = rem
                                )
                                showAddSubDialog = false
                                subName = ""
                                subAmount = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ التكاليف", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubDialog = false }) {
                        Text("إلغاء", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
