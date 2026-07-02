package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.SalaryDistributionEntity
import com.example.data.local.entities.SalaryEnvelopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDistributionDao {

    // --- Distribution ---

    @Query("SELECT * FROM salary_distributions WHERE salaryId = :salaryId LIMIT 1")
    fun getDistributionForSalary(salaryId: Long): Flow<SalaryDistributionEntity?>

    @Query("SELECT * FROM salary_distributions WHERE salaryId = :salaryId LIMIT 1")
    suspend fun getDistributionForSalaryOnce(salaryId: Long): SalaryDistributionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistribution(distribution: SalaryDistributionEntity): Long

    @Update
    suspend fun updateDistribution(distribution: SalaryDistributionEntity)

    @Query("DELETE FROM salary_distributions WHERE id = :id")
    suspend fun deleteDistribution(id: Long)

    // --- Envelopes ---

    @Query("SELECT * FROM salary_envelopes WHERE distributionId = :distributionId ORDER BY `type` ASC")
    fun getEnvelopesForDistribution(distributionId: Long): Flow<List<SalaryEnvelopeEntity>>

    @Query("SELECT * FROM salary_envelopes WHERE distributionId = :distributionId ORDER BY `type` ASC")
    suspend fun getEnvelopesForDistributionOnce(distributionId: Long): List<SalaryEnvelopeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelope(envelope: SalaryEnvelopeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnvelopes(envelopes: List<SalaryEnvelopeEntity>)

    @Update
    suspend fun updateEnvelope(envelope: SalaryEnvelopeEntity)

    @Update
    suspend fun updateEnvelopes(envelopes: List<SalaryEnvelopeEntity>)

    @Query("DELETE FROM salary_envelopes WHERE distributionId = :distributionId")
    suspend fun deleteEnvelopesForDistribution(distributionId: Long)

    @Query("UPDATE salary_envelopes SET linkedCategoryIds = :categoryIds WHERE id = :envelopeId")
    suspend fun updateLinkedCategories(envelopeId: Long, categoryIds: String)
}
