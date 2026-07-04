package com.qdash.domain.repository

import com.qdash.data.local.entities.CategoryRuleEntity
import com.qdash.data.local.entities.UserCategoryMappingEntity
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
