package com.qdash.data.repository

import com.qdash.data.local.dao.CategoryRuleDao
import com.qdash.data.local.dao.UserCategoryMappingDao
import com.qdash.data.local.entities.CategoryRuleEntity
import com.qdash.data.local.entities.UserCategoryMappingEntity
import com.qdash.domain.repository.CategorizationRepository
import kotlinx.coroutines.flow.Flow

class CategorizationRepositoryImpl(
    private val categoryRuleDao: CategoryRuleDao,
    private val userCategoryMappingDao: UserCategoryMappingDao
) : CategorizationRepository {

    override fun getAllRules(): Flow<List<CategoryRuleEntity>> {
        return categoryRuleDao.getAllRules()
    }

    override suspend fun getAllActiveRules(): List<CategoryRuleEntity> {
        return categoryRuleDao.getAllActiveRules()
    }

    override suspend fun insertRule(rule: CategoryRuleEntity): Long {
        return categoryRuleDao.insertRule(rule)
    }

    override suspend fun insertRules(rules: List<CategoryRuleEntity>) {
        categoryRuleDao.insertRules(rules)
    }

    override suspend fun deleteRule(rule: CategoryRuleEntity) {
        categoryRuleDao.deleteRule(rule)
    }

    override fun getAllMappings(): Flow<List<UserCategoryMappingEntity>> {
        return userCategoryMappingDao.getAllMappings()
    }

    override suspend fun getMappingByText(normalizedText: String): UserCategoryMappingEntity? {
        return userCategoryMappingDao.getMappingByText(normalizedText)
    }

    override suspend fun insertMapping(mapping: UserCategoryMappingEntity): Long {
        return userCategoryMappingDao.insertMapping(mapping)
    }

    override suspend fun updateMapping(mapping: UserCategoryMappingEntity) {
        userCategoryMappingDao.updateMapping(mapping)
    }

    override suspend fun deleteMapping(mapping: UserCategoryMappingEntity) {
        userCategoryMappingDao.deleteMapping(mapping)
    }
}
