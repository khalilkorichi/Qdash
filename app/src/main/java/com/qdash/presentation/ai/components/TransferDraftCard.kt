package com.qdash.presentation.ai.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.Account
import com.qdash.domain.model.TransferDraftState
import com.qdash.presentation.ai.AiChatMessage
import com.qdash.presentation.ai.DraftField
import com.qdash.core.utils.FormatterUtils
import com.qdash.ui.theme.TextGray

@Composable
fun TransferDraftCard(
    draft: TransferDraftState,
    message: AiChatMessage,
    accounts: List<Account>,
    onUpdateField: (DraftField, Any) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAmount = message.editedTransferAmount ?: draft.amount
    val currentFromId = message.editedTransferFromAccountId ?: draft.fromAccountId
    val currentToId = message.editedTransferToAccountId ?: draft.toAccountId
    val currentNote = message.editedTransferNote ?: (draft.note ?: "")
    
    var amountText by remember(message.id, currentAmount) {
        mutableStateOf(if (currentAmount == currentAmount.toLong().toDouble()) currentAmount.toLong().toString() else currentAmount.toString())
    }
    
    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }
    
    val isInteractable = !message.isTransferConfirmed && !message.isTransferCancelled
    
    Card(
        modifier = modifier
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFFE9E9E6)), shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "🔵 مسودة تحويل مقترحة",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("المبلغ", fontSize = 10.sp, color = TextGray)
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        amountText = raw.filter { it.isDigit() || it == '.' }
                        val parsed = amountText.toDoubleOrNull()
                        if (parsed != null) onUpdateField(DraftField.TRANSFER_AMOUNT, parsed)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    suffix = { Text("دج", fontSize = 11.sp, color = TextGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            } else {
                Text(FormatterUtils.formatCurrency(currentAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text("من حساب", fontSize = 10.sp, color = TextGray)
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable && accounts.isNotEmpty()) {
                val selectedName = accounts.find { it.id == currentFromId }?.name ?: "غير محدد"
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showFromDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = showFromDropdown, onDismissRequest = { showFromDropdown = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, fontSize = 12.sp) },
                                onClick = {
                                    onUpdateField(DraftField.TRANSFER_FROM_ACCOUNT_ID, acc.id)
                                    showFromDropdown = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(accounts.find { it.id == currentFromId }?.name ?: "غير محدد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text("إلى حساب", fontSize = 10.sp, color = TextGray)
            Spacer(modifier = Modifier.height(2.dp))
            if (isInteractable && accounts.isNotEmpty()) {
                val selectedName = accounts.find { it.id == currentToId }?.name ?: "غير محدد"
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showToDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, null, tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = showToDropdown, onDismissRequest = { showToDropdown = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name, fontSize = 12.sp) },
                                onClick = {
                                    onUpdateField(DraftField.TRANSFER_TO_ACCOUNT_ID, acc.id)
                                    showToDropdown = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(accounts.find { it.id == currentToId }?.name ?: "غير محدد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            
            if (isInteractable) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("ملاحظة", fontSize = 10.sp, color = TextGray)
                Spacer(modifier = Modifier.height(2.dp))
                var noteText by remember(message.id, currentNote) { mutableStateOf(currentNote) }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { raw ->
                        noteText = raw
                        onUpdateField(DraftField.TRANSFER_NOTE, raw)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    placeholder = { Text("أضف ملاحظة...", fontSize = 11.sp, color = TextGray) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            when {
                message.isTransferConfirmed -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22C55E).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تم التحويل بنجاح ✅", color = Color(0xFF22C55E), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
                    }
                }
                message.isTransferCancelled -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("تم إلغاء التحويل ❌", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("إلغاء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("تأكيد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
