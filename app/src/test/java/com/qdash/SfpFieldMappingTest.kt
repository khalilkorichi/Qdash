package com.qdash

import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.repository.PostalProfileRepository
import com.qdash.presentation.simulator.DocumentSimulatorViewModel
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SfpOperationType
import com.qdash.presentation.simulator.WordsLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SfpFieldMappingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DocumentSimulatorViewModel

    private val fakeRepository = object : PostalProfileRepository {
        override fun getAllProfiles(): Flow<List<PostalProfile>> = flowOf(emptyList())
        override fun getFavoriteProfiles(): Flow<List<PostalProfile>> = flowOf(emptyList())
        override fun getProfilesByRole(role: PostalProfileRole): Flow<List<PostalProfile>> = flowOf(emptyList())
        override suspend fun getProfileById(id: Long): PostalProfile? = null
        override suspend fun insertProfile(profile: PostalProfile): Long = 0L
        override suspend fun updateProfile(profile: PostalProfile) {}
        override suspend fun deleteProfile(profile: PostalProfile) {}
        override suspend fun deleteProfileById(id: Long) {}
        override suspend fun getProfilesCount(): Int = 0
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DocumentSimulatorViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateSfpNewStateFields() = runTest(testDispatcher) {
        // Assert initial states
        assertEquals("", viewModel.uiState.value.sfpIdDescription)
        assertFalse(viewModel.uiState.value.sfpJustificatifCcp)
        assertFalse(viewModel.uiState.value.sfpAvisCredit)
        assertFalse(viewModel.uiState.value.sfpCarnetCheques)
        assertFalse(viewModel.uiState.value.sfpCodeConfidentiel)
        assertFalse(viewModel.uiState.value.sfpRip)

        // Update fields
        viewModel.updateSfpIdDescription("ID-12345")
        viewModel.updateSfpJustificatifCcp(true)
        viewModel.updateSfpAvisCredit(true)
        viewModel.updateSfpCarnetCheques(true)
        viewModel.updateSfpCodeConfidentiel(true)
        viewModel.updateSfpRip(true)

        // Assert updated states
        assertEquals("ID-12345", viewModel.uiState.value.sfpIdDescription)
        assertTrue(viewModel.uiState.value.sfpJustificatifCcp)
        assertTrue(viewModel.uiState.value.sfpAvisCredit)
        assertTrue(viewModel.uiState.value.sfpCarnetCheques)
        assertTrue(viewModel.uiState.value.sfpCodeConfidentiel)
        assertTrue(viewModel.uiState.value.sfpRip)
    }

    @Test
    fun testGetStepIndexForFields() {
        viewModel.selectDocumentType(DocumentType.SFP01)

        val getStepIndexForFieldMethod = viewModel.javaClass.getDeclaredMethod(
            "getStepIndexForField",
            DocumentType::class.java,
            String::class.java
        ).apply { isAccessible = true }

        val stepOperation = getStepIndexForFieldMethod.invoke(viewModel, DocumentType.SFP01, "sfpOperation") as Int
        val stepJustificatif = getStepIndexForFieldMethod.invoke(viewModel, DocumentType.SFP01, "sfpJustificatifCcp") as Int
        val stepCcp = getStepIndexForFieldMethod.invoke(viewModel, DocumentType.SFP01, "sfpCcp") as Int
        val stepId = getStepIndexForFieldMethod.invoke(viewModel, DocumentType.SFP01, "sfpIdDescription") as Int

        assertEquals(0, stepOperation)
        assertEquals(0, stepJustificatif)
        assertEquals(1, stepCcp)
        assertEquals(8, stepId)
    }

    @Test
    fun testAutofillSfpProfile() = runTest(testDispatcher) {
        val testProfile = PostalProfile(
            id = 1L,
            profileName = "Test Profile",
            firstName = "John",
            lastName = "Doe",
            fullName = "John Doe",
            accountNumber = "1234567890",
            accountKey = "12",
            address = "Algeria",
            phone = "0612345678"
        )

        // Select SFP01 and set operation to VERSEMENT
        viewModel.selectDocumentType(DocumentType.SFP01)
        viewModel.updateSfpOperation(SfpOperationType.VERSEMENT)

        // Autofill as beneficiary (which populates beneficiary and CCP)
        viewModel.autofillFromProfile(testProfile, PostalProfileRole.BENEFICIARY)

        assertEquals("Doe", viewModel.uiState.value.sfpBeneficiaryNom)
        assertEquals("John", viewModel.uiState.value.sfpBeneficiaryPrenom)
        assertEquals("Algeria", viewModel.uiState.value.sfpBeneficiaryAddress)
        assertEquals("0612345678", viewModel.uiState.value.sfpBeneficiaryPhone)
        assertEquals("1234567890", viewModel.uiState.value.sfpCcp)
        assertEquals("12", viewModel.uiState.value.sfpKey)
    }

    @Test
    fun testQuickFillAmountPresetsAndIncrements() = runTest(testDispatcher) {
        viewModel.selectDocumentType(DocumentType.CHEQUE)
        viewModel.selectPresetAmount(5000)
        assertEquals("5000", viewModel.uiState.value.chequeAmount)

        viewModel.incrementAmount(1000)
        assertEquals("6000", viewModel.uiState.value.chequeAmount)

        viewModel.selectPresetAmount(0)
        assertEquals("", viewModel.uiState.value.chequeAmount)

        viewModel.selectDocumentType(DocumentType.SFP01)
        viewModel.selectPresetAmount(10000)
        assertEquals("10000", viewModel.uiState.value.sfpAmount)
    }

    @Test
    fun testWordsLanguageToggleAndMoiMeme() = runTest(testDispatcher) {
        viewModel.selectDocumentType(DocumentType.CHEQUE)
        assertEquals(WordsLanguage.ARABIC, viewModel.uiState.value.chequeWordsLanguage)

        viewModel.setChequeBeneficiaryMoiMeme()
        assertEquals("لنفسي", viewModel.uiState.value.chequeBeneficiary)

        viewModel.toggleWordsLanguage()
        assertEquals(WordsLanguage.FRENCH, viewModel.uiState.value.chequeWordsLanguage)

        viewModel.setChequeBeneficiaryMoiMeme()
        assertEquals("Moi-même", viewModel.uiState.value.chequeBeneficiary)
    }

    @Test
    fun testComputeCcpKeyAndExtractRipPaste() = runTest(testDispatcher) {
        viewModel.selectDocumentType(DocumentType.CHEQUE)
        viewModel.updateChequeCcp("42339121")
        viewModel.computeCcpKeyFromCurrent()
        assertEquals("98", viewModel.uiState.value.chequeKey)

        // Wrong key generates validation warning
        viewModel.updateChequeKey("11")
        assertTrue(viewModel.uiState.value.warnings.any { it.contains("98") })

        // Pasting full RIP extracts CCP + Key and clears mismatch warning
        assertTrue(viewModel.extractFromRipPaste("00799999004233912175"))
        assertEquals("42339121", viewModel.uiState.value.chequeCcp)
        assertEquals("98", viewModel.uiState.value.chequeKey)
        assertFalse(viewModel.uiState.value.warnings.any { it.contains("لا يطابق") })
    }

    @Test
    fun testSfpNewCheckboxesAndPiFieldsAndValidationWarnings() = runTest(testDispatcher) {
        viewModel.selectDocumentType(DocumentType.SFP01)
        viewModel.updateSfpReleveOperations(true)
        viewModel.updateSfpAvecSolde(true)
        viewModel.updateSfpNotificationAvoir(true)
        viewModel.updateSfpCommandeCarnet(true)

        // Relevé checked without dates produces helpful warning
        assertTrue(viewModel.uiState.value.warnings.any { it.contains("كشف العمليات") })

        viewModel.updateSfpReleveDu("01/01/2026")
        viewModel.updateSfpReleveAu("31/01/2026")
        viewModel.updateSfpPiType("CNI")
        viewModel.updateSfpPiNumero("100200300")
        viewModel.updateSfpPiDelivreLe("01/05/2020")
        viewModel.updateSfpPiPar("Daira Alger")

        val state = viewModel.uiState.value
        assertTrue(state.sfpReleveOperations)
        assertTrue(state.sfpAvecSolde)
        assertTrue(state.sfpNotificationAvoir)
        assertTrue(state.sfpCommandeCarnet)
        assertEquals("01/01/2026", state.sfpReleveDu)
        assertEquals("31/01/2026", state.sfpReleveAu)
        assertEquals("CNI", state.sfpPiType)
        assertEquals("100200300", state.sfpPiNumero)
        assertEquals("01/05/2020", state.sfpPiDelivreLe)
        assertEquals("Daira Alger", state.sfpPiPar)
        assertFalse(state.warnings.any { it.contains("كشف العمليات") })
    }
}
