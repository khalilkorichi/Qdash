package com.qdash.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import com.qdash.core.preferences.PreferencesManager
import com.qdash.data.local.dao.UserProfileDao
import com.qdash.data.local.entities.UserProfileEntity
import com.qdash.domain.model.UserProfile
import com.qdash.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfileFlow().map { it?.toDomain() }
    }

    override suspend fun signIn(account: GoogleSignInAccount): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val entity = UserProfileEntity(
                id = 1,
                name = account.displayName ?: "مستخدم جوجل",
                email = account.email,
                avatarUrl = account.photoUrl?.toString(),
                isGoogleLinked = true,
                birthDate = userProfileDao.getUserProfile()?.birthDate
            )
            userProfileDao.insertUserProfile(entity)
            preferencesManager.isGoogleLinked = true
            preferencesManager.connectedEmail = account.email
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun silentSignIn(context: Context): Flow<Result<UserProfile>> = flow {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        try {
            val account = Tasks.await(googleSignInClient.silentSignIn())
            val existing = userProfileDao.getUserProfile()
            val entity = UserProfileEntity(
                id = 1,
                name = account.displayName ?: existing?.name ?: "مستخدم جوجل",
                email = account.email ?: existing?.email,
                avatarUrl = account.photoUrl?.toString() ?: existing?.avatarUrl,
                isGoogleLinked = true,
                birthDate = existing?.birthDate
            )
            userProfileDao.insertUserProfile(entity)
            preferencesManager.isGoogleLinked = true
            preferencesManager.connectedEmail = account.email
            emit(Result.success(entity.toDomain()))
        } catch (e: Exception) {
            val cause = e.cause
            if (cause is ApiException) {
                if (cause.statusCode == CommonStatusCodes.SIGN_IN_REQUIRED) {
                    // Session expired / not signed in
                }
            }
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun signOut(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            Tasks.await(googleSignInClient.signOut())

            // Keep the birthDate if any, but clear Google-specific data
            val existing = userProfileDao.getUserProfile()
            val guestEntity = UserProfileEntity(
                id = 1,
                name = "ضيف قداشّ",
                email = null,
                avatarUrl = null,
                isGoogleLinked = false,
                birthDate = existing?.birthDate ?: preferencesManager.guestBirthdate
            )
            userProfileDao.insertUserProfile(guestEntity)
            preferencesManager.isGoogleLinked = false
            preferencesManager.connectedEmail = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBirthDate(birthDate: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = userProfileDao.getUserProfile()
            val entity = if (existing != null) {
                existing.copy(birthDate = birthDate)
            } else {
                UserProfileEntity(
                    id = 1,
                    name = "ضيف قداشّ",
                    birthDate = birthDate,
                    isGoogleLinked = false
                )
            }
            userProfileDao.insertUserProfile(entity)
            preferencesManager.guestBirthdate = birthDate
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        name = name,
        email = email,
        birthDate = birthDate,
        avatarUrl = avatarUrl,
        isGoogleLinked = isGoogleLinked
    )
}
