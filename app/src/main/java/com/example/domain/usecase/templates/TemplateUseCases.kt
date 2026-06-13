package com.example.domain.usecase.templates

import com.example.domain.model.TransactionDraft
import com.example.domain.model.TransactionTemplate
import com.example.domain.model.Transaction
import com.example.domain.repository.TransactionTemplateRepository
import kotlinx.coroutines.flow.Flow

class GetAllTemplatesUseCase(private val repository: TransactionTemplateRepository) {
    operator fun invoke(): Flow<List<TransactionTemplate>> = repository.getAllTemplates()
}

class GetPinnedTemplatesUseCase(private val repository: TransactionTemplateRepository) {
    operator fun invoke(): Flow<List<TransactionTemplate>> = repository.getPinnedTemplates()
}

class GetFrequentTemplatesUseCase(private val repository: TransactionTemplateRepository) {
    operator fun invoke(): Flow<List<TransactionTemplate>> = repository.getFrequentTemplates()
}

class CreateTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(template: TransactionTemplate): Long = repository.insertTemplate(template)
}

class UpdateTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(template: TransactionTemplate) = repository.updateTemplate(template)
}

class DeleteTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteTemplate(id)
}

class UseTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(templateId: Long): TransactionDraft? {
        val template = repository.getTemplateById(templateId) ?: return null
        repository.incrementUsage(templateId)
        return TransactionDraft(
            amount = template.amount,
            type = template.transactionType,
            categoryId = template.categoryId,
            subcategoryId = template.subcategoryId,
            accountId = template.accountId,
            targetAccountId = template.targetAccountId,
            notes = template.notes,
            templateId = template.id
        )
    }
}

class SaveTransactionAsTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(
        name: String,
        transaction: Transaction,
        iconEmoji: String? = null,
        colorHex: String? = null
    ): Long {
        val template = TransactionTemplate(
            name = name,
            amount = transaction.amount,
            transactionType = transaction.type,
            accountId = transaction.accountId,
            targetAccountId = transaction.toAccountId,
            categoryId = transaction.categoryId,
            notes = transaction.note,
            iconEmoji = iconEmoji ?: "📝",
            colorHex = colorHex ?: "#6C63FF",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return repository.insertTemplate(template)
    }
}

class TogglePinTemplateUseCase(private val repository: TransactionTemplateRepository) {
    suspend operator fun invoke(id: Long, isPinned: Boolean) = repository.togglePin(id, isPinned)
}

class SearchTemplatesUseCase(private val repository: TransactionTemplateRepository) {
    operator fun invoke(query: String): Flow<List<TransactionTemplate>> = repository.searchTemplates(query)
}
