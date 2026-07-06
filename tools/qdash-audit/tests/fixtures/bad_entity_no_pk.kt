// fixtures/bad_entity_no_pk.kt
// BAD: Room entity without a primary key annotation — triggers DB-001
package com.qdash.data.local.entities

import androidx.room.Entity

@Entity(tableName = "bad_records")
data class BadRecordEntity(
    val name: String,
    val amount: Double,
    val createdAt: Long
    // MISSING: no primary key annotation — this entity will trigger DB-001!
)
