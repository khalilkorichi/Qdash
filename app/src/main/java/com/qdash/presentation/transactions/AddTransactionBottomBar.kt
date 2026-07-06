package com.qdash.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.TransactionType

@Composable
fun AddTransactionBottomBar(
    isKeypadExpanded: Boolean,
    onKeypadExpandedChange: (Boolean) -> Unit,
    displayAmount: String,
    rawAmountValue: TextFieldValue,
    onAmountValueChange: (TextFieldValue) -> Unit,
    type: TransactionType,
    transactionId: Long?,
    rawAmount: String,
    canSaveTransaction: Boolean,
    typeAccentColor: Color,
    onSaveClick: () -> Unit,
    onShowSaveTemplateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .navigationBarsPadding()
    ) {
        // Keypad toggle handle
        KeypadToggleBar(
            isExpanded = isKeypadExpanded,
            currentAmount = displayAmount,
            onToggle = { onKeypadExpandedChange(!isKeypadExpanded) }
        )

        // Collapsible numpad
        AnimatedVisibility(
            visible = isKeypadExpanded,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn(animationSpec = tween(150)),
            exit = shrinkVertically(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
            ) + fadeOut(animationSpec = tween(100))
        ) {
            NumPad(
                onKeyPress = { key ->
                    onAmountValueChange(handleNumpadKey(rawAmountValue, key))
                }
            )
        }

        // Save transaction / save as template action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (transactionId == null) {
                OutlinedButton(
                    onClick = onShowSaveTemplateDialog,
                    enabled = com.qdash.core.utils.CalculatorParser.evaluate(rawAmount) > 0.0,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, typeAccentColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = typeAccentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "حفظ كقالب",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Button(
                onClick = onSaveClick,
                enabled = canSaveTransaction,
                modifier = Modifier
                    .weight(if (transactionId == null) 1.5f else 1f)
                    .testTag("save_transaction_button"),
                colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (transactionId != null) "حفظ التعديلات" else "تسجيل العملية",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
