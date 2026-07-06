package com.qdash.domain.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.qdash.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun signIn(account: GoogleSignInAccount): Result<UserProfile>
    suspend fun silentSignIn(context: Context): Flow<Result<UserProfile>>
    suspend fun signOut(context: Context): Result<Unit>
    suspend fun updateBirthDate(birthDate: String): Result<Unit>
}
