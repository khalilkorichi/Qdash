package com.qdash.domain.model.common

interface AmountHolder {
    val /* contract */ amount: Double
}

interface AccountLinked {
    val /* contract */ accountId: Long
}

interface AccountLinkedAmount : AmountHolder, AccountLinked {
    override val /* contract */ amount: Double
    override val /* contract */ accountId: Long
}
