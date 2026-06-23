package com.example.presentation.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.PostalProfile
import com.example.domain.model.PostalProfileRole
import com.example.domain.repository.PostalProfileRepository
import com.example.domain.usecase.simulator.AmountConversionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DocumentSimulatorUiState(
    val selectedDocType: DocumentType = DocumentType.CHEQUE,
    
    // Cheque fields
    val chequeAmount: String = "",
    val chequeBeneficiary: String = "",
    val chequeCcp: String = "",
    val chequeKey: String = "",
    val chequePlace: String = "",
    val chequeDate: String = "",
    
    // SFP 01 fields
    val sfpOperation: SfpOperationType = SfpOperationType.VERSEMENT,
    val sfpCcp: String = "",
    val sfpKey: String = "",
    val sfpAmount: String = "",
    val sfpSenderNom: String = "",
    val sfpSenderPrenom: String = "",
    val sfpSenderAddress: String = "",
    val sfpSenderPhone: String = "",
    val sfpBeneficiaryNom: String = "",
    val sfpBeneficiaryPrenom: String = "",
    val sfpBeneficiaryAddress: String = "",
    val sfpBeneficiaryPhone: String = "",
    val sfpPlace: String = "",
    val sfpDate: String = "",
    val sfpIdDescription: String = "",
    val sfpJustificatifCcp: Boolean = false,
    val sfpAvisCredit: Boolean = false,
    val sfpCarnetCheques: Boolean = false,
    val sfpCodeConfidentiel: Boolean = false,
    val sfpRip: Boolean = false,
    
    // Profile list
    val savedProfiles: List<PostalProfile> = emptyList(),
    
    // Interactive guidance
    val guideActive: Boolean = false,
    val currentGuideStep: Int = 0,
    val focusedField: String? = null,
    
    // Validation
    val errors: Map<String, String> = emptyMap(), // fieldName -> message
    val warnings: List<String> = emptyList(),
    val completionPercentage: Float = 0f,
    
    // New states
    val activePanel: SimulatorActivePanel = SimulatorActivePanel.NONE,
    val showReadyAmounts: Boolean = false,
    val landingSelectedProfileId: Long? = null
)

enum class SimulatorActivePanel {
    NONE, GUIDE, REVIEW
}

enum class DocumentType {
    CHEQUE, SFP01
}

enum class SfpOperationType {
    VERSEMENT, RETRAIT, VIREMENT
}

