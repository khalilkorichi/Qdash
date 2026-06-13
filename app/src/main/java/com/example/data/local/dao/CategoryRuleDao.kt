package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.CategoryRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryRuleDao {
    @Query("SELECT * FROM category_rules WHERE isActive = 1 ORDER BY priority DESC")
    suspend fun getAllActiveRules(): List<CategoryRuleEntity>

    @Query("SELECT * FROM category_rules ORDER BY priority DESC")
    fun getAllRules(): Flow<List<CategoryRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CategoryRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<CategoryRuleEntity>)

    @Update
    suspend fun updateRule(rule: CategoryRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CategoryRuleEntity)

    @Query("DELETE FROM category_rules")
    suspend fun deleteAllRules()
}
