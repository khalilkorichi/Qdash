package com.example.presentation.simulator

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.components.FinTrackTopBar
import com.example.domain.model.PostalProfile
import com.example.domain.model.PostalProfileRole
import com.example.domain.usecase.simulator.AmountConversionEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSimulatorScreen(
    viewModel: DocumentSimulatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeTab by remember { mutableStateOf(0) } // 0: Form/Fill, 1: Guide, 2: Checklist
    var showProfilePicker by remember { mutableStateOf(false) }
    var selectedRoleForAutofill by remember { mutableStateOf<PostalProfileRole?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = if (uiState.selectedDocType == DocumentType.CHEQUE) "محاكي الصك البريدي" else "محاكي استمارة SFP 01",
                showBackButton = true,
                onBackClick = onBack,
                actions = {
                    IconButton(onClick = {
                        val newType = if (uiState.selectedDocType == DocumentType.CHEQUE) DocumentType.SFP01 else DocumentType.CHEQUE
                        viewModel.selectDocumentType(newType)
                    }) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تغيير الوثيقة",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Document Type Switcher / Info
            DocumentSelectorHeader(
                selectedType = uiState.selectedDocType,
                onTypeSelect = { viewModel.selectDocumentType(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // SFP 01 – Operation type selector OUTSIDE the form (premium styled)
            if (uiState.selectedDocType == DocumentType.SFP01) {
                SfpOperationSelectorBar(
                    selectedOperation = uiState.sfpOperation,
                    onOperationSelected = { viewModel.updateSfpOperation(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Document Simulator Preview (Layer 1 & 2)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(6.dp, RoundedCornerShape(4.dp))
            ) {
                if (uiState.selectedDocType == DocumentType.CHEQUE) {
                    ChequeVisualView(
                        uiState = uiState,
                        onFieldTap = { viewModel.setFocusedField(it) }
                    )
                } else {
                    SfpVisualView(
                        uiState = uiState,
                        onFieldTap = { viewModel.setFocusedField(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Tabs (Mode B + Tools + Guide + Checklist)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("تعبئة البيانات", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("دليل خطوة بخطوة", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = {
                                BadgedBox(badge = {
                                    if (uiState.errors.isNotEmpty()) {
                                        Badge { Text(uiState.errors.size.toString()) }
                                    }
                                }) {
                                    Text("مراجعة الوثيقة", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedContent(
                        targetState = activeTab,
                        label = "tab_animation"
                    ) { tabIndex ->
                        when (tabIndex) {
                            0 -> FillFormPanel(
                                uiState = uiState,
                                viewModel = viewModel,
                                onAutofillClick = { role ->
                                    selectedRoleForAutofill = role
                                    showProfilePicker = true
                                }
                            )
                            1 -> EducationalGuidePanel(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                            2 -> ValidationChecklistPanel(
                                uiState = uiState
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showProfilePicker) {
        AutofillProfileBottomSheet(
            profiles = uiState.savedProfiles,
            onDismiss = { showProfilePicker = false },
            onProfileSelected = { profile ->
                selectedRoleForAutofill?.let { role ->
                    viewModel.autofillFromProfile(profile, role)
                    Toast.makeText(context, "تم التعبئة التلقائية!", Toast.LENGTH_SHORT).show()
                }
                showProfilePicker = false
            }
        )
    }
}

@Composable
fun DocumentSelectorHeader(
    selectedType: DocumentType,
    onTypeSelect: (DocumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedType == DocumentType.CHEQUE,
            onClick = { onTypeSelect(DocumentType.CHEQUE) },
            label = { Text("صك بريدي (Chèque)", fontWeight = FontWeight.Bold) },
            modifier = Modifier.weight(1f)
        )
        FilterChip(
            selected = selectedType == DocumentType.SFP01,
            onClick = { onTypeSelect(DocumentType.SFP01) },
            label = { Text("حوالة الدفع (SFP 01)", fontWeight = FontWeight.Bold) },
            modifier = Modifier.weight(1f)
        )
    }
}

// --- Layer 1 & 2: Cheque Visual Simulator Layout ---
@Composable
fun ChequeVisualView(
    uiState: DocumentSimulatorUiState,
    onFieldTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chequeBgColor = Color(0xFFFAF6E9) // Traditional postal check cream background
    val chequeBorderColor = Color(0xFF8C9D86)
    val writingColor = Color(0xFF0D1B2A) // Pen ink blue/black
    
    val formattedAmount = remember(uiState.chequeAmount) {
        val amt = uiState.chequeAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.formatAmountToPostal(amt) else ""
    }

    val amountInWords = remember(uiState.chequeAmount) {
        val amt = uiState.chequeAmount.toDoubleOrNull()
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
                text = uiState.chequeBeneficiary.ifEmpty { "..........................................................................." },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (uiState.chequeBeneficiary.isNotEmpty()) writingColor else Color.Gray,
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
                        text = uiState.chequePlace.ifEmpty { "..............." },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.chequePlace.isNotEmpty()) writingColor else Color.Gray
                    )
                }
                
                // Date
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onFieldTap("chequeDate") }) {
                    Text(text = "بتاريخ (Le) : ", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    Text(
                        text = uiState.chequeDate.ifEmpty { "..../..../........" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (uiState.chequeDate.isNotEmpty()) writingColor else Color.Gray
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
                    text = "رقم الحساب: ${uiState.chequeCcp.ifEmpty { ".................." }}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (uiState.chequeCcp.isNotEmpty()) writingColor else Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .border(1.dp, chequeBorderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "المفتاح: ${uiState.chequeKey.ifEmpty { ".." }}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (uiState.chequeKey.isNotEmpty()) writingColor else Color.Gray
                )
            }
        }
    }
}

// --- SFP Operation Type Selector Bar (OUTSIDE the form, premium design) ---
@Composable
fun SfpOperationSelectorBar(
    selectedOperation: SfpOperationType,
    onOperationSelected: (SfpOperationType) -> Unit,
    modifier: Modifier = Modifier
) {
    val operations = listOf(
        Triple(SfpOperationType.VERSEMENT, "دفع\nVersement", Icons.Default.ArrowUpward),
        Triple(SfpOperationType.RETRAIT,   "سحب\nRetrait",   Icons.Default.ArrowDownward),
        Triple(SfpOperationType.VIREMENT,  "تحويل\nVirement", Icons.Default.SwapHoriz)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "اختر نوع العملية المالية:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            operations.forEach { (type, label, icon) ->
                val isSelected = selectedOperation == type
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOperationSelected(type) },
                    color = bgColor,
                    shadowElevation = if (isSelected) 4.dp else 0.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                            ),
                            color = contentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// --- Layer 1 & 2: SFP 01 Visual Simulator Layout (Authentic Form) ---
@Composable
fun SfpVisualView(
    uiState: DocumentSimulatorUiState,
    onFieldTap: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val formBg      = Color(0xFFFFFDE7)
    val lineColor   = Color(0xFF795548)
    val headerBg    = Color(0xFFFDD835)
    val writingColor = Color(0xFF1A1A2E)
    val emptyColor  = Color(0xFFBCAAA4)
    val labelColor  = Color(0xFF5D4037)
    
    val formattedAmount = remember(uiState.sfpAmount) {
        val amt = uiState.sfpAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.formatAmountToPostal(amt) else ""
    }

    val amountInWords = remember(uiState.sfpAmount) {
        val amt = uiState.sfpAmount.toDoubleOrNull()
        if (amt != null) AmountConversionEngine.convertToArabicWords(amt) else ""
    }

    val isVersement = uiState.sfpOperation == SfpOperationType.VERSEMENT
    val isRetrait   = uiState.sfpOperation == SfpOperationType.RETRAIT
    val isVirement  = uiState.sfpOperation == SfpOperationType.VIREMENT

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(formBg)
            .border(2.dp, lineColor)
    ) {
        // ======= HEADER =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "العمليات المالية البريدية",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "Opérations financières postales",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF333333)
                )
            }
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(2.dp))
                    .border(1.5.dp, lineColor)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "SFP 01",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }
        }

        // ======= ACCOUNT NUMBER ROW =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
        ) {
            Column(
                modifier = Modifier
                    .weight(2.5f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpCcp") }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "رقم حساب المرسل إليه / Compte N°",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    color = labelColor
                )
                Text(
                    text = uiState.sfpCcp.ifEmpty { "_ _ _ _ _ _ _ _ _ _ _" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp
                    ),
                    color = if (uiState.sfpCcp.isNotEmpty()) writingColor else emptyColor
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpKey") }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "المفتاح/Clé",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    color = labelColor
                )
                Text(
                    text = uiState.sfpKey.ifEmpty { "_ _" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = if (uiState.sfpKey.isNotEmpty()) writingColor else emptyColor
                )
            }
        }

        // ======= OPERATION CHECKBOXES ROW =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
        ) {
            // دفع / Versement
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.5.dp, lineColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVersement) {
                        Text("X", style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Black, color = writingColor, fontSize = 11.sp
                        ))
                    }
                }
                Text(
                    text = "دفع / Versement",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    lineHeight = 10.sp
                )
            }
            // سحب / Retrait
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.5.dp, lineColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRetrait) {
                        Text("X", style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Black, color = writingColor, fontSize = 11.sp
                        ))
                    }
                }
                Text(
                    text = "سحب / Retrait",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    lineHeight = 10.sp
                )
            }
            // تحويل / Virement
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .border(1.5.dp, lineColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVirement) {
                        Text("X", style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Black, color = writingColor, fontSize = 11.sp
                        ))
                    }
                }
                Text(
                    text = "تحويل / Virement",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    lineHeight = 10.sp
                )
            }
            // Extra options column
            Column(
                modifier = Modifier
                    .weight(1.4f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .padding(horizontal = 4.dp, vertical = 3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("مع إظهار الرصيد", "إشعار بالرصيد", "طلب دفتر").forEach { lbl ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(modifier = Modifier.size(9.dp).border(0.8.dp, lineColor))
                        Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp))
                    }
                }
            }
        }

        // ======= AMOUNTS =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
                .clickable { onFieldTap("sfpAmount") }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المبلغ بالأرقام / Montant en chiffres:",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    color = labelColor
                )
                Text(
                    text = if (formattedAmount.isNotEmpty()) "$formattedAmount DA" else "_ _ _ _ _ _ _",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    ),
                    color = if (formattedAmount.isNotEmpty()) writingColor else emptyColor
                )
            }
            Divider(color = lineColor.copy(alpha = 0.4f), thickness = 0.5.dp)
            Text(
                text = "بالحروف / en lettres:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                color = labelColor
            )
            Text(
                text = amountInWords.ifEmpty { "_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _" },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                ),
                color = if (amountInWords.isNotEmpty()) writingColor else emptyColor,
                maxLines = 2
            )
        }

        // ======= SENDER & BENEFICIARY =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
        ) {
            // Sender
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpSenderName") }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "المرسل / Expéditeur - Donneur d'ordre",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFB71C1C)
                )
                Text(
                    text = "اللقب/Nom: ${uiState.sfpSenderNom.ifEmpty { "_ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (uiState.sfpSenderNom.isNotEmpty()) writingColor else emptyColor
                )
                Text(
                    text = "الاسم/Prénom: ${uiState.sfpSenderPrenom.ifEmpty { "_ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (uiState.sfpSenderPrenom.isNotEmpty()) writingColor else emptyColor
                )
                Text(
                    text = "العنوان/Adresse: ${uiState.sfpSenderAddress.ifEmpty { "_ _ _ _ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    color = if (uiState.sfpSenderAddress.isNotEmpty()) writingColor else emptyColor,
                    maxLines = 2
                )
            }
            // Beneficiary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpBeneficiaryName") }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "المرسل إليه / Bénéficiaire",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B5E20)
                )
                Text(
                    text = "اللقب/Nom: ${uiState.sfpBeneficiaryNom.ifEmpty { "_ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (uiState.sfpBeneficiaryNom.isNotEmpty()) writingColor else emptyColor
                )
                Text(
                    text = "الاسم/Prénom: ${uiState.sfpBeneficiaryPrenom.ifEmpty { "_ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = if (uiState.sfpBeneficiaryPrenom.isNotEmpty()) writingColor else emptyColor
                )
                Text(
                    text = "العنوان/Adresse: ${uiState.sfpBeneficiaryAddress.ifEmpty { "_ _ _ _ _ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    color = if (uiState.sfpBeneficiaryAddress.isNotEmpty()) writingColor else emptyColor,
                    maxLines = 2
                )
            }
        }

        // ======= MOTIF =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "سبب الإرسال / Motif - Correspondance:",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                color = labelColor
            )
            Text(
                text = "_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = emptyColor)
            )
        }

        // ======= DATE & SIGNATURE =======
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, lineColor))
        ) {
            Column(
                modifier = Modifier
                    .weight(2f)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpPlaceDate") }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "في / A: ${uiState.sfpPlace.ifEmpty { "_ _ _ _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                    color = if (uiState.sfpPlace.isNotEmpty()) writingColor else emptyColor
                )
                Text(
                    text = "بتاريخ / le: ${uiState.sfpDate.ifEmpty { "_ _ / _ _ / _ _ _ _" }}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.5.sp, fontFamily = FontFamily.Monospace
                    ),
                    color = if (uiState.sfpDate.isNotEmpty()) writingColor else emptyColor
                )
            }
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
                    .border(BorderStroke(0.5.dp, lineColor))
                    .clickable { onFieldTap("sfpSignature") }                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "الإمضاء / Signature",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.5.sp),
                    color = labelColor
                )
            }
        }

        // ======= RESERVED BUREAU FOOTER =======
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF9C4))
                .border(BorderStroke(1.5.dp, lineColor))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "إطار مخصص لمكتب البريد / Cadre réservé au bureau de poste",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp, fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4E342E)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = lineColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "وصل العملية / Reçu de l'opération",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 7.5.sp, fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF4E342E)
            )
            Text(
                text = "اترك هذا القسم فارغاً — يملؤه عون البريد",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                color = Color(0xFF8D6E63)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// --- Mode B: Field Editors / Assistant Inputs Panel ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillFormPanel(
    uiState: DocumentSimulatorUiState,
    viewModel: DocumentSimulatorViewModel,
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = uiState.chequeAmount,
                    onValueChange = { viewModel.updateChequeAmount(it) },
                    label = { Text("المبلغ بالدينار الجزائري (DA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (uiState.chequeAmount.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateChequeAmount("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    }
                )

                // Spoken dialect helper
                val amt = uiState.chequeAmount.toDoubleOrNull()
                if (amt != null && amt > 0) {
                    val colloquial = AmountConversionEngine.getAlgerianColloquialWords(amt)
                    val officialWords = AmountConversionEngine.convertToArabicWords(amt)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "العامية: $colloquial",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الفصحى: $officialWords",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "نسخ",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .clickable {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("amount_words", officialWords)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "تم النسخ الحروف الفصحى!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Beneficiary Input
            OutlinedTextField(
                value = uiState.chequeBeneficiary,
                onValueChange = { viewModel.updateChequeBeneficiary(it) },
                label = { Text("المستفيد (لأمر / A l'ordre de)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // 3. Account CCP & Key
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.chequeCcp,
                    onValueChange = { viewModel.updateChequeCcp(it) },
                    label = { Text("رقم حسابك CCP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.chequeKey,
                    onValueChange = { viewModel.updateChequeKey(it) },
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
                    onValueChange = { viewModel.updateChequePlace(it) },
                    label = { Text("المكان") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.chequeDate,
                    onValueChange = { viewModel.updateChequeDate(it) },
                    label = { Text("التاريخ") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

        } else {
            // SFP 01 Edit Mode
            // ── Autofill buttons ──
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

            // Account number & Clé
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.sfpCcp,
                    onValueChange = { viewModel.updateSfpCcp(it) },
                    label = { Text("رقم حساب المستفيد CCP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.sfpKey,
                    onValueChange = { viewModel.updateSfpKey(it) },
                    label = { Text("المفتاح") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Amount Sfp
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = uiState.sfpAmount,
                    onValueChange = { viewModel.updateSfpAmount(it) },
                    label = { Text("المبلغ بالدينار (DA)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Dialect helper
                val sfpAmt = uiState.sfpAmount.toDoubleOrNull()
                if (sfpAmt != null && sfpAmt > 0) {
                    val colloquial = AmountConversionEngine.getAlgerianColloquialWords(sfpAmt)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "العامية: $colloquial",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Sender Nom & Prénom
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.sfpSenderNom,
                    onValueChange = { viewModel.updateSfpSenderNom(it) },
                    label = { Text("لقب المرسل") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.sfpSenderPrenom,
                    onValueChange = { viewModel.updateSfpSenderPrenom(it) },
                    label = { Text("اسم المرسل") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            OutlinedTextField(
                value = uiState.sfpSenderAddress,
                onValueChange = { viewModel.updateSfpSenderAddress(it) },
                label = { Text("عنوان المرسل") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            // Beneficiary details if versement
            if (uiState.sfpOperation == SfpOperationType.VERSEMENT) {
                Text("بيانات المرسل إليه (المستفيد):", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.sfpBeneficiaryNom,
                        onValueChange = { viewModel.updateSfpBeneficiaryNom(it) },
                        label = { Text("لقب المستفيد") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = uiState.sfpBeneficiaryPrenom,
                        onValueChange = { viewModel.updateSfpBeneficiaryPrenom(it) },
                        label = { Text("اسم المستفيد") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                OutlinedTextField(
                    value = uiState.sfpBeneficiaryAddress,
                    onValueChange = { viewModel.updateSfpBeneficiaryAddress(it) },
                    label = { Text("عنوان المستفيد (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Place & Date
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.sfpPlace,
                    onValueChange = { viewModel.updateSfpPlace(it) },
                    label = { Text("المكان") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = uiState.sfpDate,
                    onValueChange = { viewModel.updateSfpDate(it) },
                    label = { Text("التاريخ") },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

// --- Tab 2: Educational Step-by-Step Guide Panel ---
@Composable
fun EducationalGuidePanel(
    uiState: DocumentSimulatorUiState,
    viewModel: DocumentSimulatorViewModel,
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
                    Button(onClick = { viewModel.startGuide() }) {
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
                            onClick = { viewModel.prevGuideStep() },
                            enabled = uiState.currentGuideStep > 0
                        ) {
                            Text("السابق")
                        }

                        TextButton(
                            onClick = { viewModel.stopGuide() }
                        ) {
                            Text("إنهاء الدليل", color = MaterialTheme.colorScheme.error)
                        }

                        Button(
                            onClick = {
                                if (uiState.currentGuideStep < totalSteps - 1) {
                                    viewModel.nextGuideStep()
                                } else {
                                    viewModel.stopGuide()
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

// --- Tab 3: Verification Checklist Panel ---
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
        // Completion bar
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

        Divider()

        // Errors & Warnings list
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

// --- Autofill Profile Picker Bottom Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillProfileBottomSheet(
    profiles: List<PostalProfile>,
    onDismiss: () -> Unit,
    onProfileSelected: (PostalProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "اختر حساباً للتعبئة التلقائية",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "سيتم إدخال الاسم، اللقب، CCP، والمفتاح فوراً في الوثيقة.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (profiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد ملفات حسابات محفوظة. قم بحفظ حسابات أولاً في قسم إدارة الحسابات.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(profiles) { profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { onProfileSelected(profile) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = profile.profileName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "CCP: ${profile.accountNumber} مفتاح ${profile.accountKey}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "اختيار",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
