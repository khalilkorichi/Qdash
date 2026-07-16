package com.qdash.domain.model.common

interface Timestamped {
    val /* contract */ createdAt: Long
    val /* contract */ updatedAt: Long get() = createdAt
}

interface Updatable {
    val /* contract */ updatedAt: Long
}
