package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "savings_contributions",
    indices = [
        Index(value = ["savingGoalId"]),
        Index(value = ["accountId"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SavingGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingGoalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SavingsContributionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savingGoalId: Long,
    val accountId: Long,
    val amount: Double,
    val type: String, // "DEPOSIT", "WITHDRAWAL"
    val note: String? = null,
    val date: Long,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "debts",
    indices = [Index(value = ["linkedAccountId"])],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val creditorName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val interestRate: Double? = null,
    val dueDate: Long? = null,
    val minimumPayment: Double,
    val recommendedPayment: Double? = null,
    val paymentFrequency: String, // "MONTHLY", "WEEKLY", "MANUAL"
    val linkedAccountId: Long? = null,
    val priority: Int,
    val notes: String? = null,
    val color: String,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val debtType: String = "INSTALLMENT"
)

@Entity(
    tableName = "debt_payments",
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["accountId"]),
        Index(value = ["paymentDate"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val accountId: Long,
    val amount: Double,
    val paymentDate: Long,
    val paymentType: String, // "MINIMUM", "EXTRA", "MANUAL", "SCHEDULED"
    val note: String? = null,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["fromAccountId"]),
        Index(value = ["toAccountId"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val feeAmount: Double? = null,
    val note: String? = null,
    val date: Long,
    val referenceId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_financial_aggregates",
    indices = [Index(value = ["localDateTimestamp"], unique = true)]
)
data class DailyFinancialAggregateEntity(
    @PrimaryKey
    val localDateTimestamp: Long, // Midnight timestamp in local timezone
    val totalExpense: Double,
    val totalIncome: Double,
    val transactionCount: Int,
    val netCashflow: Double,
    val activityScore: Double
)
