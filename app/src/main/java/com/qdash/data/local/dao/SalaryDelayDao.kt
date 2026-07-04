package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.SalaryDelayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDelayDao {
    @Query("SELECT * FROM salary_delays WHERE salaryId = :salaryId ORDER BY createdAt DESC")
    fun getSalaryDelays(salaryId: Long): Flow<List<SalaryDelayEntity>>

    @Query("SELECT * FROM salary_delays ORDER BY createdAt DESC")
    suspend fun getAllSalaryDelaysOnce(): List<SalaryDelayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryDelay(salaryDelay: SalaryDelayEntity): Long

    @Delete
    suspend fun deleteSalaryDelay(salaryDelay: SalaryDelayEntity)

    @Query("SELECT * FROM salary_delays WHERE id = :id")
    suspend fun getSalaryDelayById(id: Long): SalaryDelayEntity?

    @Update
    suspend fun updateSalaryDelay(salaryDelay: SalaryDelayEntity)
}
