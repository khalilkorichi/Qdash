package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.SalaryDelayEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalaryDelayDao {
    @Query("SELECT * FROM salary_delays WHERE salaryId = :salaryId ORDER BY createdAt DESC")
    fun getSalaryDelays(salaryId: Long): Flow<List<SalaryDelayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryDelay(salaryDelay: SalaryDelayEntity): Long

    @Delete
    suspend fun deleteSalaryDelay(salaryDelay: SalaryDelayEntity)
}
