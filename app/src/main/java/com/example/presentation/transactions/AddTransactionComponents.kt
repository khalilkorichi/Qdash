package com.example.presentation.transactions

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import com.example.core.ui.components.CategoryChip
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.core.utils.FormatterUtils
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService

// ─────────────────────────────────────────────────────────
//  Helper functions for currency in words (Algerian DZ)
// ─────────────────────────────────────────────────────────

private fun numberToArabicWordsDZ(amount: Double): Pair<String, String> {
    val totalCentimes = Math.round(amount * 100.0)
    if (totalCentimes <= 0) return Pair("", "")
    
    val dinars = totalCentimes / 100
    val cents = totalCentimes % 100
    
    // Dinar representation
    val dinarText = when {
        dinars > 0 && cents > 0 -> {
            val dinarPart = when (dinars) {
                1L -> "دينار واحد"
                2L -> "ديناران"
                in 3..10 -> "${convertWholeNumber(dinars)} دنانير"
                else -> "${convertWholeNumber(dinars)} دينار"
            }
            val centsPart = when (cents) {
                1L -> "سنتيم واحد"
                2L -> "سنتيمان"
                in 3..10 -> "${convertWholeNumber(cents)} سنتيمات"
                else -> "${convertWholeNumber(cents)} سنتيم"
            }
            "$dinarPart و$centsPart"
        }
        dinars > 0 -> {
            when (dinars) {
                1L -> "دينار واحد"
                2L -> "ديناران"
                in 3..10 -> "${convertWholeNumber(dinars)} دنانير"
                else -> "${convertWholeNumber(dinars)} دينار"
            }
        }
        cents > 0 -> {
            when (cents) {
                1L -> "سنتيم واحد"
                2L -> "سنتيمان"
                in 3..10 -> "${convertWholeNumber(cents)} سنتيمات"
                else -> "${convertWholeNumber(cents)} سنتيم"
            }
        }
        else -> ""
    }
    
    // Centime representation (Algerian colloquial style)
    val centimeText = when (totalCentimes) {
        1L -> "سنتيم واحد"
        2L -> "سنتيمان"
        in 3..10 -> "${convertWholeNumber(totalCentimes)} سنتيمات"
        else -> "${convertWholeNumber(totalCentimes)} سنتيم"
    }
    
    return Pair(dinarText, centimeText)
}

private fun convertWholeNumber(n: Long): String {
    if (n == 0L) return "صفر"
    if (n == 1L) return "واحد"
    if (n == 2L) return "اثنان"

    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    val parts = mutableListOf<String>()
    var remaining = n

    // Billions
    if (remaining >= 1_000_000_000) {
        val b = remaining / 1_000_000_000
        remaining %= 1_000_000_000
        parts.add(when {
            b == 1L -> "مليار"
            b == 2L -> "ملياران"
            b in 3..10 -> "${convertSmall(b)} مليارات"
            else -> "${convertSmall(b)} مليار"
        })
    }

    // Millions
    if (remaining >= 1_000_000) {
        val m = remaining / 1_000_000
        remaining %= 1_000_000
        parts.add(when {
            m == 1L -> "مليون"
            m == 2L -> "مليونان"
            m in 3..10 -> "${convertSmall(m)} ملايين"
            else -> "${convertSmall(m)} مليون"
        })
    }

    // Thousands
    if (remaining >= 1000) {
        val t = remaining / 1000
        remaining %= 1000
        parts.add(when {
            t == 1L -> "ألف"
            t == 2L -> "ألفان"
            t in 3..10 -> "${convertSmall(t)} آلاف"
            else -> "${convertSmall(t)} ألف"
        })
    }

    // Hundreds
    if (remaining >= 100) {
        val h = (remaining / 100).toInt()
        remaining %= 100
        parts.add(hundreds[h])
    }

    // Tens and ones
    if (remaining > 0) {
        if (remaining in 10..19) {
            parts.add(teens[(remaining - 10).toInt()])
        } else {
            val o = (remaining % 10).toInt()
            val t = (remaining / 10).toInt()
            if (o > 0 && t > 0) {
                parts.add("${ones[o]} و${tens[t]}")
            } else if (t > 0) {
                parts.add(tens[t])
            } else if (o > 0) {
                parts.add(ones[o])
            }
        }
    }

    return parts.joinToString(" و")
}

