package com.qdash.presentation.simulator.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
            "في أسفل الوسط يظهر رقم CCP والمفتاح (رقمان) مع اسم صاحب الحساب، وإلى اليسار إطار التوطين الثابت (CNCP 16407 Alger)."
        } else {
            "اشطب (X) على نوع العملية: «سحب Retrait» أو «دفع Versement»، ويمكنك تفعيل كشف العمليات أو إظهار الرصيد أو طلب دفتر."
        }
        1 -> if (isCheque) {
            "اكتب المبلغ بالأرقام بين DA و دج بصيغة #5.200,00#، ثم بالحروف على السطرين بالعربية أو الفرنسية."
        } else {
            "في خانة «Compte N° / ح ب ج رقم» أدخل رقم الحساب البريدي الجاري (CCP) والمفتاح من رقمين في خانة Clé."
        }
        2 -> if (isCheque) {
            "اكتب اسم المستفيد في سطر «لأمر». للسحب الشخصي اختر «لنفسي / Moi-même»، وللتحويل أضف رقم CCP المستفيد."
        } else {
            "اكتب المبلغ بالأرقام ثم بالحروف أسفل «en lettres» بالدينار الجزائري حصراً وليس بالسنتيم."
        }
        3 -> if (isCheque) {
            "اكتب اسم البلدية أو الولاية التي تحرر فيها الصك قبل كلمة «Le / في» (مثال: الجزائر أو وهران)."
        } else {
            "في عمود «Expéditeur / المرسل» أدخل اللقب والاسم كما في بطاقة الهوية (إجباري عند الدفع Versement)."
        }
        4 -> if (isCheque) {
            "اكتب تاريخ التحرير بعد «Le / في». ملاحظة: الصك البريدي لبريد الجزائر صالح لمدة 3 سنوات من تاريخ إصداره."
        } else {
            "أكمل عنوان إقامة المرسل بدقة في خانة «Adresse / العنوان» للتواصل عند الحاجة."
        }
        5 -> if (isCheque) {
            "وقّع أسفل التاريخ ولا تكتب في الشريط الأبيض السفلي (Série AP) المخصص للقراءة الآلية MICR."
        } else {
            "في عمود «Bénéficiaire / المستفيد» أدخل لقب واسم المستفيد بدقة مطابقة لوثيقة الهوية."
        }
        6 -> "أدخل العنوان الكامل للمستفيد في خانة «Adresse / العنوان» لتسهيل التحقق في مكتب البريد."
        7 -> "حدّد تاريخ العملية في خانة «Date / التاريخ» واكتب سبب العملية في «Motif / Correspondance» عند الحاجة."
        8 -> "وقّع في خانة الإمضاء؛ الشريط السفلي (P.I / T.A.D / وصل العملية) مخصص لعون البريد (تملأ P.I هنا للمحاكاة)."
        else -> ""
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("educational_guide_panel")
    ) {
        if (!uiState.guideActive) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(accentColor.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "الدليل التفاعلي المساعد",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "شرح مختصر خطوة بخطوة لحقول الوثيقة",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onStartGuide,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_start_guide")
                    ) {
                        Text("بدء", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    IconButton(
                        onClick = onStopGuide,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_stop_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق الدليل",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ltrIsolate("${uiState.currentGuideStep + 1}/$totalSteps"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = guideTitle,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = accentColor,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = onPrevStep,
                            enabled = uiState.currentGuideStep > 0,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_prev_guide_step")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "السابق",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Surface(
                            onClick = {
                                if (uiState.currentGuideStep < totalSteps - 1) {
                                    onNextStep()
                                } else {
                                    onStopGuide()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor,
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("btn_next_guide_step")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = if (uiState.currentGuideStep == totalSteps - 1) "إنهاء" else "التالي",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isDark) Color.Black else Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                        IconButton(
                            onClick = onStopGuide,
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("btn_stop_guide")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (uiState.currentGuideStep + 1).toFloat() / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )

                    Text(
                        text = guideDesc,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
