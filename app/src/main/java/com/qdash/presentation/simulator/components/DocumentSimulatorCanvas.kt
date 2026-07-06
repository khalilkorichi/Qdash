package com.qdash.presentation.simulator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qdash.domain.usecase.simulator.AmountConversionEngine

@Composable
fun ChequeVisualView(
    chequeAmount: String,
    chequeBeneficiary: String,
    chequePlace: String,
    chequeDate: String,
    chequeCcp: String,
    chequeKey: String,
    onFieldTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chequeBgColor = Color(0xFFFAF6E9) // Traditional postal check cream background
    val chequeBorderColor = Color(0xFF8C9D86)
    val writingColor = Color(0xFF0D1B2A) // Pen ink blue/black

    val formattedAmount = remember(chequeAmount) {
        val amt = chequeAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.formatAmountToPostal(amt) else ""
    }

    val amountInWords = remember(chequeAmount) {
        val amt = chequeAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.convertToArabicWords(amt) else ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(chequeBgColor, RoundedCornerShape(8.dp))
            .border(2.dp, chequeBorderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // Cheque Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "بريد الجزائر",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "ALGERIE POSTE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B5E20)
                )
            }

            // Amount in numbers box
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(38.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, chequeBorderColor)
                    .clickable { onFieldTap("chequeAmount") }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (formattedAmount.isNotEmpty()) {
                    Text(
                        text = "$formattedAmount DA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = writingColor
                    )
                } else {
                    Text(
                        text = "المبلغ بالأرقام دج",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Words line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFieldTap("chequeAmount") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "إدفعوا مقابل هذا الصك : ",
                style = MaterialTheme.typography.labelMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = amountInWords.ifEmpty { "..........................................................." },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (amountInWords.isNotEmpty()) writingColor else Color.Gray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Beneficiary line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFieldTap("chequeBeneficiary") },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "لأمر : ",
                style = MaterialTheme.typography.labelMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = chequeBeneficiary.ifEmpty { "..........................................................................." },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (chequeBeneficiary.isNotEmpty()) writingColor else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Place and Date / Signature Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1.5f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Place
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onFieldTap("chequePlace") }) {
                    Text(text = "في : ", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    Text(
                        text = chequePlace.ifEmpty { "..............." },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (chequePlace.isNotEmpty()) writingColor else Color.Gray
                    )
                }

                // Date
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onFieldTap("chequeDate") }) {
                    Text(text = "بتاريخ (Le) : ", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    Text(
                        text = chequeDate.ifEmpty { "..../..../........" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (chequeDate.isNotEmpty()) writingColor else Color.Gray
                    )
                }
            }

            // Signature area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .border(
                        1.dp,
                        Color.Gray.copy(alpha = 0.5f),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onFieldTap("chequeSignature") },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "الإمضاء (Signature)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom Details (CCP Account & Clé)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onFieldTap("chequeCcp") },
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, chequeBorderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "رقم الحساب: ${chequeCcp.ifEmpty { ".................." }}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (chequeCcp.isNotEmpty()) writingColor else Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, chequeBorderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { onFieldTap("chequeKey") }
            ) {
                Text(
                    text = "المفتاح: ${chequeKey.ifEmpty { ".." }}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (chequeKey.isNotEmpty()) writingColor else Color.Gray
                )
            }
        }
    }
}
