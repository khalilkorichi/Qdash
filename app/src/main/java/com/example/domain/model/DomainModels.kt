package com.example.domain.model

import com.example.data.local.entities.*

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long,
    val toAccountId: Long? = null,
    val note: String? = null,
    val date: Long,
    val isRecurring: Boolean = false,
    val recurringPeriod: String? = null,
    val attachmentPath: String? = null,
    val tags: String? = null,
    val kind: TransactionKind = when (type) {
        TransactionType.INCOME -> TransactionKind.INCOME
        TransactionType.EXPENSE -> TransactionKind.EXPENSE
        TransactionType.TRANSFER -> TransactionKind.TRANSFER
    },
    val transferId: String? = null,
    val isDebit: Boolean = true
)

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

enum class TransactionKind {
    INCOME, EXPENSE, TRANSFER, SAVINGS_CONTRIBUTION, SAVINGS_WITHDRAWAL, SALARY
}

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "DZD",
    val color: String,
    val icon: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

enum class AccountType {
    BANK, CCP, BARIDIMOB, CASH, SAVINGS, WALLET, OTHER
}

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val budgetLimit: Double? = null,
    val isSystem: Boolean = false,
    val parentId: Long? = null,
    val sortOrder: Int = 0
)

enum class CategoryType {
    EXPENSE, INCOME
}


data class IncomeSource(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val type: String, // "SALARY", "FREELANCE", "GIFT", "RENTAL", "OTHER"
    val accountId: Long,
    val dayOfMonth: Int,
    val isActive: Boolean = true,
    val nextExpectedDate: Long = System.currentTimeMillis()
)

data class SavingGoal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val accountId: Long,
    val icon: String,
    val color: String,
    val isCompleted: Boolean = false
)

data class Subscription(
    val id: Long = 0,
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

// Mappers from Entity to Domain
fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    note = note,
    date = date,
    isRecurring = isRecurring,
    recurringPeriod = recurringPeriod,
    attachmentPath = attachmentPath,
    tags = tags,
    kind = try { TransactionKind.valueOf(kind) } catch (e: Exception) { TransactionKind.INCOME },
    transferId = transferId,
    isDebit = isDebit
)

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    balance = balance,
    currency = currency,
    color = color,
    icon = icon,
    isDefault = isDefault,
    isArchived = isArchived,
    createdAt = createdAt,
    sortOrder = sortOrder
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = CategoryType.valueOf(type),
    icon = icon,
    color = color,
    budgetLimit = budgetLimit,
    isSystem = isSystem,
    parentId = parentId,
    sortOrder = sortOrder
)

fun IncomeSourceEntity.toDomain() = IncomeSource(
    id = id,
    name = name,
    amount = amount,
    type = type,
    accountId = accountId,
    dayOfMonth = dayOfMonth,
    isActive = isActive,
    nextExpectedDate = nextExpectedDate
)

fun SavingGoalEntity.toDomain() = SavingGoal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    deadline = deadline,
    accountId = accountId,
    icon = icon,
    color = color,
    isCompleted = isCompleted
)

fun SubscriptionEntity.toDomain() = Subscription(
    id = id,
    name = name,
    amount = amount,
    currency = currency,
    billingCycle = billingCycle,
    nextBillingDate = nextBillingDate,
    accountId = accountId,
    categoryId = categoryId,
    icon = icon,
    isActive = isActive,
    reminderDaysBefore = reminderDaysBefore,
    isAutoShiftableBySalary = isAutoShiftableBySalary
)

// Mappers from Domain to Entity
fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    note = note,
    date = date,
    isRecurring = isRecurring,
    recurringPeriod = recurringPeriod,
    attachmentPath = attachmentPath,
    tags = tags,
    kind = kind.name,
    transferId = transferId,
    isDebit = isDebit
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    balance = balance,
    currency = currency,
    color = color,
    icon = icon,
    isDefault = isDefault,
    isArchived = isArchived,
    createdAt = createdAt,
    sortOrder = sortOrder
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    type = type.name,
    icon = icon,
    color = color,
    budgetLimit = budgetLimit,
    isSystem = isSystem,
    parentId = parentId,
    sortOrder = sortOrder
)

fun IncomeSource.toEntity() = IncomeSourceEntity(
    id = id,
    name = name,
    amount = amount,
    type = type,
    accountId = accountId,
    dayOfMonth = dayOfMonth,
    isActive = isActive,
    nextExpectedDate = nextExpectedDate
)

fun SavingGoal.toEntity() = SavingGoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    deadline = deadline,
    accountId = accountId,
    icon = icon,
    color = color,
    isCompleted = isCompleted
)