private fun convertSmall(n: Long): String {
    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    val tens = arrayOf("", "", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val hundreds = arrayOf("", "مائة", "مئتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")
    
    if (n == 0L) return "صفر"
    val parts = mutableListOf<String>()
    var r = n
    if (r >= 100) { parts.add(hundreds[(r/100).toInt()]); r %= 100 }
    if (r in 10..19) { parts.add(teens[(r-10).toInt()]); r = 0 }
    if (r > 0) {
        val o = (r % 10).toInt()
        val t = (r / 10).toInt()
        if (o > 0 && t > 0) parts.add("${ones[o]} و${tens[t]}")
        else if (t > 0) parts.add(tens[t])
        else if (o > 0) parts.add(ones[o])
    }
    return parts.joinToString(" و")
}

// ─────────────────────────────────────────────────────────
//  TypeSelectorBar
// ─────────────────────────────────────────────────────────

@Composable
fun TypeSelectorBar(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val types = listOf(
        TransactionType.EXPENSE  to "مصروف",
        TransactionType.INCOME   to "دخل",
        TransactionType.TRANSFER to "تحويل"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        types.forEach { (t, label) ->
            val isSelected = selected == t
            val activeColor = when (t) {
                TransactionType.EXPENSE  -> ExpenseRed
                TransactionType.INCOME   -> IncomeGreen
                TransactionType.TRANSFER -> TransferBlue
            }
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else Color.Transparent,
                animationSpec = tween(200),
                label = "type_bg_$t"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onSelect(t) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  AmountDisplayCard
// ─────────────────────────────────────────────────────────

@Composable
fun AmountDisplayCard(
    rawAmountValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    displayAmount: String,
    livePreviewAmount: String,
    accentColor: Color,
    showAmountWords: Boolean = true,
    onTap: () -> Unit
) {
    val numericAmount = remember(displayAmount, livePreviewAmount) {
        val rawStr = if (livePreviewAmount.isNotEmpty()) livePreviewAmount else displayAmount
        val normalizedStr = rawStr.map { char ->
            when (char) {
                '٠' -> '0'
                '١' -> '1'
                '٢' -> '2'
                '٣' -> '3'
                '٤' -> '4'
                '٥' -> '5'
                '٦' -> '6'
                '٧' -> '7'
                '٨' -> '8'
                '٩' -> '9'
                else -> char
            }
        }.joinToString("")
        val symbols = java.text.DecimalFormatSymbols.getInstance()
        val groupingSep = symbols.groupingSeparator.toString()
        val decimalSep = symbols.decimalSeparator.toString()
        val cleaned = normalizedStr
            .replace(groupingSep, "")
            .replace(decimalSep, ".")
            .replace("٬", "")
            .replace(",", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace("\\s".toRegex(), "")
        cleaned.toDoubleOrNull() ?: 0.0
    }
    val amountWords = remember(numericAmount) {
        numberToArabicWordsDZ(numericAmount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = accentColor.copy(alpha = 0.15f),
                spotColor = accentColor.copy(alpha = 0.25f)
            )
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "المبلغ",
                style = MaterialTheme.typography.labelMedium,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                val focusRequester = remember { FocusRequester() }
                CompositionLocalProvider(LocalTextInputService provides null) {
                    BasicTextField(
                        value = rawAmountValue,
                        onValueChange = onValueChange,
                        readOnly = false,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.displayLarge.copy(
                            fontSize = if (rawAmountValue.text.length > 12) 28.sp else 38.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            localeList = if (com.example.core.utils.FormatterUtils.useWesternNumerals) {
                                androidx.compose.ui.text.intl.LocaleList("en")
                            } else {
                                androidx.compose.ui.text.intl.LocaleList("ar")
                            }
                        ),
                        cursorBrush = SolidColor(accentColor),
                        visualTransformation = FormulaThousandsSeparatorTransformation(),
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    onValueChange(
                                        rawAmountValue.copy(
                                            selection = TextRange(0, rawAmountValue.text.length)
                                        )
                                    )
                                }
                            }
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    focusRequester.requestFocus()
                                    onTap()
                                }
                            }
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "دج",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            if (showAmountWords && numericAmount > 0 && amountWords.first.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                // Dinar words
                Text(
                    text = amountWords.first,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = accentColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                // Centime equivalent (as Algerians speak daily)
                if (amountWords.second.isNotEmpty()) {
                    Text(
                        text = "بالسنتيم: ${amountWords.second}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextGray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
            if (livePreviewAmount.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "= ${com.example.core.utils.FormatterUtils.convertNumerals(livePreviewAmount)} دج",
                    style = MaterialTheme.typography.titleMedium.copy(
                        localeList = if (com.example.core.utils.FormatterUtils.useWesternNumerals) {
                            androidx.compose.ui.text.intl.LocaleList("en")
                        } else {
                            androidx.compose.ui.text.intl.LocaleList("ar")
                        }
                    ),
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  AccountPickerRow
// ─────────────────────────────────────────────────────────

@Composable
fun AccountPickerRow(
    accounts: List<com.example.domain.model.Account>,
    selectedId: Long?,
    accentColor: Color,
    disabledId: Long?,
    expectedBalances: Map<Long, Double> = emptyMap(),
    parsedAmount: Double = 0.0,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        accounts.forEach { acc ->
            val isSelected = selectedId == acc.id
            val isDisabled = acc.id == disabledId
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected  -> accentColor.copy(alpha = 0.18f)
                            isDisabled  -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            else        -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable(enabled = !isDisabled) { onSelect(acc.id) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = acc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
                        isSelected -> accentColor
                        else       -> MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = FormatterUtils.formatCurrency(acc.balance),
                    style = MaterialTheme.typography.labelSmall.copy(
                        localeList = if (com.example.core.utils.FormatterUtils.useWesternNumerals) {
                            androidx.compose.ui.text.intl.LocaleList("en")
                        } else {
                            androidx.compose.ui.text.intl.LocaleList("ar")
                        }
                    ),
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        isSelected -> accentColor.copy(alpha = 0.8f)
                        else       -> TextGray
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
                // Live preview of expected balance
                val expectedBalance = expectedBalances[acc.id] ?: acc.balance
                val showExpected = isSelected && parsedAmount > 0.0 && expectedBalance != acc.balance
                if (showExpected) {
                    val isPlus = expectedBalance > acc.balance
                    Text(
                        text = "➔ " + FormatterUtils.formatCurrency(expectedBalance),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            localeList = if (com.example.core.utils.FormatterUtils.useWesternNumerals) {
                                androidx.compose.ui.text.intl.LocaleList("en")
                            } else {
                                androidx.compose.ui.text.intl.LocaleList("ar")
                            }
                        ),
                        fontWeight = FontWeight.Bold,
                        color = if (isPlus) IncomeGreen else ExpenseRed,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  SectionLabel
// ─────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

// ─────────────────────────────────────────────────────────
//  KeypadToggleBar
// ─────────────────────────────────────────────────────────

@Composable
fun KeypadToggleBar(
    isExpanded: Boolean,
    currentAmount: String,
    onToggle: () -> Unit
) {
    val Primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown
                              else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isExpanded) "إخفاء لوحة المفاتيح" else "إظهار لوحة المفاتيح",
                style = MaterialTheme.typography.labelMedium,
                color = Primary
            )
        }
        Text(
            text = "${com.example.core.utils.FormatterUtils.convertNumerals(currentAmount)} دج",
            style = MaterialTheme.typography.labelLarge.copy(
                localeList = if (com.example.core.utils.FormatterUtils.useWesternNumerals) {
                    androidx.compose.ui.text.intl.LocaleList("en")
                } else {
                    androidx.compose.ui.text.intl.LocaleList("ar")
                }
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────────────────
//  NumPad
// ─────────────────────────────────────────────────────────

@Composable
fun NumPad(onKeyPress: (String) -> Unit) {
    val rows = listOf(
        listOf("7", "8", "9", "÷"),
        listOf("4", "5", "6", "×"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "⌫", "+")
    )
    val bottomRow = listOf("C", "00", "=")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isDelete = key == "⌫"
                        val isOperator = key == "+" || key == "-" || key == "×" || key == "÷"
                        val buttonBg = when {
                            isDelete -> ExpenseRed.copy(alpha = 0.12f)
                            isOperator -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        }
                        val textColor = when {
                            isDelete -> ExpenseRed
                            isOperator -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(buttonBg)
                                .clickable { onKeyPress(key) }
                                .testTag("numpad_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDelete) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "حذف",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = com.example.core.utils.FormatterUtils.convertNumerals(key),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bottomRow.forEach { key ->
                    val isEquals = key == "="
                    val isClear = key == "C"
                    val buttonBg = when {
                        isEquals -> MaterialTheme.colorScheme.primary
                        isClear -> ExpenseRed.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    }
                    val textColor = when {
                        isEquals -> Color.White
                        isClear -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    val weight = if (isEquals) 2f else 1f

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(buttonBg)
                            .clickable { onKeyPress(key) }
                            .testTag("numpad_key_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = com.example.core.utils.FormatterUtils.convertNumerals(key),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
//  SaveTransactionButton
// ─────────────────────────────────────────────────────────

@Composable
fun SaveTransactionButton(
    accentColor: Color,
    isEnabled: Boolean,
    isEditMode: Boolean = false,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Button(
            onClick = onClick,
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("save_transaction_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.35f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEditMode) "حفظ التعديلات" else "تسجيل العملية المالية",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  CategoryIconView
// ─────────────────────────────────────────────────────────

@Composable
fun CategoryIconView(
    iconStr: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isMaterialIcon = remember(iconStr) {
        iconStr.matches(Regex("^[a-zA-Z_]+$"))
    }

    if (isMaterialIcon) {
        val vectorIcon = when (iconStr) {
            "person" -> Icons.Default.Person
            "groups" -> Icons.Default.Groups
            "home" -> Icons.Default.Home
            "restaurant" -> Icons.Default.Restaurant
            "directions_car" -> Icons.Default.DirectionsCar
            "receipt_long" -> Icons.Default.ReceiptLong
            "shopping_bag" -> Icons.Default.ShoppingBag
            "medical_services" -> Icons.Default.MedicalServices
            "school" -> Icons.Default.School
            "sports_esports" -> Icons.Default.SportsEsports
            "work" -> Icons.Default.Work
            "redeem" -> Icons.Default.Redeem
            "storefront" -> Icons.Default.Storefront
            "schedule" -> Icons.Default.Schedule
            "monetization_on" -> Icons.Default.MonetizationOn
            "savings" -> Icons.Default.Savings
            "payments" -> Icons.Default.Payments
            "account_balance" -> Icons.Default.AccountBalance
            "trending_up" -> Icons.Default.TrendingUp
            "card_giftcard" -> Icons.Default.CardGiftcard
            "shopping_cart" -> Icons.Default.ShoppingCart
            "local_gas_station" -> Icons.Default.LocalGasStation
            "directions_bus" -> Icons.Default.DirectionsBus
            "local_taxi" -> Icons.Default.LocalTaxi
            "flight" -> Icons.Default.Flight
            "checkroom" -> Icons.Default.Checkroom
            "spa" -> Icons.Default.Spa
            "fitness_center" -> Icons.Default.FitnessCenter
            "live_tv" -> Icons.Default.LiveTv
            "event" -> Icons.Default.Event
            "phone_android" -> Icons.Default.PhoneAndroid
            "wifi" -> Icons.Default.Wifi
            "bolt" -> Icons.Default.Bolt
            "water_drop" -> Icons.Default.WaterDrop
            "chair" -> Icons.Default.Chair
            "coffee" -> Icons.Default.Coffee
            "child_care" -> Icons.Default.ChildCare
            "pets" -> Icons.Default.Pets
            "favorite" -> Icons.Default.Favorite
            "star" -> Icons.Default.Star
            "attach_money" -> Icons.Default.AttachMoney
            "receipt" -> Icons.Default.Receipt
            "build" -> Icons.Default.Build
            "local_hospital" -> Icons.Default.LocalHospital
            "mosque" -> Icons.Default.Mosque
            "volunteer_activism" -> Icons.Default.VolunteerActivism
            else -> Icons.Default.Category
        }
        Icon(
            imageVector = vectorIcon,
            contentDescription = null,
            tint = color,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconStr,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

sealed interface DropdownCategoryItem {
    data class MainCategory(val category: Category, val isSelected: Boolean) : DropdownCategoryItem
    data class SubCategory(val category: Category, val parentCategory: Category, val isSelected: Boolean) : DropdownCategoryItem
}

// ─────────────────────────────────────────────────────────
//  CategoryDropdownSelector
// ─────────────────────────────────────────────────────────

@Composable
fun CategoryDropdownSelector(
    categories: List<Category>,
    transactions: List<Transaction>,
    type: TransactionType,
    selectedCategoryId: Long?,
    subcategoryId: Long?,
    typeAccentColor: Color,
    smartSortEnabled: Boolean,
    onToggleSmartSort: () -> Unit,
    onCategorySelected: (Long?, Long?) -> Unit,
    onAddMainCategory: () -> Unit,
    onAddSubCategory: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val categoryFrequencies = remember(transactions) {
        transactions
            .groupBy { it.categoryId }
            .mapValues { it.value.size }
    }

    val displayList = remember(categories, type, selectedCategoryId, subcategoryId, smartSortEnabled, categoryFrequencies) {
        val filtered = categories.filter { it.type.name == type.name }
        val parentCategories = filtered.filter { it.parentId == null }

        val sortedParents = if (smartSortEnabled) {
            parentCategories.sortedByDescending { categoryFrequencies[it.id] ?: 0 }
        } else {
            parentCategories.sortedBy { it.sortOrder }
        }

        val items = mutableListOf<DropdownCategoryItem>()
        sortedParents.forEach { parent ->
            val isParentSelected = selectedCategoryId == parent.id
            items.add(DropdownCategoryItem.MainCategory(parent, isParentSelected))

            val childCategories = filtered.filter { it.parentId == parent.id }
            val sortedChildren = if (smartSortEnabled) {
                childCategories.sortedByDescending { categoryFrequencies[it.id] ?: 0 }
            } else {
                childCategories.sortedBy { it.sortOrder }
            }

            sortedChildren.forEach { child ->
                val isChildSelected = subcategoryId == child.id
                items.add(DropdownCategoryItem.SubCategory(child, parent, isChildSelected))
            }
        }
        items
    }

    val selectedLabel = remember(selectedCategoryId, subcategoryId, categories) {
        val mainCat = categories.find { it.id == selectedCategoryId }
        val subCat = categories.find { it.id == subcategoryId }
        when {
            subCat != null && mainCat != null -> "${mainCat.name} ➔ ${subCat.name}"
            mainCat != null -> mainCat.name
            else -> "اختيار فئة المعاملة"
        }
    }

    val selectedColorHex = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }?.color ?: "#9CA3AF"
    }
    val selectedColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (e: Exception) {
            typeAccentColor
        }
    }

    val selectedIcon = remember(selectedCategoryId, subcategoryId, categories) {
        val subCat = categories.find { it.id == subcategoryId }
        val mainCat = categories.find { it.id == selectedCategoryId }
        subCat?.icon ?: mainCat?.icon ?: "📁"
    }

    Box {
        OutlinedCard(
            onClick = { showDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(selectedColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIconView(iconStr = selectedIcon, color = selectedColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = selectedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اختر فئة العملية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ترتيب ذكي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = smartSortEnabled,
                                onCheckedChange = { _ -> onToggleSmartSort() },
                                modifier = Modifier.scale(0.7f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = typeAccentColor,
                                    checkedTrackColor = typeAccentColor.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxHeight(0.65f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    showDialog = false
                                    onAddMainCategory()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فئة رئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    showDialog = false
                                    onAddSubCategory()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = typeAccentColor.copy(alpha = 0.85f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فئة فرعية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(displayList) { item ->
                                when (item) {
                                    is DropdownCategoryItem.MainCategory -> {
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(item.category.color))
                                        } catch (e: Exception) {
                                            typeAccentColor
                                        }
                                        Surface(
                                            onClick = {
                                                onCategorySelected(item.category.id, null)
                                                showDialog = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.isSelected) catColor.copy(alpha = 0.12f) else Color.Transparent,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(catColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CategoryIconView(iconStr = item.category.icon, color = catColor, modifier = Modifier.size(14.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = item.category.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (item.isSelected) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                    is DropdownCategoryItem.SubCategory -> {
                                        val parentColor = try {
                                            Color(android.graphics.Color.parseColor(item.parentCategory.color))
                                        } catch (e: Exception) {
                                            typeAccentColor
                                        }
                                        Surface(
                                            onClick = {
                                                onCategorySelected(item.parentCategory.id, item.category.id)
                                                showDialog = false
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (item.isSelected) parentColor.copy(alpha = 0.12f) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 24.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "└ ",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(parentColor.copy(alpha = 0.1f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CategoryIconView(iconStr = item.category.icon, color = parentColor, modifier = Modifier.size(12.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = item.category.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (item.isSelected) parentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (item.isSelected) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = parentColor, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("إغلاق", color = typeAccentColor)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────
//  QuickDateButton
// ─────────────────────────────────────────────────────────

@Composable
fun RowScope.QuickDateButton(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

class FormulaThousandsSeparatorTransformation : VisualTransformation {
    private val westernSymbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
    private val westernFormatter = java.text.DecimalFormat("#,###", westernSymbols)

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val operators = setOf("+", "-", "×", "÷")
        val parts = originalText.split(" ")
        
        val transformedParts = ArrayList<String>()
        val originalToTransformedMap = ArrayList<Int>()
        val transformedToOriginalMap = ArrayList<Int>()
        
        var originalOffset = 0
        var transformedOffset = 0
        
        for (i in parts.indices) {
            val part = parts[i]
            if (i > 0) {
                transformedParts.add(" ")
                originalToTransformedMap.add(transformedOffset)
                transformedToOriginalMap.add(originalOffset)
                originalOffset += 1
                transformedOffset += 1
            }
            
            if (operators.contains(part) || part.isEmpty()) {
                for (j in part.indices) {
                    originalToTransformedMap.add(transformedOffset + j)
                    transformedToOriginalMap.add(originalOffset + j)
                }
                transformedParts.add(part)
                originalOffset += part.length
                transformedOffset += part.length
            } else {
                val subParts = part.split(".")
                val integerPart = subParts[0]
                val decimalPart = if (subParts.size > 1) subParts[1] else null
                
                val formattedInteger = try {
                    val longVal = integerPart.toLongOrNull()
                    if (longVal != null) {
                        westernFormatter.format(longVal)
                    } else {
                        integerPart
                    }
                } catch (e: Exception) {
                    integerPart
                }
                
                val formattedPart = buildString {
                    append(formattedInteger)
                    if (decimalPart != null) {
                        append(".")
                        append(decimalPart)
                    } else if (part.endsWith(".")) {
                        append(".")
                    }
                }
                
                var origIntIdx = 0
                var transIntIdx = 0
                val partOrigToTrans = IntArray(part.length + 1)
                val partTransToOrig = IntArray(formattedPart.length + 1)
                
                while (origIntIdx < part.length && transIntIdx < formattedPart.length) {
                    val charTrans = formattedPart[transIntIdx]
                    if (charTrans == ',') {
                        partTransToOrig[transIntIdx] = origIntIdx
                        transIntIdx++
                    } else {
                        partOrigToTrans[origIntIdx] = transIntIdx
                        partTransToOrig[transIntIdx] = origIntIdx
                        origIntIdx++
                        transIntIdx++
                    }
                }
                partOrigToTrans[part.length] = formattedPart.length
                partTransToOrig[formattedPart.length] = part.length
                
                for (j in 0 until part.length) {
                    originalToTransformedMap.add(transformedOffset + partOrigToTrans[j])
                }
                for (j in 0 until formattedPart.length) {
                    transformedToOriginalMap.add(originalOffset + partTransToOrig[j])
                }
                
                transformedParts.add(formattedPart)
                originalOffset += part.length
                transformedOffset += formattedPart.length
            }
        }
        
        originalToTransformedMap.add(transformedOffset)
        transformedToOriginalMap.add(originalOffset)
        
        val transformedString = FormatterUtils.convertNumerals(transformedParts.joinToString(""))
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, originalText.length)
                return originalToTransformedMap[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedString.length)
                return transformedToOriginalMap[clamped]
            }
        }
        
        return TransformedText(
            text = AnnotatedString(transformedString),
            offsetMapping = offsetMapping
        )
    }
}


