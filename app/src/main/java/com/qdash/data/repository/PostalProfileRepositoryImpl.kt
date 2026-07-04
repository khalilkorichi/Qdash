package com.qdash.data.repository

import com.qdash.data.local.dao.PostalProfileDao
import com.qdash.domain.model.PostalProfile
import com.qdash.domain.model.PostalProfileRole
import com.qdash.domain.model.toDomain
import com.qdash.domain.model.toEntity
import com.qdash.domain.repository.PostalProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostalProfileRepositoryImpl(
    private val postalProfileDao: PostalProfileDao
) : PostalProfileRepository {

    override fun getAllProfiles(): Flow<List<PostalProfile>> {
        return postalProfileDao.getAllProfiles().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteProfiles(): Flow<List<PostalProfile>> {
        return postalProfileDao.getFavoriteProfiles().map { list -> list.map { it.toDomain() } }
    }

    override fun getProfilesByRole(role: PostalProfileRole): Flow<List<PostalProfile>> {
        return postalProfileDao.getProfilesByRole(role.name).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getProfileById(id: Long): PostalProfile? {
        return postalProfileDao.getProfileById(id)?.toDomain()
    }

    override suspend fun insertProfile(profile: PostalProfile): Long {
        return postalProfileDao.insertProfile(profile.toEntity())
    }

    override suspend fun updateProfile(profile: PostalProfile) {
        postalProfileDao.updateProfile(profile.toEntity())
    }

    override suspend fun deleteProfile(profile: PostalProfile) {
        postalProfileDao.deleteProfile(profile.toEntity())
    }

    override suspend fun deleteProfileById(id: Long) {
        postalProfileDao.deleteProfileById(id)
    }

    override suspend fun getProfilesCount(): Int {
        return postalProfileDao.getProfilesCount()
    }
}
