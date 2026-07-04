package com.qdash.domain.repository

import com.qdash.domain.model.TransferRecord
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAllTransfers(): Flow<List<TransferRecord>>
    fun getTransfersByAccount(accountId: Long): Flow<List<TransferRecord>>
    suspend fun insertTransfer(transfer: TransferRecord): Long
    suspend fun deleteTransfer(transfer: TransferRecord)
}
