package com.example.domain.repository

import com.example.domain.model.TransferRecord
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAllTransfers(): Flow<List<TransferRecord>>
    fun getTransfersByAccount(accountId: Long): Flow<List<TransferRecord>>
    suspend fun insertTransfer(transfer: TransferRecord): Long
    suspend fun deleteTransfer(transfer: TransferRecord)
}
