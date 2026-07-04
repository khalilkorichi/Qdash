package com.qdash.data.repository

import com.qdash.data.local.dao.TransferDao
import com.qdash.domain.model.TransferRecord
import com.qdash.domain.model.toDomain
import com.qdash.domain.model.toEntity
import com.qdash.domain.repository.TransferRepository
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
