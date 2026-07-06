// fixtures/bad_dao_sync.kt
// BAD: DAO functions are not suspend and don't return Flow — triggers THR-001
package com.qdash.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.qdash.data.local.entities.TransactionEntity

@Dao
interface BadTransactionDao {

    // BAD: synchronous return — should be suspend fun or Flow<>
    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): List<TransactionEntity>

    // BAD: synchronous insert — should be suspend fun
    @Insert
    fun insertTransaction(transaction: TransactionEntity): Long

    // BAD: synchronous query with filtering
    @Query("SELECT * FROM transactions WHERE accountId = :accountId")
    fun getByAccount(accountId: Long): List<TransactionEntity>
}
