package com.example.data.repository

import com.example.data.local.dao.TransferDao
import com.example.domain.model.TransferRecord
import com.example.domain.model.toDomain
import com.example.domain.model.toEntity
import com.example.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransferRepositoryImpl(
    private val transferDao: TransferDao
) : TransferRepository {

    override fun getAllTransfers(): Flow<List<TransferRecord>> {
        return transferDao.getAllTransfers().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransfersByAccount(accountId: Long): Flow<List<TransferRecord>> {
        return transferDao.getTransfersByAccount(accountId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun insertTransfer(transfer: TransferRecord): Long {
        return transferDao.insertTransfer(transfer.toEntity())
    }

    override suspend fun deleteTransfer(transfer: TransferRecord) {
        transferDao.deleteTransfer(transfer.toEntity())
    }
}
