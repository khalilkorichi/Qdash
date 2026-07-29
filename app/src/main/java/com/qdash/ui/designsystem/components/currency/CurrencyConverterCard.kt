package com.qdash.ui.designsystem.components.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.CURRENCY_ARABIC_NAMES
import com.qdash.domain.model.ExchangeRate
import com.qdash.ui.designsystem.components.AppCard
import com.qdash.ui.designsystem.components.CardVariant
import com.qdash.ui.designsystem.tokens.ColorTokens
import com.qdash.ui.designsystem.tokens.SpacingTokens
import com.qdash.ui.designsystem.tokens.ShapeTokens
import java.text.DecimalFormat

/**
 * Dual-field converter card:
 * - Top field: user input (amount + from currency selector)
 * - Center: SwapCurrencyButton
 * - Bottom field: result (read-only, to currency selector)
 * - AmountInWordsText below the card
 */
@Composable
fun CurrencyConverterCard(
    amount: String,
    fromCurrency: String,
    toCurrency: String,
    result: Double,
    availableCurrencies: List<ExchangeRate>,
    onAmountChange: (String) -> Unit,
    onFromCurrencyChange: (String) -> Unit,
    onToCurrencyChange: (String) -> Unit,
    onSwap: () -> Unit,
    useWesternNumerals: Boolean = true,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US))
    val allCodes = (listOf("DZD") + availableCurrencies.map { it.currencyCode }).distinct()
    val flagMap = buildMap {
        put("DZD", "🇩🇿")
        availableCurrencies.forEach { put(it.currencyCode, it.countryFlagEmoji) }
    }

    val displayAmount = com.qdash.core.utils.FormatterUtils.convertNumerals(amount, useWesternNumerals)
    val rawResult = if (result > 0.0) formatter.format(result) else ""
    val displayResult = com.qdash.core.utils.FormatterUtils.convertNumerals(rawResult, useWesternNumerals)

    AppCard(
        modifier = modifier.fillMaxWidth(),
        variant = CardVariant.OUTLINED,
        shape = ShapeTokens.Xl
    ) {
        Column(
            modifier = Modifier.padding(SpacingTokens.Lg),
            verticalArrangement = Arrangement.spacedBy(SpacingTokens.Md)
        ) {
            // ── From field ───────────────────────────────────────────────────
            CurrencyInputField(
                label = "من",
                value = displayAmount,
                currencyCode = fromCurrency,
                flagEmoji = flagMap[fromCurrency] ?: "🏳",
                allCodes = allCodes,
                flagMap = flagMap,
                onValueChange = onAmountChange,
                onCurrencyChange = onFromCurrencyChange,
                readOnly = false
            )

            // ── Swap button ──────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                SwapCurrencyButton(onSwap = onSwap)
            }

            // ── To field ─────────────────────────────────────────────────────
            CurrencyInputField(
                label = "إلى",
                value = displayResult,
                currencyCode = toCurrency,
                flagEmoji = flagMap[toCurrency] ?: "🏳",
                allCodes = allCodes,
                flagMap = flagMap,
                onValueChange = {},
                onCurrencyChange = onToCurrencyChange,
                readOnly = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyInputField(
    label: String,
    value: String,
    currencyCode: String,
    flagEmoji: String,
    allCodes: List<String>,
    flagMap: Map<String, String>,
    onValueChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ShapeTokens.Md
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Currency picker
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.menuAnchor(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = flagEmoji, fontSize = 20.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = currencyCode,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .widthIn(min = 220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    allCodes.forEach { code ->
                        val isSelected = code == currencyCode
                        val name = CURRENCY_ARABIC_NAMES[code] ?: ""

                        DropdownMenuItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(ShapeTokens.Md)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else Color.Transparent
                                ),
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = flagMap[code] ?: "🏳", fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = code,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                                ),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (name.isNotEmpty()) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                                )
                                            }
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onCurrencyChange(code)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Amount input
            BasicTextField(
                value = value,
                onValueChange = if (!readOnly) onValueChange else { _ -> },
                readOnly = readOnly,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    color = if (readOnly)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    inner()
                }
            )
        }
    }
}

// Alias to avoid importing BasicTextField everywhere
@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean = false,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        modifier = modifier,
        textStyle = textStyle,
        singleLine = singleLine,
        decorationBox = decorationBox,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        )
    )
}
