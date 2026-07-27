package com.qdash.presentation.simulator

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qdash.core.ui.components.FinTrackTopBar
import com.qdash.domain.model.PostalProfileRole
import com.qdash.presentation.simulator.components.*
import com.qdash.ui.designsystem.components.FormGrid
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSimulatorScreen(
    viewModel: DocumentSimulatorViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showProfilePicker by remember { mutableStateOf(false) }
    var selectedRoleForAutofill by remember { mutableStateOf<PostalProfileRole?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            FinTrackTopBar(
                title = if (uiState.selectedDocType == DocumentType.CHEQUE) "محاكي الصك البريدي" else "محاكي استمارة SFP 01",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // --- 1. Upper Toolbar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Document switcher
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isCheque = uiState.selectedDocType == DocumentType.CHEQUE
                    Surface(
                        onClick = { viewModel.selectDocumentType(DocumentType.CHEQUE) },
                        color = if (isCheque) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = "صك",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isCheque) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Surface(
                        onClick = { viewModel.selectDocumentType(DocumentType.SFP01) },
                        color = if (!isCheque) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = "استمارة",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!isCheque) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                // Right: Action Icons (Guide, Review)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Guide Button
                    val isGuideActive = uiState.activePanel == SimulatorActivePanel.GUIDE
                    IconButton(
                        onClick = { viewModel.toggleActivePanel(SimulatorActivePanel.GUIDE) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isGuideActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (isGuideActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "دليل خطوة بخطوة",
                            tint = if (isGuideActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    // Review Button
                    val isReviewActive = uiState.activePanel == SimulatorActivePanel.REVIEW
                    BadgedBox(
                        badge = {
                            if (uiState.errors.isNotEmpty()) {
                                Badge { Text(uiState.errors.size.toString()) }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleActivePanel(SimulatorActivePanel.REVIEW) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isReviewActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(1.dp, if (isReviewActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = "مراجعة الوثيقة",
                                tint = if (isReviewActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // --- 2. Collapsible Active Panel (Guide / Review) ---
            AnimatedVisibility(
                visible = uiState.activePanel != SimulatorActivePanel.NONE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        when (uiState.activePanel) {
                            SimulatorActivePanel.GUIDE -> {
                                EducationalGuidePanel(
                                    uiState = uiState,
                                    onStartGuide = { viewModel.startGuide() },
                                    onPrevStep = { viewModel.prevGuideStep() },
                                    onNextStep = { viewModel.nextGuideStep() },
                                    onStopGuide = { viewModel.stopGuide() }
                                )
                            }
                            SimulatorActivePanel.REVIEW -> {
                                ValidationChecklistPanel(
                                    uiState = uiState
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            // --- 3. Document Simulator Area ---
            if (uiState.selectedDocType == DocumentType.CHEQUE) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    ChequeVisualView(
                        chequeAmount = uiState.chequeAmount,
                        chequeBeneficiary = uiState.chequeBeneficiary,
                        chequePlace = uiState.chequePlace,
                        chequeDate = uiState.chequeDate,
                        chequeCcp = uiState.chequeCcp,
                        chequeKey = uiState.chequeKey,
                        onFieldTap = { viewModel.setFocusedField(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- 4. Scrollable Form Inputs Area ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FillFormPanel(
                            uiState = uiState,
                            onChequeAmountChange = { viewModel.updateChequeAmount(it) },
                            onChequeBeneficiaryChange = { viewModel.updateChequeBeneficiary(it) },
                            onChequeCcpChange = { viewModel.updateChequeCcp(it) },
                            onChequeKeyChange = { viewModel.updateChequeKey(it) },
                            onChequePlaceChange = { viewModel.updateChequePlace(it) },
                            onChequeDateChange = { viewModel.updateChequeDate(it) },
                            onToggleReadyAmounts = { viewModel.toggleReadyAmounts(it) },
                            onAutofillClick = { role ->
                                selectedRoleForAutofill = role
                                showProfilePicker = true
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // SFP 01 Layout
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(8.dp))
                        ) {
                            FormGrid(
                                uiState = uiState,
                                onCcpChange = { viewModel.updateSfpCcp(it) },
                                onKeyChange = { viewModel.updateSfpKey(it) },
                                onAmountChange = { viewModel.updateSfpAmount(it) },
                                onSenderNomChange = { viewModel.updateSfpSenderNom(it) },
                                onSenderPrenomChange = { viewModel.updateSfpSenderPrenom(it) },
                                onSenderAddressChange = { viewModel.updateSfpSenderAddress(it) },
                                onSenderPhoneChange = { viewModel.updateSfpSenderPhone(it) },
                                onBeneficiaryNomChange = { viewModel.updateSfpBeneficiaryNom(it) },
                                onBeneficiaryPrenomChange = { viewModel.updateSfpBeneficiaryPrenom(it) },
                                onBeneficiaryAddressChange = { viewModel.updateSfpBeneficiaryAddress(it) },
                                onBeneficiaryPhoneChange = { viewModel.updateSfpBeneficiaryPhone(it) },
                                onPlaceChange = { viewModel.updateSfpPlace(it) },
                                onDateChange = { viewModel.updateSfpDate(it) },
                                onIdDescriptionChange = { viewModel.updateSfpIdDescription(it) },
                                onOperationChange = { viewModel.updateSfpOperation(it) },
                                onJustificatifCcpChange = { viewModel.updateSfpJustificatifCcp(it) },
                                onAvisCreditChange = { viewModel.updateSfpAvisCredit(it) },
                                onCarnetChequesChange = { viewModel.updateSfpCarnetCheques(it) },
                                onCodeConfidentielChange = { viewModel.updateSfpCodeConfidentiel(it) },
                                onRipChange = { viewModel.updateSfpRip(it) },
                                focusedField = uiState.focusedField,
                                onFieldFocus = { viewModel.setFocusedField(it) }
                            )
                        }

                        FillFormPanel(
                            uiState = uiState,
                            onChequeAmountChange = { viewModel.updateChequeAmount(it) },
                            onChequeBeneficiaryChange = { viewModel.updateChequeBeneficiary(it) },
                            onChequeCcpChange = { viewModel.updateChequeCcp(it) },
                            onChequeKeyChange = { viewModel.updateChequeKey(it) },
                            onChequePlaceChange = { viewModel.updateChequePlace(it) },
                            onChequeDateChange = { viewModel.updateChequeDate(it) },
                            onToggleReadyAmounts = { viewModel.toggleReadyAmounts(it) },
                            onAutofillClick = { role ->
                                selectedRoleForAutofill = role
                                showProfilePicker = true
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
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

    if (uiState.showReadyAmounts) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleReadyAmounts(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "المبالغ الجاهزة (التعبئة السريعة)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                AmountCheatSheetPanel(
                    onAmountSelected = { amount ->
                        if (uiState.selectedDocType == DocumentType.CHEQUE) {
                            viewModel.updateChequeAmount(amount)
                        } else {
                            viewModel.updateSfpAmount(amount)
                        }
                        viewModel.toggleReadyAmounts(false)
                    }
                )
            }
        }
    }
}
