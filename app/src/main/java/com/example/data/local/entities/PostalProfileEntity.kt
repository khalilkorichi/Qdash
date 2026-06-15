package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "postal_profiles")
data class PostalProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileName: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val accountNumber: String,
    val accountKey: String,
    val phone: String?,
    val address: String?,
    val city: String?,
    val defaultRole: String, // "SENDER", "BENEFICIARY", "SELF"
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
