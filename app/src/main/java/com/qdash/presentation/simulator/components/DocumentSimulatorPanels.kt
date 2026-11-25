package com.qdash.presentation.simulator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qdash.presentation.simulator.DocumentSimulatorUiState
import com.qdash.presentation.simulator.DocumentType
import com.qdash.ui.designsystem.tokens.ColorTokens

@Composable
private fun isDarkSimulatorTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

private fun ltrIsolate(text: String): String = "\u2066$text\u2069"

@Composable
fun EducationalGuidePanel(
    uiState: DocumentSimulatorUiState,
    onStartGuide: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onStopGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkSimulatorTheme()
    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
    val totalSteps = if (isCheque) 6 else 9

    val accentColor = if (isDark) ColorTokens.PosteYellow else ColorTokens.PosteBlue
    val cardBg = if (isDark) ColorTokens.CardDark else ColorTokens.PosteBlue.copy(alpha = 0.04f)
    val cardBorder = if (isDark) ColorTokens.PosteYellow.copy(alpha = 0.35f) else ColorTokens.PosteBlue.copy(alpha = 0.25f)

    val guideTitle = when (uiState.currentGuideStep) {
        0 -> if (isCheque) "1. الحساب البريدي والموطن (CCP & Payable à)" else "1. اختيار العملية والخدمات (2)"
        1 -> if (isCheque) "2. المبلغ بالأرقام والحروف" else "2. رقم الحساب والمفتاح (1)"
        2 -> if (isCheque) "3. المستفيد من الصك (لأمر)" else "3. المبلغ بالأرقام والحروف"
        3 -> if (isCheque) "4. مكان تحرير الصك" else "4. هوية المرسل / صاحب الأمر (3)"
        4 -> if (isCheque) "5. تاريخ التحرير وصلاحية الصك" else "5. عنوان المرسل الكامل"
        5 -> if (isCheque) "6. التوقيع والمنطقة البيضاء (Série AP)" else "6. معلومات المستفيد (3)"
        6 -> "7. عنوان المستفيد"
        7 -> "8. السبب وتاريخ شاك سوكور"
        8 -> "9. الإمضاء وإطار مكتب البريد (P.I)"
        else -> ""
    }

    val guideDesc = when (uiState.currentGuideStep) {
        0 -> if (isCheque) {
            "في أسفل الوسط يظهر رقم حسابك الجاري CCP مرفقاً بالمفتاح (Clé) المكون من رقمين واسم صاحب الحساب. على اليسار يظهر إطار التوطين الثابت «Payable à / يوفى: Agence 99999 - CNCP 16407 Alger»."
        } else {
            "اشطب (X) في الخانات أعلى اليسار: «سحب / Retrait» أو «دفع / Versement»، ويمكنك تحديد «كشف العمليات / Relevé des opérations» مع الفترة (du ... au ...)، أو «مع إظهار الرصيد»، أو «إشعار بالرصيد»، أو «طلب دفتر»."
        }
        1 -> if (isCheque) {
            "اكتب المبلغ بالأرقام بين DA و دج بصيغة #5.200,00# لمنع التلاعب، ثم اكتبه بالحروف على السطرين أسفل «Payez, contre ce chèque / إدفعوا مقابل هذا الصك» بالعربية أو الفرنسية."
        } else {
            "في الخانة العلوية «(1) Compte N° / ح ب ج رقم»، اكتب رقم الحساب البريدي الجاري (CCP) داخل المربع الأبيض الطويل، والمفتاح من رقمين في خانة «Clé / المفتاح»."
        }
        2 -> if (isCheque) {
            "اكتب اسم ولقب المستفيد في سطر «A l'ordre de / لأمر». للسحب النقدي الشخصي اكتب «لنفسي» أو «Moi-même»، وللتحويل أضف اسم الشخص ورقم حسابه CCP والمفتاح."
        } else {
            "اكتب المبلغ بالأرقام داخل المربع الأبيض لخانة «(1) Montant en chiffres / المبلغ بالأرقام»، ثم اكتبه بالحروف أسفل «en lettres» بالدينار الجزائري حصراً وليس بالسنتيم."
        }
        3 -> if (isCheque) {
            "اكتب اسم البلدية أو الولاية التي تحرر فيها الصك قبل كلمة «Le» (مثال: الجزائر أو وهران أو بومرداس)."
        } else {
            "في عمود «(3) Expéditeur / Donneur d'ordre (المرسل)»، اكتب اللقب (Nom) والاسم (Prénom) كما هما في بطاقة الهوية (إجباري في حالة الدفع Versement)."
        }
        4 -> if (isCheque) {
            "اكتب التاريخ الفعلي للتوقيع بعد «Le / في». ملاحظة: الصك البريدي لبريد الجزائر صالح قانونياً لمدة 3 سنوات من تاريخ إصداره."
        } else {
            "أكمل خانة «Adresse / العنوان» في عمود المرسل بعنوان إقامتك الحالي بدقة للتواصل عند وجود أي ملاحظة على الحوالة."
        }
        5 -> if (isCheque) {
            "وقّع في خانة الإمضاء أسفل التاريخ بما يطابق توقيعك المعتمد. تنبيه: الشريط الأبيض السفلي بجانب «Série : AP» مخصص للقراءة الآلية (MICR) — يُمنع الكتابة فيه (الرجاء عدم الكتابة في المساحة البيضاء)."
        } else {
            "في عمود «(3) Bénéficiaire (المستفيد)»، اكتب لقب واسم المستفيد بدقة مطابقة لوثيقة الهوية الوطنية (إجباري في حالة الدفع)."
        }
        6 -> "أدخل العنوان الكامل للشخص المستفيد في خانة «Adresse / العنوان» داخل عمود المستفيد لتسهيل التحقق والمطابقة في مكتب البريد."
        7 -> "حدّد تاريخ العملية في خانة «Date / التاريخ» بصيغة اليوم/الشهر/السنة، واكتب غرض العملية في خانة «(3) Motif / Correspondance» عند الحاجة."
        8 -> "وقّع في خانة «(1) Signature / الإمضاء». تنبيه: الشريط الأصفر «إطار مخصص لمكتب البريد / Cadre réservé au bureau de poste» وما تحته من بيانات بطاقة الهوية (P.I / N° / Délivré(e) le / Par) وختم التاريخ (T.A.D) ووصل العملية مخصصة لعون البريد (وتملأ P.I هنا للمحاكاة التعليمية)."
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("educational_guide_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!uiState.guideActive) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(accentColor.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "الدليل التفاعلي المساعد",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على زر البدء للحصول على محاكاة مرئية حية لكل خانة بالترتيب مع إرشادات لتفادي أخطاء ملء الوثائق البريدية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartGuide,
                        modifier = Modifier.testTag("btn_start_guide")
                    ) {
                        Text("بدء الدليل التفاعلي", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = guideTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ltrIsolate("${uiState.currentGuideStep + 1} / $totalSteps"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (uiState.currentGuideStep + 1).toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    Text(
                        text = guideDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onPrevStep,
                            enabled = uiState.currentGuideStep > 0,
                            modifier = Modifier.testTag("btn_prev_guide_step")
                        ) {
                            Text("السابق", maxLines = 1)
                        }

                        TextButton(
                            onClick = onStopGuide,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_stop_guide")
                        ) {
                            Text(
                                text = "إنهاء الدليل",
                                color = if (isDark) ColorTokens.DangerDark else MaterialTheme.colorScheme.error,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                if (uiState.currentGuideStep < totalSteps - 1) {
                                    onNextStep()
                                } else {
                                    onStopGuide()
                                }
                            },
                            modifier = Modifier.testTag("btn_next_guide_step")
                        ) {
                            Text(
                                text = if (uiState.currentGuideStep == totalSteps - 1) "إنهاء" else "التالي",
                                maxLines = 1
                            )
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
    val isDark = isDarkSimulatorTheme()
    val errors = uiState.errors
    val warnings = uiState.warnings
    val percentage = uiState.completionPercentage
    val isReady = errors.isEmpty() && percentage == 1f

    val successTextColor = if (isDark) ColorTokens.SuccessDark else Color(0xFF15803D)
    val successBgColor = if (isDark) ColorTokens.PositiveContainerDark.copy(alpha = 0.5f) else Color(0xFFE8F5E9)
    val successBorderColor = if (isDark) ColorTokens.SuccessDark.copy(alpha = 0.45f) else ColorTokens.Success.copy(alpha = 0.45f)

    val progressAccent = when {
        isReady -> successTextColor
        isDark -> ColorTokens.PosteYellow
        else -> ColorTokens.PosteBlue
    }

    val errorBgColor = if (isDark) ColorTokens.NegativeContainerDark.copy(alpha = 0.4f) else ColorTokens.Danger.copy(alpha = 0.08f)
    val errorBorderColor = if (isDark) ColorTokens.DangerDark.copy(alpha = 0.4f) else ColorTokens.Danger.copy(alpha = 0.3f)
    val errorTitleColor = if (isDark) ColorTokens.DangerDark else Color(0xFFB91C1C)
    val errorTextColor = if (isDark) ColorTokens.TextPrimaryDark else Color(0xFF7F1D1D)

    val warningBgColor = if (isDark) ColorTokens.WarningDark.copy(alpha = 0.14f) else ColorTokens.Warning.copy(alpha = 0.10f)
    val warningBorderColor = if (isDark) ColorTokens.WarningDark.copy(alpha = 0.4f) else ColorTokens.Warning.copy(alpha = 0.35f)
    val warningTitleColor = if (isDark) ColorTokens.WarningDark else Color(0xFFB45309)
    val warningTextColor = if (isDark) ColorTokens.TextPrimaryDark else Color(0xFF78350F)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("validation_checklist_panel"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "جاهزية الوثيقة للتعبئة الحقيقية:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ltrIsolate("${(percentage * 100).toInt()}%"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = progressAccent
                    )
                )
            }
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressAccent,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (isReady) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("validation_ready_banner"),
                color = successBgColor,
                border = BorderStroke(1.dp, successBorderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = successTextColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (warnings.isEmpty()) {
                            "الوثيقة جاهزة تماماً! يمكنك نسخ البيانات المكتوبة هنا إلى الوثيقة الورقية الحقيقية بأمان."
                        } else {
                            "الحقول الأساسية مكتملة! راجع التنبيهات الإضافية أدناه قبل التحرير على الورق."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = successTextColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (errors.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("validation_errors_card"),
                color = errorBgColor,
                border = BorderStroke(1.dp, errorBorderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = errorTitleColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "الحقول الناقصة أو الخاطئة (${errors.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = errorTitleColor
                        )
                    }
                    errors.forEach { (_, message) ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .background(errorTitleColor, CircleShape)
                            )
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = errorTextColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (warnings.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("validation_warnings_card"),
                color = warningBgColor,
                border = BorderStroke(1.dp, warningBorderColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = warningTitleColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تنبيهات هامة وملاحظات:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = warningTitleColor
                        )
                    }
                    warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 7.dp)
                                    .size(6.dp)
                                    .background(warningTitleColor, CircleShape)
                            )
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = warningTextColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
