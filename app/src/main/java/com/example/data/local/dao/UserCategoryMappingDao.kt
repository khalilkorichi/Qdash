package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.UserCategoryMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCategoryMappingDao {
    @Query("SELECT * FROM user_category_mappings ORDER BY usageCount DESC")
    fun getAllMappings(): Flow<List<UserCategoryMappingEntity>>

    @Query("SELECT * FROM user_category_mappings WHERE normalizedText = :normalizedText LIMIT 1")
    suspend fun getMappingByText(normalizedText: String): UserCategoryMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: UserCategoryMappingEntity): Long

    @Update
    suspend fun updateMapping(mapping: UserCategoryMappingEntity)

    @Delete
    suspend fun deleteMapping(mapping: UserCategoryMappingEntity)

    @Query("DELETE FROM user_category_mappings")
    suspend fun deleteAllMappings()
}
