package com.qdash.data.local.dao

import androidx.room.*
import com.qdash.data.local.entities.AmanaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AmanaDao {

    @Query("SELECT * FROM amanas WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getAmanasByAccount(accountId: Long): Flow<List<AmanaEntity>>

    @Query("SELECT * FROM amanas ORDER BY createdAt DESC")
    fun getAllAmanas(): Flow<List<AmanaEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM amanas")
    fun getTotalAmanaAmount(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM amanas WHERE accountId = :accountId")
    fun getTotalAmanaAmountForAccount(accountId: Long): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmana(amana: AmanaEntity): Long

    @Update
    suspend fun updateAmana(amana: AmanaEntity)

    @Delete
    suspend fun deleteAmana(amana: AmanaEntity)

    @Query("DELETE FROM amanas WHERE id = :id")
    suspend fun deleteAmanaById(id: Long)
}
