package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "salary_distributions",
    indices = [Index(value = ["salaryId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = IncomeSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["salaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SalaryDistributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val salaryId: Long,
    val isEnabled: Boolean = false,
    val needsPercentage: Int = 50,
    val wantsPercentage: Int = 30,
    val savingsPercentage: Int = 20,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "salary_envelopes",
    indices = [Index(value = ["distributionId"])],
    foreignKeys = [
        ForeignKey(
            entity = SalaryDistributionEntity::class,
            parentColumns = ["id"],
            childColumns = ["distributionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class SalaryEnvelopeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val distributionId: Long,
    val type: String, // "NEEDS", "WANTS", "SAVINGS"
    val label: String, // "احتياجات", "رغبات", "ادخار"
    val percentage: Int,
    val allocatedAmount: Double,
    val spentAmount: Double = 0.0,
    val linkedCategoryIds: String = "", // comma-separated IDs
    val linkedAccountId: Long? = null,
    val color: String,
    val icon: String
)
