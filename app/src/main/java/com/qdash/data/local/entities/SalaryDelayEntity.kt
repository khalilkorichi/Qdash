package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "salary_delays",
    indices = [Index(value = ["salaryId"])],
    foreignKeys = [
        ForeignKey(
            entity = IncomeSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SalaryDelayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryId: Long,
    val delayDays: Int,
    val originalDate: Long,
    val newDate: Long,
    val severityScore: Int,
    val status: String = "CONFIRMED",
    val createdAt: Long = System.currentTimeMillis()
)
