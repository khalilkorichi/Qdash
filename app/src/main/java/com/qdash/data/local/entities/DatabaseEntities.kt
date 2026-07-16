package com.qdash.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ForeignKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["date"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null, // for transfer destination
    val note: String? = null,
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringPeriod: String? = null, // "MONTHLY", "WEEKLY", "DAILY"
    val attachmentPath: String? = null,
    val tags: String? = null,
    val suggestedCategoryId: Long? = null,
    val suggestionSource: String? = null,
    val confidenceScore: Float? = null,
    val userAcceptedSuggestion: Boolean? = null,
    val kind: String = "INCOME",
    val transferId: String? = null,
    val isDebit: Boolean = true,
    // Precise execution timestamp (epoch ms, local timezone). Null for records created before
    // time-picker support was added. Display time only when this field is non-null.
    val occurredAt: Long? = null
)

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isArchived", "sortOrder", "createdAt"]),
        Index(value = ["isActive"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "BANK", "CCP", "BARIDIMOB", "CASH", "SAVINGS", "WALLET", "OTHER"
    val balance: Double,
    val currency: String = "DZD",
    val color: String, // Hex string
    val icon: String, // Icon name (Material icon key)
    val iconPath: String? = null, // Path to gallery image in internal storage (overrides icon)
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val isActive: Boolean = true, // false = disabled: hidden from net worth but still visible in list
    val isAmanaEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["parentId"])],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "EXPENSE", "INCOME"
    val icon: String,
    val color: String, // Hex string
    val budgetLimit: Double? = null,
    val isSystem: Boolean = false,
    val parentId: Long? = null, // null = root category, non-null = subcategory
    val sortOrder: Int = 0
)

@Entity(
    tableName = "income_sources",
    indices = [Index(value = ["accountId"])],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String, // "SALARY", "FREELANCE", "GIFT", "RENTAL", "OTHER"
    val accountId: Long,
    val dayOfMonth: Int, // 1 to 31
    val isActive: Boolean = true,
    val nextExpectedDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saving_goals",
    indices = [Index(value = ["accountId"])],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SavingGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val accountId: Long,
    val icon: String,
    val color: String,
    val isCompleted: Boolean = false
)

@Entity(
    tableName = "subscriptions",
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["categoryId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val currency: String = "DZD",
    val billingCycle: String, // "MONTHLY", "YEARLY", "WEEKLY"
    val nextBillingDate: Long,
    val accountId: Long,
    val categoryId: Long,
    val icon: String? = null,
    val isActive: Boolean = true,
    val reminderDaysBefore: Int = 3,
    val isAutoShiftableBySalary: Boolean = false
)
