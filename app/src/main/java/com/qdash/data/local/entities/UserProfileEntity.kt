package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1, // Single profile local database instance
    val name: String = "ضيف قداشّ",
    val email: String? = null,
    val birthDate: String? = null,
    val avatarUrl: String? = null,
    val isGoogleLinked: Boolean = false
)
