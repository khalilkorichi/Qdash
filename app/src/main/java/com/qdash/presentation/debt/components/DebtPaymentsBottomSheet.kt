package com.qdash.presentation.debt.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Debt
import com.qdash.domain.model.DebtPayment
import com.qdash.ui.designsystem.components.*
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import com.qdash.ui.theme.ExpenseRed
import com.qdash.ui.theme.IncomeGreen
import com.qdash.ui.theme.TextGray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPaymentsBottomSheet(
    debt: Debt,
    payments: List<DebtPayment>,
    onCancelPaymentClick: (DebtPayment) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    AppBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "سجل دفعات: ${debt.title}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "إجمالي ما تم سداده: ${debt.totalAmount.toInt() - debt.remainingAmount.toInt()} د.ج",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم تسجل أي دفعات سداد لهذا الالتزام بعد.",
                        textAlign = TextAlign.Center,
                        color = TextGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(payments, key = { it.id }) { payment ->
                        AppCard(
                            modifier = Modifier.fillMaxWidth(),
                            variant = CardVariant.SOLID,
                            shape = ShapeTokens.Lg,
                            backgroundColor = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(IncomeGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                        val dateStr = sdf.format(Date(payment.paymentDate))
                                        Text(
                                            text = payment.note ?: "تسديد قسط دين منتظم",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGray
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "- ${payment.amount.toInt()} د.ج",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                    IconButton(
                                        onClick = { onCancelPaymentClick(payment) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "إلغاء الدفعة",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
