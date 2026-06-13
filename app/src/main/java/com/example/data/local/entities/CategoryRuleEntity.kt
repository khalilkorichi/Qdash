package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "category_rules",
    indices = [Index(value = ["keyword"])]
)
data class CategoryRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val categoryId: Long,
    val priority: Int = 0,
    val source: String = "SYSTEM", // "SYSTEM", "USER", "AI_IMPORTED"
    val isActive: Boolean = true
)
