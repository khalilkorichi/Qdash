package com.example.domain.repository

import com.example.domain.model.TransactionTemplate
import kotlinx.coroutines.flow.Flow

interface TransactionTemplateRepository {
    suspend fun insertTemplate(template: TransactionTemplate): Long
    suspend fun updateTemplate(template: TransactionTemplate)
    suspend fun deleteTemplate(id: Long)
    suspend fun getTemplateById(id: Long): TransactionTemplate?
    fun getAllTemplates(): Flow<List<TransactionTemplate>>
    fun getPinnedTemplates(): Flow<List<TransactionTemplate>>
    fun getFrequentTemplates(): Flow<List<TransactionTemplate>>
    fun searchTemplates(query: String): Flow<List<TransactionTemplate>>
    suspend fun incrementUsage(id: Long)
    suspend fun togglePin(id: Long, isPinned: Boolean)
}
