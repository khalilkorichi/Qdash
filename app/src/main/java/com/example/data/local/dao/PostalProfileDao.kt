package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.PostalProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostalProfileDao {
    @Query("SELECT * FROM postal_profiles ORDER BY isFavorite DESC, updatedAt DESC")
    fun getAllProfiles(): Flow<List<PostalProfileEntity>>

    @Query("SELECT * FROM postal_profiles WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteProfiles(): Flow<List<PostalProfileEntity>>

    @Query("SELECT * FROM postal_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): PostalProfileEntity?

    @Query("SELECT * FROM postal_profiles WHERE defaultRole = :role ORDER BY isFavorite DESC, updatedAt DESC")
    fun getProfilesByRole(role: String): Flow<List<PostalProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PostalProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: PostalProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: PostalProfileEntity)

    @Query("DELETE FROM postal_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("SELECT COUNT(*) FROM postal_profiles")
    suspend fun getProfilesCount(): Int
}