class DocumentSimulatorViewModel(
    private val postalProfileRepository: PostalProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentSimulatorUiState())
    val uiState: StateFlow<DocumentSimulatorUiState> = _uiState.asStateFlow()
    private var profilesJob: Job? = null

    init {
        loadSavedProfiles()
        setDefaultDates()
    }

    private fun loadSavedProfiles() {
        profilesJob?.cancel()
        profilesJob = viewModelScope.launch {
            postalProfileRepository.getAllProfiles().collect { list ->
                _uiState.update { it.copy(savedProfiles = list) }
                validateCurrentDocument()
            }
        }
    }

    private fun setDefaultDates() {
        val today = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(java.util.Date())
        _uiState.update { 
            it.copy(
                chequeDate = today,
                sfpDate = today
            )
        }
    }

    fun selectDocumentType(type: DocumentType) {
        _uiState.update { 
            it.copy(
                selectedDocType = type,
                focusedField = null,
                guideActive = false,
                currentGuideStep = 0,
                activePanel = SimulatorActivePanel.NONE,
                showReadyAmounts = false
            ) 
        }
        val profileId = _uiState.value.landingSelectedProfileId
        if (profileId != null) {
            val profile = _uiState.value.savedProfiles.find { it.id == profileId }
            if (profile != null) {
                autofillFromProfile(profile, PostalProfileRole.SELF)
            }
        }
        validateCurrentDocument()
    }

    // --- Cheque Field Updates ---
    fun updateChequeAmount(value: String) {
        _uiState.update { it.copy(chequeAmount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
        validateCurrentDocument()
    }

    fun updateChequeBeneficiary(value: String) {
        _uiState.update { it.copy(chequeBeneficiary = value) }
        validateCurrentDocument()
    }

    fun updateChequeCcp(value: String) {
        _uiState.update { it.copy(chequeCcp = value.filter { it.isDigit() }) }
        validateCurrentDocument()
    }

    fun updateChequeKey(value: String) {
        _uiState.update { it.copy(chequeKey = value.filter { it.isDigit() }.take(2)) }
        validateCurrentDocument()
    }

    fun updateChequePlace(value: String) {
        _uiState.update { it.copy(chequePlace = value) }
        validateCurrentDocument()
    }

    fun updateChequeDate(value: String) {
        _uiState.update { it.copy(chequeDate = value) }
        validateCurrentDocument()
    }

    // --- SFP 01 Field Updates ---
    fun updateSfpOperation(type: SfpOperationType) {
        _uiState.update { it.copy(sfpOperation = type) }
        validateCurrentDocument()
    }

    fun updateSfpCcp(value: String) {
        _uiState.update { it.copy(sfpCcp = value.filter { it.isDigit() }) }
        validateCurrentDocument()
    }

    fun updateSfpKey(value: String) {
        _uiState.update { it.copy(sfpKey = value.filter { it.isDigit() }.take(2)) }
        validateCurrentDocument()
    }

    fun updateSfpAmount(value: String) {
        _uiState.update { it.copy(sfpAmount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
        validateCurrentDocument()
    }

    fun updateSfpSenderNom(value: String) {
        _uiState.update { it.copy(sfpSenderNom = value) }
        validateCurrentDocument()
    }

    fun updateSfpSenderPrenom(value: String) {
        _uiState.update { it.copy(sfpSenderPrenom = value) }
        validateCurrentDocument()
    }

    fun updateSfpSenderAddress(value: String) {
        _uiState.update { it.copy(sfpSenderAddress = value) }
        validateCurrentDocument()
    }

    fun updateSfpSenderPhone(value: String) {
        _uiState.update { it.copy(sfpSenderPhone = value) }
        validateCurrentDocument()
    }

    fun updateSfpBeneficiaryNom(value: String) {
        _uiState.update { it.copy(sfpBeneficiaryNom = value) }
        validateCurrentDocument()
    }

    fun updateSfpBeneficiaryPrenom(value: String) {
        _uiState.update { it.copy(sfpBeneficiaryPrenom = value) }
        validateCurrentDocument()
    }

    fun updateSfpBeneficiaryAddress(value: String) {
        _uiState.update { it.copy(sfpBeneficiaryAddress = value) }
        validateCurrentDocument()
    }

    fun updateSfpBeneficiaryPhone(value: String) {
        _uiState.update { it.copy(sfpBeneficiaryPhone = value) }
        validateCurrentDocument()
    }

    fun updateSfpPlace(value: String) {
        _uiState.update { it.copy(sfpPlace = value) }
        validateCurrentDocument()
    }

    fun updateSfpDate(value: String) {
        _uiState.update { it.copy(sfpDate = value) }
        validateCurrentDocument()
    }

    fun updateSfpIdDescription(value: String) {
        _uiState.update { it.copy(sfpIdDescription = value) }
        validateCurrentDocument()
    }

    fun updateSfpJustificatifCcp(value: Boolean) {
        _uiState.update { it.copy(sfpJustificatifCcp = value) }
        validateCurrentDocument()
    }

    fun updateSfpAvisCredit(value: Boolean) {
        _uiState.update { it.copy(sfpAvisCredit = value) }
        validateCurrentDocument()
    }

    fun updateSfpCarnetCheques(value: Boolean) {
        _uiState.update { it.copy(sfpCarnetCheques = value) }
        validateCurrentDocument()
    }

    fun updateSfpCodeConfidentiel(value: Boolean) {
        _uiState.update { it.copy(sfpCodeConfidentiel = value) }
        validateCurrentDocument()
    }

    fun updateSfpRip(value: Boolean) {
        _uiState.update { it.copy(sfpRip = value) }
        validateCurrentDocument()
    }

    // --- Interactive Guide Actions ---
    fun startGuide() {
        _uiState.update { 
            it.copy(
                guideActive = true,
                currentGuideStep = 0,
                focusedField = getGuideFieldName(it.selectedDocType, 0)
            ) 
        }
    }

    fun stopGuide() {
        _uiState.update { it.copy(guideActive = false, focusedField = null) }
    }

    fun nextGuideStep() {
        val totalSteps = if (_uiState.value.selectedDocType == DocumentType.CHEQUE) 6 else 9
        _uiState.update {
            val nextStep = (it.currentGuideStep + 1).coerceAtMost(totalSteps - 1)
            it.copy(
                currentGuideStep = nextStep,
                focusedField = getGuideFieldName(it.selectedDocType, nextStep)
            )
        }
    }

    fun prevGuideStep() {
        _uiState.update {
            val prevStep = (it.currentGuideStep - 1).coerceAtLeast(0)
            it.copy(
                currentGuideStep = prevStep,
                focusedField = getGuideFieldName(it.selectedDocType, prevStep)
            )
        }
    }

    fun setFocusedField(fieldName: String?) {
        _uiState.update { it.copy(focusedField = fieldName) }
        
        // If guide is active, synchronize the guide step with the focused field
        if (fieldName != null) {
            val stepIndex = getStepIndexForField(_uiState.value.selectedDocType, fieldName)
            if (stepIndex != -1) {
                _uiState.update { it.copy(currentGuideStep = stepIndex, guideActive = true) }
            }
        }
    }

    fun toggleActivePanel(panel: SimulatorActivePanel) {
        _uiState.update { 
            val next = if (it.activePanel == panel) SimulatorActivePanel.NONE else panel
            it.copy(
                activePanel = next,
                guideActive = if (next == SimulatorActivePanel.GUIDE) it.guideActive else false
            )
        }
    }

    fun toggleReadyAmounts(show: Boolean) {
        _uiState.update { it.copy(showReadyAmounts = show) }
    }

    fun selectLandingProfileId(profileId: Long?) {
        _uiState.update { it.copy(landingSelectedProfileId = profileId) }
        if (profileId != null) {
            val profile = _uiState.value.savedProfiles.find { it.id == profileId }
            if (profile != null) {
                autofillFromProfile(profile, PostalProfileRole.SELF)
            }
        }
    }

    // --- Profile Autofill Logic ---
    fun autofillFromProfile(profile: PostalProfile, role: PostalProfileRole) {
        if (_uiState.value.selectedDocType == DocumentType.CHEQUE) {
            when (role) {
                PostalProfileRole.SELF -> {
                    _uiState.update {
                        it.copy(
                            chequeCcp = profile.accountNumber,
                            chequeKey = profile.accountKey,
                            chequeBeneficiary = "لنفسي"
                        )
                    }
                }
                PostalProfileRole.BENEFICIARY -> {
                    _uiState.update {
                        it.copy(
                            chequeBeneficiary = "${profile.lastName} ${profile.firstName} ccp ${profile.accountNumber} clé ${profile.accountKey}"
                        )
                    }
                }
                else -> {}
            }
        } else {
            // SFP 01
            when (role) {
                PostalProfileRole.SELF, PostalProfileRole.SENDER -> {
                    _uiState.update {
                        it.copy(
                            sfpSenderNom = profile.lastName,
                            sfpSenderPrenom = profile.firstName,
                            sfpSenderAddress = profile.address ?: "",
                            sfpSenderPhone = profile.phone ?: "",
                            sfpCcp = if (it.sfpOperation != SfpOperationType.VERSEMENT) profile.accountNumber else it.sfpCcp,
                            sfpKey = if (it.sfpOperation != SfpOperationType.VERSEMENT) profile.accountKey else it.sfpKey
                        )
                    }
                }
                PostalProfileRole.BENEFICIARY -> {
                    _uiState.update {
                        it.copy(
                            sfpBeneficiaryNom = profile.lastName,
                            sfpBeneficiaryPrenom = profile.firstName,
                            sfpBeneficiaryAddress = profile.address ?: "",
                            sfpBeneficiaryPhone = profile.phone ?: "",
                            sfpCcp = if (it.sfpOperation == SfpOperationType.VERSEMENT) profile.accountNumber else it.sfpCcp,
                            sfpKey = if (it.sfpOperation == SfpOperationType.VERSEMENT) profile.accountKey else it.sfpKey
                        )
                    }
                }
            }
        }
        validateCurrentDocument()
    }

    // --- Profile Operations ---
    fun savePostalProfile(profile: PostalProfile) {
        viewModelScope.launch {
            if (profile.id == 0L) {
                postalProfileRepository.insertProfile(profile)
            } else {
                postalProfileRepository.updateProfile(profile)
            }
        }
    }

    fun deletePostalProfile(profile: PostalProfile) {
        viewModelScope.launch {
            postalProfileRepository.deleteProfile(profile)
        }
    }

    fun toggleProfileFavorite(profile: PostalProfile) {
        viewModelScope.launch {
            postalProfileRepository.updateProfile(profile.copy(isFavorite = !profile.isFavorite))
        }
    }

    // --- Validation Engine ---
    private fun validateCurrentDocument() {
        val state = _uiState.value
        val errMap = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()
        var filledCount = 0
        var totalCount = 0

        if (state.selectedDocType == DocumentType.CHEQUE) {
            totalCount = 6

            // 1. Amount
            if (state.chequeAmount.isBlank()) {
                errMap["chequeAmount"] = "المبلغ مطلوب بالأرقام"
            } else {
                val amt = parseAmount(state.chequeAmount)
                if (amt == null || amt <= 0) {
                    errMap["chequeAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                } else {
                    filledCount++
                    if (state.chequeAmount.contains(Regex("[.,]")) && !state.chequeAmount.endsWith("00")) {
                        warnings.add("تنبيه: الصك يملأ بالدينار وليس السنتيم، تأكد من حذف الصفرين الإضافيين!")
                    }
                }
            }

            // 2. Beneficiary
            if (state.chequeBeneficiary.isBlank()) {
                errMap["chequeBeneficiary"] = "اسم المستفيد مطلوب (لأمر)"
            } else {
                filledCount++
            }

            // 3. CCP Account
            if (state.chequeCcp.isBlank()) {
                errMap["chequeCcp"] = "رقم الحساب البريدي (CCP) مطلوب"
            } else {
                filledCount++
            }

            // 4. Clé
            if (state.chequeKey.isBlank()) {
                errMap["chequeKey"] = "المفتاح مطلوب"
            } else if (state.chequeKey.length != 2) {
                errMap["chequeKey"] = "المفتاح يتكون من رقمين"
            } else {
                filledCount++
            }

            // 5. Place
            if (state.chequePlace.isBlank()) {
                errMap["chequePlace"] = "المكان مطلوب"
            } else {
                filledCount++
            }

            // 6. Date
            if (state.chequeDate.isBlank()) {
                errMap["chequeDate"] = "التاريخ مطلوب"
            } else {
                filledCount++
            }

        } else {
            // SFP 01
            val isVersement = state.sfpOperation == SfpOperationType.VERSEMENT
            totalCount = if (isVersement) 10 else 7

            // 1. Operation type is implicitly selected (always VERSEMENT/RETRAIT/VIREMENT)
            filledCount++

            // 2. CCP
            if (state.sfpCcp.isBlank()) {
                errMap["sfpCcp"] = "رقم الحساب مطلوب"
            } else {
                filledCount++
            }

            // 3. Key
            if (state.sfpKey.isBlank()) {
                errMap["sfpKey"] = "المفتاح مطلوب"
            } else if (state.sfpKey.length != 2) {
                errMap["sfpKey"] = "المفتاح يتكون من رقمين"
            } else {
                filledCount++
            }

            // 4. Amount
            if (state.sfpAmount.isBlank()) {
                errMap["sfpAmount"] = "المبلغ مطلوب"
            } else {
                val amt = parseAmount(state.sfpAmount)
                if (amt == null || amt <= 0) {
                    errMap["sfpAmount"] = "أدخل مبلغاً صحيحاً أكبر من الصفر"
                } else {
                    filledCount++
                }
            }

            // 5. Sender Name
            if (state.sfpSenderNom.isBlank() || state.sfpSenderPrenom.isBlank()) {
                errMap["sfpSenderName"] = "معلومات المرسل (الاسم واللقب) مطلوبة"
            } else {
                filledCount++
            }

            // 6. Sender Address
            if (state.sfpSenderAddress.isBlank()) {
                errMap["sfpSenderAddress"] = "عنوان المرسل مطلوب"
            } else {
                filledCount++
            }

            // 7. Place / Date
            if (state.sfpPlace.isBlank() || state.sfpDate.isBlank()) {
                errMap["sfpPlaceDate"] = "المكان والتاريخ مطلوبان"
            } else {
                filledCount++
            }

            // For Versement, Beneficiary info is required
            if (isVersement) {
                if (state.sfpBeneficiaryNom.isBlank() || state.sfpBeneficiaryPrenom.isBlank()) {
                    errMap["sfpBeneficiaryName"] = "معلومات المستفيد (المرسل إليه) مطلوبة للدفع"
                } else {
                    filledCount++
                }

                if (state.sfpBeneficiaryAddress.isBlank()) {
                    warnings.add("تنبيه: يفضل ملء عنوان المستفيد لتسهيل مطابقة الحوالة.")
                } else {
                    filledCount++
                }
                
                // Placeholder to represent beneficiary requirement
                filledCount++ 
            }
        }

        val percentage = (filledCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
        _uiState.update { 
            it.copy(
                errors = errMap,
                warnings = warnings,
                completionPercentage = percentage
            )
        }
    }

    private fun parseAmount(value: String): Double? {
        val compact = value.trim().replace(" ", "")
        if (compact.isBlank()) return null
        val normalized = if (compact.count { it == ',' } == 1 && !compact.contains('.')) {
            compact.replace(',', '.')
        } else {
            compact.replace(",", "")
        }
        return normalized.toDoubleOrNull()
    }

    private fun getGuideFieldName(docType: DocumentType, step: Int): String? {
        return if (docType == DocumentType.CHEQUE) {
            when (step) {
                0 -> "chequeCcp"
                1 -> "chequeAmount"
                2 -> "chequeBeneficiary"
                3 -> "chequePlace"
                4 -> "chequeDate"
                5 -> "chequeSignature"
                else -> null
            }
        } else {
            when (step) {
                0 -> "sfpOperation"
                1 -> "sfpCcp"
                2 -> "sfpAmount"
                3 -> "sfpSenderName"
                4 -> "sfpSenderAddress"
                5 -> "sfpBeneficiaryName"
                6 -> "sfpBeneficiaryAddress"
                7 -> "sfpPlaceDate"
                8 -> "sfpSignature"
                else -> null
            }
        }
    }

    private fun getStepIndexForField(docType: DocumentType, fieldName: String): Int {
        return if (docType == DocumentType.CHEQUE) {
            when (fieldName) {
                "chequeCcp", "chequeKey" -> 0
                "chequeAmount" -> 1
                "chequeBeneficiary" -> 2
                "chequePlace" -> 3
                "chequeDate" -> 4
                "chequeSignature" -> 5
                else -> -1
            }
        } else {
            when (fieldName) {
                "sfpOperation", "sfpJustificatifCcp", "sfpAvisCredit", "sfpCarnetCheques", "sfpCodeConfidentiel", "sfpRip" -> 0
                "sfpCcp", "sfpKey" -> 1
                "sfpAmount" -> 2
                "sfpSenderNom", "sfpSenderPrenom", "sfpSenderName" -> 3
                "sfpSenderAddress", "sfpSenderPhone" -> 4
                "sfpBeneficiaryNom", "sfpBeneficiaryPrenom", "sfpBeneficiaryName" -> 5
                "sfpBeneficiaryAddress", "sfpBeneficiaryPhone" -> 6
                "sfpPlace", "sfpDate" -> 7
                "sfpSignature", "sfpIdDescription" -> 8
                else -> -1
            }
        }
    }
}
