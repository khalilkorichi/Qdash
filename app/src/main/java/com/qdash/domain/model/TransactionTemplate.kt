package com.qdash.domain.model

import com.qdash.data.local.entities.TransactionTemplateEntity

data class TransactionTemplate(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val transactionType: TransactionType,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val notes: String? = null,
    val iconEmoji: String? = null,
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TransactionDraft(
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val subcategoryId: Long?,
    val accountId: Long,
    val targetAccountId: Long? = null,
    val notes: String? = null,
    val templateId: Long? = null
)

// Mappers
fun TransactionTemplateEntity.toDomain() = TransactionTemplate(
    id = id,
    name = name,
    amount = amount,
    transactionType = TransactionType.valueOf(transactionType),
    accountId = accountId,
    targetAccountId = targetAccountId,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    notes = notes,
    iconEmoji = iconEmoji,
    colorHex = colorHex,
    isPinned = isPinned,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TransactionTemplate.toEntity() = TransactionTemplateEntity(
    id = id,
    name = name,
    amount = amount,
    transactionType = transactionType.name,
    accountId = accountId,
    targetAccountId = targetAccountId,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    notes = notes,
    iconEmoji = iconEmoji,
    colorHex = colorHex,
    isPinned = isPinned,
    usageCount = usageCount,
    lastUsedAt = lastUsedAt,
    createdAt = createdAt,
    updatedAt = updatedAt
)
