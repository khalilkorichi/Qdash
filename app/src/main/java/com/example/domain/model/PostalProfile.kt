package com.example.domain.model

import com.example.data.local.entities.PostalProfileEntity

data class PostalProfile(
    val id: Long = 0,
    val profileName: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val accountNumber: String,
    val accountKey: String,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val defaultRole: PostalProfileRole = PostalProfileRole.SELF,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PostalProfileRole {
    SENDER, BENEFICIARY, SELF
}

fun PostalProfileEntity.toDomain() = PostalProfile(
    id = id,
    profileName = profileName,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    accountNumber = accountNumber,
    accountKey = accountKey,
    phone = phone,
    address = address,
    city = city,
    defaultRole = try { PostalProfileRole.valueOf(defaultRole) } catch (e: Exception) { PostalProfileRole.SELF },
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PostalProfile.toEntity() = PostalProfileEntity(
    id = id,
    profileName = profileName,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    accountNumber = accountNumber,
    accountKey = accountKey,
    phone = phone,
    address = address,
    city = city,
    defaultRole = defaultRole.name,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt
)