fun Subscription.toEntity() = SubscriptionEntity(
    id = id,
    name = name,
    amount = amount,
    currency = currency,
    billingCycle = billingCycle,
    nextBillingDate = nextBillingDate,
    accountId = accountId,
    categoryId = categoryId,
    icon = icon,
    isActive = isActive,
    reminderDaysBefore = reminderDaysBefore,
    isAutoShiftableBySalary = isAutoShiftableBySalary
)

data class SalaryDelay(
    val id: Long = 0,
    val salaryId: Long,
    val delayDays: Int,
    val originalDate: Long,
    val newDate: Long,
    val severityScore: Int,
    val status: String = "CONFIRMED",
    val createdAt: Long = System.currentTimeMillis()
)

fun SalaryDelayEntity.toDomain() = SalaryDelay(
    id = id,
    salaryId = salaryId,
    delayDays = delayDays,
    originalDate = originalDate,
    newDate = newDate,
    severityScore = severityScore,
    status = status,
    createdAt = createdAt
)

fun SalaryDelay.toEntity() = SalaryDelayEntity(
    id = id,
    salaryId = salaryId,
    delayDays = delayDays,
    originalDate = originalDate,
    newDate = newDate,
    severityScore = severityScore,
    status = status,
    createdAt = createdAt
)

enum class DelaySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class AffectedObligation(
    val id: Long,
    val name: String,
    val amount: Double,
    val originalDueDate: Long,
    val type: String, // "SUBSCRIPTION", "DEBT"
    val isAutoShiftable: Boolean
)

data class SalaryDelayImpact(
    val newDate: Long,
    val affectedCount: Int,
    val totalAmount: Double,
    val affectedObligations: List<AffectedObligation>,
    val severityScore: Int,
    val severity: DelaySeverity
)

data class SalaryManagementOverview(
    val salary: IncomeSource?,
    val delays: List<SalaryDelay>,
    val activeSubscriptions: List<Subscription>,
    val activeDebts: List<Debt>,
    val distribution: SalaryDistribution? = null,
    val envelopes: List<SalaryEnvelope> = emptyList()
)

// --- Salary Distribution (50/30/20 Rule) ---

enum class EnvelopeType {
    NEEDS, WANTS, SAVINGS
}

data class SalaryDistribution(
    val id: Long = 0,
    val salaryId: Long,
    val isEnabled: Boolean = false,
    val needsPercentage: Int = 50,
    val wantsPercentage: Int = 30,
    val savingsPercentage: Int = 20,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SalaryEnvelope(
    val id: Long = 0,
    val distributionId: Long,
    val type: EnvelopeType,
    val label: String,
    val percentage: Int,
    val allocatedAmount: Double,
    val spentAmount: Double = 0.0,
    val linkedCategoryIds: List<Long> = emptyList(),
    val color: String,
    val icon: String
) {
    val remainingAmount: Double get() = allocatedAmount - spentAmount
    val usagePercentage: Double get() = if (allocatedAmount > 0) (spentAmount / allocatedAmount * 100) else 0.0
}

fun com.example.data.local.entities.SalaryDistributionEntity.toDomain() = SalaryDistribution(
    id = id,
    salaryId = salaryId,
    isEnabled = isEnabled,
    needsPercentage = needsPercentage,
    wantsPercentage = wantsPercentage,
    savingsPercentage = savingsPercentage,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SalaryDistribution.toEntity() = com.example.data.local.entities.SalaryDistributionEntity(
    id = id,
    salaryId = salaryId,
    isEnabled = isEnabled,
    needsPercentage = needsPercentage,
    wantsPercentage = wantsPercentage,
    savingsPercentage = savingsPercentage,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun com.example.data.local.entities.SalaryEnvelopeEntity.toDomain() = SalaryEnvelope(
    id = id,
    distributionId = distributionId,
    type = try { EnvelopeType.valueOf(type) } catch (e: Exception) { EnvelopeType.NEEDS },
    label = label,
    percentage = percentage,
    allocatedAmount = allocatedAmount,
    spentAmount = spentAmount,
    linkedCategoryIds = if (linkedCategoryIds.isBlank()) emptyList() else linkedCategoryIds.split(",").mapNotNull { it.trim().toLongOrNull() },
    color = color,
    icon = icon
)

fun SalaryEnvelope.toEntity() = com.example.data.local.entities.SalaryEnvelopeEntity(
    id = id,
    distributionId = distributionId,
    type = type.name,
    label = label,
    percentage = percentage,
    allocatedAmount = allocatedAmount,
    spentAmount = spentAmount,
    linkedCategoryIds = linkedCategoryIds.joinToString(","),
    color = color,
    icon = icon
)

