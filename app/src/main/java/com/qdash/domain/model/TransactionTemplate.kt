package com.qdash.domain.model

import com.qdash.data.local.entities.TransactionTemplateEntity
import com.qdash.domain.model.common.*

data class TransactionTemplate(
    override val /* contract */ id: Long = 0,
    override val /* contract */ name: String,
    override val /* contract */ amount: Double,
    val transactionType: TransactionType,
    override val /* contract */ accountId: Long,
    val targetAccountId: Long? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    override val /* contract */ notes: String? = null,
    val iconEmoji: String? = null,
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    override val /* contract */ createdAt: Long = System.currentTimeMillis(),
    override val /* contract */ updatedAt: Long = System.currentTimeMillis()
) : Identifiable, Nameable, AccountLinkedAmount, NotesHolder, Timestamped, Updatable

data class TransactionDraft(
    override val /* contract */ amount: Double,
    override val /* contract */ type: TransactionType,
    val categoryId: Long?,
    val subcategoryId: Long?,
    override val /* contract */ accountId: Long,
    val targetAccountId: Long? = null,
    override val /* contract */ notes: String? = null,
    val templateId: Long? = null
) : AccountLinkedAmount, TypeHolder<TransactionType>, NotesHolder

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
