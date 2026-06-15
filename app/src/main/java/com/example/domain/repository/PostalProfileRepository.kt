package com.example.domain.repository

import com.example.domain.model.PostalProfile
import com.example.domain.model.PostalProfileRole
import kotlinx.coroutines.flow.Flow

interface PostalProfileRepository {
    fun getAllProfiles(): Flow<List<PostalProfile>>
    fun getFavoriteProfiles(): Flow<List<PostalProfile>>
    fun getProfilesByRole(role: PostalProfileRole): Flow<List<PostalProfile>>
    suspend fun getProfileById(id: Long): PostalProfile?
    suspend fun insertProfile(profile: PostalProfile): Long
    suspend fun updateProfile(profile: PostalProfile)
    suspend fun deleteProfile(profile: PostalProfile)
    suspend fun deleteProfileById(id: Long)
    suspend fun getProfilesCount(): Int
}
