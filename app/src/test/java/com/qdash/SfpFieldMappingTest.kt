package com.qdash

import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.repository.PostalProfileRepository
import com.qdash.presentation.simulator.DocumentSimulatorViewModel
import com.qdash.presentation.simulator.DocumentType
import com.qdash.presentation.simulator.SfpOperationType
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
}
