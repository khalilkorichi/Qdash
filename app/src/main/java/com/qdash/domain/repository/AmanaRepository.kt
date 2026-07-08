package com.qdash.domain.repository

import com.qdash.domain.model.Amana
import kotlinx.coroutines.flow.Flow

interface AmanaRepository {
    fun getAmanasByAccount(accountId: Long): Flow<List<Amana>>
    fun getAllAmanas(): Flow<List<Amana>>
    fun getTotalAmanaAmount(): Flow<Double>
    fun getTotalAmanaAmountForAccount(accountId: Long): Flow<Double>
    suspend fun insertAmana(amana: Amana): Long
    suspend fun updateAmana(amana: Amana)
    suspend fun deleteAmana(amana: Amana)
    suspend fun deleteAmanaById(id: Long)
}
