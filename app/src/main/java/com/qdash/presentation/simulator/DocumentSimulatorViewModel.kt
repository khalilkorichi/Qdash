package com.qdash.presentation.simulator

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qdash.core.preferences.PreferencesManager
import com.qdash.core.utils.AmountConversionEngine
import com.qdash.core.utils.PostalCcpUtils
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.repository.PostalProfileRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DocumentSimulatorUiState(
    val selectedDocType: DocumentType = DocumentType.CHEQUE,
    // Cheque fields
    val chequeAmount: String = "",
    val chequeBeneficiary: String = "",
    val chequeCcp: String = "",
    val chequeKey: String = "",
    val chequePlace: String = "",
    val chequeDate: String = "",
    val chequeNumber: String = "3245101",
    val chequeAccountHolder: String = "",
    val chequeAccountAddress: String = "",
    val chequeWordsLanguage: WordsLanguage = WordsLanguage.ARABIC,
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
    val sfpMotif: String = "",
    val sfpIdDescription: String = "",
    val sfpJustificatifCcp: Boolean = false,
    val sfpAvisCredit: Boolean = false,
    val sfpCarnetCheques: Boolean = false,
    val sfpCodeConfidentiel: Boolean = false,
    val sfpRip: Boolean = false,
    val sfpWordsLanguage: WordsLanguage = WordsLanguage.ARABIC,
    val sfpReleveOperations: Boolean = false,
    val sfpAvecSolde: Boolean = false,
    val sfpNotificationAvoir: Boolean = false,
    val sfpCommandeCarnet: Boolean = false,
    val sfpReleveDu: String = "",
    val sfpReleveAu: String = "",
    val sfpPiType: String = "",
    val sfpPiNumero: String = "",
    val sfpPiDelivreLe: String = "",
    val sfpPiPar: String = "",
    // Quick-fill & UI state
    val isManualEditMode: Boolean = false,
    val lastSavedPlace: String = "",
    val quickFillFeedback: String? = null,
    val savedProfiles: List<PostalProfile> = emptyList(),
    // Interactive guidance
    val guideActive: Boolean = false,
    val currentGuideStep: Int = 0,
    val focusedField: String? = null,
    // Validation
    val errors: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val completionPercentage: Float = 0f,
    val missingFieldLabels: List<String> = emptyList(),
    // Fullscreen rotated editor state
    val isFullscreenViewerOpen: Boolean = false,
    // Panels & navigation context
    val activePanel: SimulatorActivePanel = SimulatorActivePanel.NONE,
    val showReadyAmounts: Boolean = false,
    val landingSelectedProfileId: Long? = null
)

enum class SimulatorActivePanel { NONE, GUIDE, REVIEW }
enum class DocumentType { CHEQUE, SFP01 }
enum class SfpOperationType { VERSEMENT, RETRAIT, VIREMENT }
enum class WordsLanguage { ARABIC, FRENCH }

