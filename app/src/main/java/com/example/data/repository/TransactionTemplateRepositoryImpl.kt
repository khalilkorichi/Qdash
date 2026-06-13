package com.example.data.repository

import com.example.data.local.dao.TransactionTemplateDao
import com.example.domain.model.TransactionTemplate
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.repository.TransactionTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TransactionTemplateRepositoryImpl(
    private val templateDao: TransactionTemplateDao
) : TransactionTemplateRepository {

    override suspend fun insertTemplate(template: TransactionTemplate): Long =
        withContext(Dispatchers.IO) {
            templateDao.insertTemplate(template.toEntity())
        }

    override suspend fun updateTemplate(template: TransactionTemplate) =
        withContext(Dispatchers.IO) {
            templateDao.updateTemplate(template.toEntity())
        }

    override suspend fun deleteTemplate(id: Long) =
        withContext(Dispatchers.IO) {
            templateDao.deleteTemplate(id)
        }

    override suspend fun getTemplateById(id: Long): TransactionTemplate? =
        withContext(Dispatchers.IO) {
            templateDao.getTemplateById(id)?.toDomain()
        }

    override fun getAllTemplates(): Flow<List<TransactionTemplate>> =
        templateDao.getAllTemplates()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun getPinnedTemplates(): Flow<List<TransactionTemplate>> =
        templateDao.getPinnedTemplates()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun getFrequentTemplates(): Flow<List<TransactionTemplate>> =
        templateDao.getFrequentTemplates()
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override fun searchTemplates(query: String): Flow<List<TransactionTemplate>> =
        templateDao.searchTemplates(query)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    override suspend fun incrementUsage(id: Long) =
        withContext(Dispatchers.IO) {
            templateDao.incrementUsage(id, System.currentTimeMillis())
        }

    override suspend fun togglePin(id: Long, isPinned: Boolean) =
        withContext(Dispatchers.IO) {
            templateDao.togglePin(id, isPinned, System.currentTimeMillis())
        }
}
