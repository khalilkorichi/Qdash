package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "user_category_mappings",
    indices = [Index(value = ["normalizedText"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserCategoryMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedText: String,
    val categoryId: Long,
    val usageCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)
