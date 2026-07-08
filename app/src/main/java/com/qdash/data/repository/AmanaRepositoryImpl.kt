package com.qdash.data.repository

import com.qdash.data.local.dao.AmanaDao
import com.qdash.data.local.entities.AmanaEntity
import com.qdash.domain.model.Amana
import com.qdash.domain.repository.AmanaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AmanaRepositoryImpl(
    private val amanaDao: AmanaDao
) : AmanaRepository {

    override fun getAmanasByAccount(accountId: Long): Flow<List<Amana>> =
        amanaDao.getAmanasByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun getAllAmanas(): Flow<List<Amana>> =
        amanaDao.getAllAmanas().map { list -> list.map { it.toDomain() } }

    override fun getTotalAmanaAmount(): Flow<Double> =
        amanaDao.getTotalAmanaAmount()

    override fun getTotalAmanaAmountForAccount(accountId: Long): Flow<Double> =
        amanaDao.getTotalAmanaAmountForAccount(accountId)

    override suspend fun insertAmana(amana: Amana): Long =
        amanaDao.insertAmana(amana.toEntity())

    override suspend fun updateAmana(amana: Amana) =
        amanaDao.updateAmana(amana.toEntity())

    override suspend fun deleteAmana(amana: Amana) =
        amanaDao.deleteAmana(amana.toEntity())

    override suspend fun deleteAmanaById(id: Long) =
        amanaDao.deleteAmanaById(id)
}

// --- Mapping extensions ---
private fun AmanaEntity.toDomain() = Amana(
    id = id,
    accountId = accountId,
    name = name,
    ownerName = ownerName,
    amount = amount,
    notes = notes,
    createdAt = createdAt
)

private fun Amana.toEntity() = AmanaEntity(
    id = id,
    accountId = accountId,
    name = name,
    ownerName = ownerName,
    amount = amount,
    notes = notes,
    createdAt = createdAt
)
