package com.qdash.domain.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.data.local.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getUserProfile(): Flow<UserProfileEntity?>
    suspend fun signIn(account: GoogleSignInAccount): Result<UserProfileEntity>
    suspend fun silentSignIn(context: Context): Flow<Result<UserProfileEntity>>
    suspend fun signOut(context: Context): Result<Unit>
    suspend fun updateBirthDate(birthDate: String): Result<Unit>
}
