package com.qdash.presentation.simulator.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.domain.model.PostalProfileRole
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SimulatorActivePanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillFormPanel(
    uiState: DocumentSimulatorUiState,
    onChequeAmountChange: (String) -> Unit,
    onChequeBeneficiaryChange: (String) -> Unit,
    onChequeCcpChange: (String) -> Unit,
    onChequeKeyChange: (String) -> Unit,
    onChequePlaceChange: (String) -> Unit,
    onChequeDateChange: (String) -> Unit,
    onToggleReadyAmounts: (Boolean) -> Unit,
    onAutofillClick: (PostalProfileRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.selectedDocType == DocumentType.CHEQUE) {
            // Cheque Edit Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAutofillClick(PostalProfileRole.SELF) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("سحب لنفسي", fontSize = 12.sp)
                }

                Button(
                    onClick = { onAutofillClick(PostalProfileRole.BENEFICIARY) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعبئة مستفيد", fontSize = 12.sp)
                }
            }

            // 1. Amount Input & Assistance
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.chequeAmount,
                        onValueChange = onChequeAmountChange,
                        label = { Text("المبلغ بالدينار الجزائري (DA)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            if (uiState.chequeAmount.isNotEmpty()) {
                                IconButton(onClick = { onChequeAmountChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح")
                                }
                            }
                        }
                    )
                    
                    IconButton(
                        onClick = { onToggleReadyAmounts(true) },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(50.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalAtm,
                            contentDescription = "المبالغ الجاهزة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val amt = uiState.chequeAmount.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    val formatted   = AmountConversionEngine.formatAmountToPostal(amt)
                    val colloquial  = AmountConversionEngine.getAlgerianColloquialWords(amt)
                    val officialWords = AmountConversionEngine.convertToArabicWords(amt)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("التنسيق المالي المضاد للتزوير:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "#$formatted#",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF0D47A1)
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFF0D47A1).copy(alpha = 0.15f))
                            Text(
                                text = "بالعامية: $colloquial",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "بالفصحى: $officialWords",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            cb.setPrimaryClip(android.content.ClipData.newPlainText("words", officialWords))
                                            Toast.makeText(context, "تم نسخ الحروف!", Toast.LENGTH_SHORT).show()
                                        },
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("نسخ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                    }
                }
            }

            // 2. Beneficiary Input
            OutlinedTextField(
                value = uiState.chequeBeneficiary,
                onValueChange = onChequeBeneficiaryChange,
                label = { Text("المستفيد (لأمر / A l'ordre de)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // 3. Account CCP & Key
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.chequeCcp,
                    onValueChange = onChequeCcpChange,
                    label = { Text("رقم حسابك CCP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.chequeKey,
                    onValueChange = onChequeKeyChange,
                    label = { Text("المفتاح") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // 4. Place & Date
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.chequePlace,
                    onValueChange = onChequePlaceChange,
                    label = { Text("المكان") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.chequeDate,
                    onValueChange = onChequeDateChange,
                    label = { Text("التاريخ") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

        } else {
            // SFP 01 Edit Mode
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAutofillClick(PostalProfileRole.SENDER) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("تعبئة المرسل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("من ملفاتك", fontSize = 9.sp)
                        }
                    }
                    Button(
                        onClick = { onAutofillClick(PostalProfileRole.BENEFICIARY) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("تعبئة المستفيد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("من ملفاتك", fontSize = 9.sp)
                        }
                    }
                }
            }

            // Anti-fraud formatted display helper
            val sfpAmt = uiState.sfpAmount.toDoubleOrNull()
            if (sfpAmt != null && sfpAmt > 0) {
                val formatted     = AmountConversionEngine.formatAmountToPostal(sfpAmt)
                val colloquial    = AmountConversionEngine.getAlgerianColloquialWords(sfpAmt)
                val officialWords = AmountConversionEngine.convertToArabicWords(sfpAmt)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("التنسيق المالي المضاد للتزوير:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "#$formatted#",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF0D47A1)
                                )
                            )
                        }
                        HorizontalDivider(color = Color(0xFF0D47A1).copy(alpha = 0.15f))
                        Text(
                            text = "بالعامية: $colloquial",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "بالفصحى: $officialWords",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        val cb = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        cb.setPrimaryClip(android.content.ClipData.newPlainText("words", officialWords))
                                        Toast.makeText(context, "تم نسخ الحروف!", Toast.LENGTH_SHORT).show()
                                    },
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                             ) {
                                Text("نسخ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmountCheatSheetPanel(
    onAmountSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val amounts = listOf(
        1_000.0   to "1.000 DA – ألف دينار",
        2_000.0   to "2.000 DA – ألفين دينار",
        5_000.0   to "5.000 DA – خمسة آلاف",
        10_000.0  to "10.000 DA – عشرة آلاف",
        20_000.0  to "20.000 DA – عشرون ألف",
        30_000.0  to "30.000 DA – ثلاثون ألف",
        50_000.0  to "50.000 DA – خمسون ألف",
        100_000.0 to "100.000 DA – مئة ألف",
        150_000.0 to "150.000 DA – مئة وخمسون ألف",
        200_000.0 to "200.000 DA – مئتا ألف"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "اضغط على أي مبلغ لتعبئته تلقائياً في الوثيقة",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("المبلغ بالأرقام", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1.2f))
            Text("تنسيق مضاد للتزوير", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1.3f), textAlign = TextAlign.Center)
            Text("بالعامية", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }

        Spacer(modifier = Modifier.height(2.dp))

        amounts.forEachIndexed { index, (value, _) ->
            val formatted = AmountConversionEngine.formatAmountToPostal(value)
            val colloquial = AmountConversionEngine.getAlgerianColloquialWords(value)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onAmountSelected(value.toInt().toString()) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${value.toInt()} DA",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1.2f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "#$formatted#",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    ),
                    modifier = Modifier.weight(1.3f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = colloquial,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (index < amounts.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun EducationalGuidePanel(
    uiState: DocumentSimulatorUiState,
    onStartGuide: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onStopGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
    val totalSteps = if (isCheque) 6 else 9

    val guideTitle = when (uiState.currentGuideStep) {
        0 -> if (isCheque) "1. الحساب البريدي والمفتاح (CCP & Clé)" else "1. تحديد نوع العملية المالية"
        1 -> if (isCheque) "2. كتابة المبلغ بالأرقام" else "2. رقم حساب المستقبل والمفتاح"
        2 -> if (isCheque) "3. المستفيد من الصك (لأمر)" else "3. كتابة المبالغ بالأرقام والحروف"
        3 -> if (isCheque) "4. تحديد مكان السحب أو التحويل" else "4. معلومات المرسل (أنت)"
        4 -> if (isCheque) "5. تاريخ ملء الصك البريدي" else "5. عنوان المرسل الكامل"
        5 -> if (isCheque) "6. إمضاء الصك البريدي" else "6. معلومات المستفيد"
        6 -> "7. عنوان المستفيد"
        7 -> "8. مكان وتاريخ ملء الحوالة"
        8 -> "9. إمضاء الاستمارة وتفاصيل الهوية"
        else -> ""
    }

    val guideDesc = when (uiState.currentGuideStep) {
        0 -> if (isCheque) {
            "في أسفل الصك، اكتب رقم حسابك الجاري CCP بدون المفتاح، ثم اكتب المفتاح المكون من رقمين في الخانة المجاورة. تأكد من دقتها لتفادي سحب الأموال من حساب خاطئ."
        } else {
            "ضع علامة مقاطع (X) في المربع المقابل للعملية المرغوبة: 'دفع' (Versement) لصب الأموال نقداً، أو 'سحب' (Retrait) لسحب المال من حسابك."
        }
        1 -> if (isCheque) {
            "اكتب المبلغ بالأرقام بوضوح في المربع الأعلى يميناً بجانب رمز DA. يُنصح بكتابة الفاصلة والصفرين مثل 12.300,00 لتفادي التلاعب بالمبلغ أو التزوير."
        } else {
            "اكتب رقم الحساب الجاري للمستقبل (CCP) والمفتاح المكون من رقمين بدقة عالية لتضمن وصول الأموال للشخص الصحيح."
        }
        2 -> if (isCheque) {
            "اكتب اسم ولقب المستفيد بالكامل. إذا كنت تسحب لنفسك نقداً، اكتب 'لنفسي' أو 'Moi-même'. وفي حالة التحويل، اكتب اسم الشخص متبوعاً بـ CCP والمفتاح الخاص به."
        } else {
            "اكتب المبلغ بالأرقام في الخانة المخصصة (مثال: 5000,00)، ثم أتبعه بكتابته بالحروف الفصحى بالدينار حصراً (مثال: خمسة آلاف دينار جزائري). انتبه: لا تكتب بالسنتيم!"
        }
        3 -> if (isCheque) {
            "اكتب اسم البلدية أو المدينة التي تسحب أو توقع فيها الصك الجاري (مثال: في تقرت أو في الجزائر العاصمة)."
        } else {
            "اكتب لقبك واسمك بوضوح كما هو مسجل في بطاقة الهوية الخاصة بك. أنت بصفتك دافع أو مرسل الأموال."
        }
        4 -> if (isCheque) {
            "اكتب التاريخ الفعلي ليوم ملء وتوقيع الصك. يرجى الملاحظة أن الصك البريدي لبريد الجزائر صالح قانونياً لمدة 3 سنوات من تاريخ إصداره."
        } else {
            "اكتب عنوان سكنك الفعلي والحالي بوضوح، حيث يطلب مكتب البريد كتابة عنوان المرسل للتواصل في حال حدوث أي مشكلة في الحوالة."
        }
        5 -> if (isCheque) {
            "ضع توقيعك المطابق للتوقيع المعتمد في ملف حسابك البريدي. تذكر أن التوقيع الخاطئ أو غير المطابق يؤدي لرفض صرف الصك فوراً."
        } else {
            "اكتب اسم ولقب الشخص المستقبل للأموال (المستفيد) بالكامل وبدقة كما هو مسجل في هويته الوطنية لضمان إمكانية استلامه للمبلغ."
        }
        6 -> "اكتب العنوان الكامل للشخص المستفيد (المرسل إليه). هذا الحقل يساعد عون البريد في التحقق من البيانات وتفادي أي التباس."
        7 -> "اكتب تاريخ ملء الاستمارة، واكتب اسم المدينة أو البلدية التي تتواجد بها حالياً في الخانات المخصصة."
        8 -> "قم بالتوقيع في خانة الإمضاء الخاصة بك. يرجى الانتباه أن الجزء السفلي المسمى 'إطار مخصص لمكتب البريد' يجب تركه تماماً فارغاً."
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!uiState.guideActive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الدليل التفاعلي المساعد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على زر البدء ليعطيك محاكاة مرئية حية لكل خانة بالترتيب مع إرشادات لتفادي أخطاء ملء النماذج.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onStartGuide) {
                        Text("بدء الدليل التفاعلي")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = guideTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${uiState.currentGuideStep + 1} / $totalSteps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = guideDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = onPrevStep,
                            enabled = uiState.currentGuideStep > 0
                        ) {
                            Text("السابق")
                        }

                        TextButton(
                            onClick = onStopGuide
                        ) {
                            Text("إنهاء الدليل", color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = {
                                if (uiState.currentGuideStep < totalSteps - 1) {
                                    onNextStep()
                                } else {
                                    onStopGuide()
                                }
                            }
                        ) {
                            Text(if (uiState.currentGuideStep == totalSteps - 1) "إنهاء" else "التالي")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationChecklistPanel(
    uiState: DocumentSimulatorUiState,
    modifier: Modifier = Modifier
) {
    val errors = uiState.errors
    val warnings = uiState.warnings
    val percentage = uiState.completionPercentage

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جاهزية الوثيقة للتعبئة الحقيقية:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = if (percentage == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                )
            }
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (percentage == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        HorizontalDivider()

        if (errors.isEmpty() && warnings.isEmpty() && percentage == 1f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "الوثيقة جاهزة تماماً! يمكنك نسخ البيانات المكتوبة هنا إلى الوثيقة الورقية الحقيقية بأمان.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (errors.isNotEmpty()) {
                    Text(
                        text = "الحقول الناقصة أو الخاطئة (${errors.size}):",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                    errors.forEach { (_, message) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                if (warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تنبيهات هامة وملاحظات:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFF8F00)
                    )
                    warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(6.dp)
                                    .background(Color(0xFFFF8F00), CircleShape)
                            )
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF827717)
                            )
                        }
                    }
                }
            }
        }
    }
}
