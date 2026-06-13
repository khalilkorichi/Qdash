package com.example.domain.repository

import com.example.data.local.entities.CategoryRuleEntity
import com.example.data.local.entities.UserCategoryMappingEntity
import kotlinx.coroutines.flow.Flow

interface CategorizationRepository {
    fun getAllRules(): Flow<List<CategoryRuleEntity>>
    suspend fun getAllActiveRules(): List<CategoryRuleEntity>
    suspend fun insertRule(rule: CategoryRuleEntity): Long
    suspend fun insertRules(rules: List<CategoryRuleEntity>)
    suspend fun deleteRule(rule: CategoryRuleEntity)

    fun getAllMappings(): Flow<List<UserCategoryMappingEntity>>
    suspend fun getMappingByText(normalizedText: String): UserCategoryMappingEntity?
    suspend fun insertMapping(mapping: UserCategoryMappingEntity): Long
    suspend fun updateMapping(mapping: UserCategoryMappingEntity)
    suspend fun deleteMapping(mapping: UserCategoryMappingEntity)
}
