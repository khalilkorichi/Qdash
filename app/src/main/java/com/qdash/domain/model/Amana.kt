package com.qdash.domain.model

import com.qdash.domain.model.common.*

data class Amana(
    override val /* contract */ id: Long = 0,
    override val /* contract */ accountId: Long,
    override val /* contract */ name: String,
    val ownerName: String,
    override val /* contract */ amount: Double,
    override val /* contract */ notes: String? = null,
    override val /* contract */ createdAt: Long = System.currentTimeMillis()
) : Identifiable, AccountLinkedAmount, Nameable, NotesHolder, Timestamped