class DocumentSimulatorViewModel(
    private val postalProfileRepository: PostalProfileRepository,
    private val preferencesManager: PreferencesManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocumentSimulatorUiState())
    val uiState: StateFlow<DocumentSimulatorUiState> = _uiState.asStateFlow()
    private var profilesJob: Job? = null

    init {
        loadSavedPlace()
        loadSavedProfiles()
        setDefaultDates()
    }

    private fun loadSavedPlace() {
        val saved = preferencesManager?.lastPostalPlace?.trim().orEmpty()
        if (saved.isNotBlank()) {
            _uiState.update { it.copy(chequePlace = saved, sfpPlace = saved, lastSavedPlace = saved) }
        }
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
        val now = Date()
        _uiState.update {
            it.copy(
                chequeDate = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(now),
                sfpDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(now)
            )
        }
    }

    private inline fun updateField(crossinline transform: (DocumentSimulatorUiState) -> DocumentSimulatorUiState) {
        _uiState.update { transform(it) }
        validateCurrentDocument()
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
            _uiState.value.savedProfiles.find { it.id == profileId }?.let { profile ->
                autofillFromProfile(profile, PostalProfileRole.SELF)
            }
        }
        validateCurrentDocument()
    }

    // --- Quick-Fill Actions ---
    fun selectPresetAmount(amount: Int) {
        val formatted = if (amount > 0) amount.toString() else ""
        updateField { state ->
            if (state.selectedDocType == DocumentType.CHEQUE) state.copy(chequeAmount = formatted)
            else state.copy(sfpAmount = formatted)
        }
    }

    fun incrementAmount(delta: Int) {
        updateField { state ->
            val currentRaw = if (state.selectedDocType == DocumentType.CHEQUE) state.chequeAmount else state.sfpAmount
            val current = AmountConversionEngine.parseFlexibleAmount(currentRaw) ?: 0.0
            val next = (current + delta).coerceAtLeast(0.0)
            val nextStr = when {
                next <= 0.0 -> ""
                next % 1.0 == 0.0 -> next.toLong().toString()
                else -> String.format(Locale.US, "%.2f", next)
            }
            if (state.selectedDocType == DocumentType.CHEQUE) state.copy(chequeAmount = nextStr)
            else state.copy(sfpAmount = nextStr)
        }
    }

    fun setChequeBeneficiaryMoiMeme() {
        updateField { state ->
            val label = if (state.chequeWordsLanguage == WordsLanguage.FRENCH) "Moi-même" else "لنفسي"
            state.copy(chequeBeneficiary = label)
        }
    }

    fun applyPrimarySelfProfile(): Boolean {
        val profiles = _uiState.value.savedProfiles
        val primary = profiles.firstOrNull { it.defaultRole == PostalProfileRole.SELF && it.isFavorite }
            ?: profiles.firstOrNull { it.defaultRole == PostalProfileRole.SELF }
            ?: profiles.firstOrNull { it.isFavorite }
        if (primary == null) {
            _uiState.update { it.copy(quickFillFeedback = "لا يوجد حساب شخصي محفوظ بعد — أضف حسابك أولاً") }
            return false
        }
        autofillFromProfile(primary, PostalProfileRole.SELF)
        _uiState.update { it.copy(quickFillFeedback = "تمت تعبئة حسابك: ${primary.profileName}") }
        return true
    }

    fun selectQuickPlace(place: String) {
        val trimmed = place.trim()
        if (trimmed.isBlank()) return
        preferencesManager?.lastPostalPlace = trimmed
        updateField { state ->
            if (state.selectedDocType == DocumentType.CHEQUE) {
                state.copy(chequePlace = trimmed, sfpPlace = state.sfpPlace.ifBlank { trimmed }, lastSavedPlace = trimmed)
            } else {
                state.copy(sfpPlace = trimmed, chequePlace = state.chequePlace.ifBlank { trimmed }, lastSavedPlace = trimmed)
            }
        }
    }

    fun setTodayDate() {
        val now = Date()
        updateField { state ->
            if (state.selectedDocType == DocumentType.CHEQUE) {
                state.copy(chequeDate = SimpleDateFormat("dd.MM.yyyy", Locale.US).format(now))
            } else {
                state.copy(sfpDate = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(now))
            }
        }
    }

    fun computeCcpKeyFromCurrent() {
        val state = _uiState.value
        val ccp = if (state.selectedDocType == DocumentType.CHEQUE) state.chequeCcp else state.sfpCcp
        val computedKey = PostalCcpUtils.calculateCcpKey(ccp)
        if (computedKey == null) {
            _uiState.update { it.copy(quickFillFeedback = "أدخل رقم CCP صحيحاً (1 إلى 10 أرقام) لحساب المفتاح") }
            return
        }
        updateField { current ->
            if (current.selectedDocType == DocumentType.CHEQUE) {
                current.copy(chequeKey = computedKey, quickFillFeedback = "تم حساب المفتاح: $computedKey")
            } else {
                current.copy(sfpKey = computedKey, quickFillFeedback = "تم حساب المفتاح: $computedKey")
            }
        }
    }

    fun extractFromRipPaste(rawRip: String): Boolean {
        val info = PostalCcpUtils.extractFromInput(rawRip)
        if (info == null) {
            _uiState.update { it.copy(quickFillFeedback = "تعذر التعرف على رقم CCP أو RIP من النص المدخل") }
            return false
        }
        val feedback = if (info.isRipKeyValid) {
            "تم استخراج الحساب ${info.ccpNumber} / ${info.ccpKey}"
        } else {
            "تم استخراج الحساب ${info.ccpNumber} / ${info.ccpKey} — تنبيه: مفتاح RIP غير متطابق (المتوقع ${info.ripKey})"
        }
        updateField { state ->
            if (state.selectedDocType == DocumentType.CHEQUE) {
                state.copy(chequeCcp = info.ccpNumber, chequeKey = info.ccpKey, quickFillFeedback = feedback)
            } else {
                state.copy(sfpCcp = info.ccpNumber, sfpKey = info.ccpKey, quickFillFeedback = feedback)
            }
        }
        return true
    }

    fun toggleManualEditMode() {
        _uiState.update { it.copy(isManualEditMode = !it.isManualEditMode) }
    }

    fun toggleWordsLanguage() {
        _uiState.update { state ->
            if (state.selectedDocType == DocumentType.CHEQUE) {
                val next = if (state.chequeWordsLanguage == WordsLanguage.ARABIC) WordsLanguage.FRENCH else WordsLanguage.ARABIC
                state.copy(chequeWordsLanguage = next)
            } else {
                val next = if (state.sfpWordsLanguage == WordsLanguage.ARABIC) WordsLanguage.FRENCH else WordsLanguage.ARABIC
                state.copy(sfpWordsLanguage = next)
            }
        }
    }

    fun clearQuickFillFeedback() {
        _uiState.update { it.copy(quickFillFeedback = null) }
    }

    // --- Cheque Field Updates ---
    fun updateChequeAmount(value: String) = updateField { it.copy(chequeAmount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun updateChequeBeneficiary(value: String) = updateField { it.copy(chequeBeneficiary = value) }
    fun updateChequeCcp(value: String) = updateField { it.copy(chequeCcp = value.filter { c -> c.isDigit() }) }
    fun updateChequeKey(value: String) = updateField { it.copy(chequeKey = value.filter { c -> c.isDigit() }.take(2)) }
    fun updateChequeDate(value: String) = updateField { it.copy(chequeDate = value) }
    fun updateChequeNumber(value: String) = updateField { it.copy(chequeNumber = value.filter { c -> c.isDigit() }.take(12)) }
    fun updateChequeAccountHolder(value: String) = updateField { it.copy(chequeAccountHolder = value) }
    fun updateChequeAccountAddress(value: String) = updateField { it.copy(chequeAccountAddress = value) }

    fun updateChequePlace(value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotBlank()) preferencesManager?.lastPostalPlace = trimmed
        updateField { it.copy(chequePlace = value, lastSavedPlace = trimmed.ifBlank { it.lastSavedPlace }) }
    }

    // --- SFP 01 Field Updates ---
    fun updateSfpOperation(type: SfpOperationType) = updateField { it.copy(sfpOperation = type) }
    fun updateSfpCcp(value: String) = updateField { it.copy(sfpCcp = value.filter { c -> c.isDigit() }) }
    fun updateSfpKey(value: String) = updateField { it.copy(sfpKey = value.filter { c -> c.isDigit() }.take(2)) }
    fun updateSfpAmount(value: String) = updateField { it.copy(sfpAmount = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }
    fun updateSfpSenderNom(value: String) = updateField { it.copy(sfpSenderNom = value) }
    fun updateSfpSenderPrenom(value: String) = updateField { it.copy(sfpSenderPrenom = value) }
    fun updateSfpSenderAddress(value: String) = updateField { it.copy(sfpSenderAddress = value) }
    fun updateSfpSenderPhone(value: String) = updateField { it.copy(sfpSenderPhone = value) }
    fun updateSfpBeneficiaryNom(value: String) = updateField { it.copy(sfpBeneficiaryNom = value) }
    fun updateSfpBeneficiaryPrenom(value: String) = updateField { it.copy(sfpBeneficiaryPrenom = value) }
    fun updateSfpBeneficiaryAddress(value: String) = updateField { it.copy(sfpBeneficiaryAddress = value) }
    fun updateSfpBeneficiaryPhone(value: String) = updateField { it.copy(sfpBeneficiaryPhone = value) }
    fun updateSfpDate(value: String) = updateField { it.copy(sfpDate = value) }
    fun updateSfpMotif(value: String) = updateField { it.copy(sfpMotif = value) }
    fun updateSfpIdDescription(value: String) = updateField { it.copy(sfpIdDescription = value) }
    fun updateSfpJustificatifCcp(value: Boolean) = updateField { it.copy(sfpJustificatifCcp = value) }
    fun updateSfpAvisCredit(value: Boolean) = updateField { it.copy(sfpAvisCredit = value) }
    fun updateSfpCarnetCheques(value: Boolean) = updateField { it.copy(sfpCarnetCheques = value) }
    fun updateSfpCodeConfidentiel(value: Boolean) = updateField { it.copy(sfpCodeConfidentiel = value) }
    fun updateSfpRip(value: Boolean) = updateField { it.copy(sfpRip = value) }
    fun updateSfpReleveOperations(value: Boolean) = updateField { it.copy(sfpReleveOperations = value) }
    fun updateSfpAvecSolde(value: Boolean) = updateField { it.copy(sfpAvecSolde = value) }
    fun updateSfpNotificationAvoir(value: Boolean) = updateField { it.copy(sfpNotificationAvoir = value) }
    fun updateSfpCommandeCarnet(value: Boolean) = updateField { it.copy(sfpCommandeCarnet = value) }
    fun updateSfpReleveDu(value: String) = updateField { it.copy(sfpReleveDu = value) }
    fun updateSfpReleveAu(value: String) = updateField { it.copy(sfpReleveAu = value) }
    fun updateSfpPiType(value: String) = updateField { it.copy(sfpPiType = value) }
    fun updateSfpPiNumero(value: String) = updateField { it.copy(sfpPiNumero = value) }
    fun updateSfpPiDelivreLe(value: String) = updateField { it.copy(sfpPiDelivreLe = value) }
    fun updateSfpPiPar(value: String) = updateField { it.copy(sfpPiPar = value) }

    fun updateSfpPlace(value: String) {
        val trimmed = value.trim()
        if (trimmed.isNotBlank()) preferencesManager?.lastPostalPlace = trimmed
        updateField { it.copy(sfpPlace = value, lastSavedPlace = trimmed.ifBlank { it.lastSavedPlace }) }
    }

    // --- Interactive Guide Actions ---
    fun startGuide() {
        _uiState.update {
            it.copy(guideActive = true, currentGuideStep = 0, focusedField = getGuideFieldName(it.selectedDocType, 0))
        }
    }

    fun stopGuide() {
        _uiState.update { it.copy(guideActive = false, focusedField = null) }
    }

    fun nextGuideStep() {
        val totalSteps = if (_uiState.value.selectedDocType == DocumentType.CHEQUE) 6 else 9
        _uiState.update {
            val nextStep = (it.currentGuideStep + 1).coerceAtMost(totalSteps - 1)
            it.copy(currentGuideStep = nextStep, focusedField = getGuideFieldName(it.selectedDocType, nextStep))
        }
    }

    fun prevGuideStep() {
        _uiState.update {
            val prevStep = (it.currentGuideStep - 1).coerceAtLeast(0)
            it.copy(currentGuideStep = prevStep, focusedField = getGuideFieldName(it.selectedDocType, prevStep))
        }
    }

    fun setFocusedField(fieldName: String?) {
        _uiState.update { it.copy(focusedField = fieldName) }
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
            it.copy(activePanel = next, guideActive = if (next == SimulatorActivePanel.GUIDE) it.guideActive else false)
        }
    }

    fun setFullscreenViewerOpen(open: Boolean) {
        _uiState.update { it.copy(isFullscreenViewerOpen = open) }
    }

    fun toggleReadyAmounts(show: Boolean) {
        _uiState.update { it.copy(showReadyAmounts = show) }
    }

    fun selectLandingProfileId(profileId: Long?) {
        _uiState.update { it.copy(landingSelectedProfileId = profileId) }
        if (profileId != null) {
            _uiState.value.savedProfiles.find { it.id == profileId }?.let { profile ->
                autofillFromProfile(profile, PostalProfileRole.SELF)
            }
        }
    }

    // --- Profile Autofill & Persistence ---
    fun autofillFromProfile(profile: PostalProfile, role: PostalProfileRole) {
        updateField { DocumentSimulatorValidator.applyAutofill(it, profile, role) }
    }

    fun savePostalProfile(profile: PostalProfile) {
        viewModelScope.launch {
            if (profile.id == 0L) postalProfileRepository.insertProfile(profile)
            else postalProfileRepository.updateProfile(profile)
        }
    }

    fun deletePostalProfile(profile: PostalProfile) {
        viewModelScope.launch { postalProfileRepository.deleteProfile(profile) }
    }

    fun toggleProfileFavorite(profile: PostalProfile) {
        viewModelScope.launch { postalProfileRepository.updateProfile(profile.copy(isFavorite = !profile.isFavorite)) }
    }

    private fun validateCurrentDocument() {
        val result = DocumentSimulatorValidator.validate(_uiState.value)
        _uiState.update {
            it.copy(
                errors = result.errors,
                warnings = result.warnings,
                completionPercentage = result.completionPercentage,
                missingFieldLabels = result.missingFieldLabels
            )
        }
    }

    private fun getGuideFieldName(docType: DocumentType, step: Int): String? =
        DocumentSimulatorValidator.getGuideFieldName(docType, step)

    private fun getStepIndexForField(docType: DocumentType, fieldName: String): Int =
        DocumentSimulatorValidator.getStepIndexForField(docType, fieldName)
}
