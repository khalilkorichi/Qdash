package com.qdash.domain.model

import com.qdash.data.local.entities.PostalProfileEntity
import com.qdash.domain.model.common.*

data class PostalProfile(
    override val /* contract */ id: Long = 0,
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
    override val /* contract */ createdAt: Long = System.currentTimeMillis(),
    override val /* contract */ updatedAt: Long = System.currentTimeMillis()
) : Identifiable, Timestamped, Updatable

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
