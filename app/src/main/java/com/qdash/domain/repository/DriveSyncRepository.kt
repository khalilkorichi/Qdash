package com.qdash.domain.repository

import android.content.Context

interface DriveSyncRepository {
    suspend fun uploadToAppData(context: Context): Result<Unit>
    suspend fun downloadFromAppData(context: Context): Result<Boolean>
}
