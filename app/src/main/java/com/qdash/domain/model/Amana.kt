package com.qdash.domain.model

data class Amana(
    val id: Long = 0,
    val accountId: Long,
    val name: String,
    val ownerName: String,
    val amount: Double,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
