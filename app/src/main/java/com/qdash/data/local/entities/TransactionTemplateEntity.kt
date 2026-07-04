package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_templates",
    indices = [
        Index(value = ["isPinned"]),
        Index(value = ["usageCount"]),
        Index(value = ["lastUsedAt"]),
        Index(value = ["transactionType"]),
        Index(value = ["categoryId"]),
        Index(value = ["accountId"])
    ]
)
data class TransactionTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val amount: Double,
    val transactionType: String, // "EXPENSE", "INCOME", "TRANSFER"
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
