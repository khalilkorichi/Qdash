package com.qdash.domain.model

import com.qdash.data.local.entities.*

data class SavingsContribution(
    val id: Long = 0,
    val savingGoalId: Long,
    val accountId: Long,
    val amount: Double,
    val type: SavingsContributionType,
    val note: String? = null,
    val date: Long,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SavingsContributionType {
    DEPOSIT, WITHDRAWAL
}

data class SavingsInsight(
    val text: String,
    val isPositive: Boolean = true,
    val icon: String? = null
)

enum class DebtType {
    INSTALLMENT, REGULAR
}

data class Debt(
    val id: Long = 0,
    val title: String,
    val creditorName: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val interestRate: Double? = null,
    val dueDate: Long? = null,
    val minimumPayment: Double,
    val recommendedPayment: Double? = null,
    val paymentFrequency: String,
    val linkedAccountId: Long? = null,
    val priority: Int,
    val notes: String? = null,
    val color: String,
    val icon: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val debtType: DebtType = DebtType.INSTALLMENT
)

data class DebtPayment(
    val id: Long = 0,
    val debtId: Long,
    val accountId: Long,
    val amount: Double,
    val paymentDate: Long,
    val paymentType: DebtPaymentType,
    val note: String? = null,
    val linkedTransactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class DebtPaymentType {
    MINIMUM, EXTRA, MANUAL, SCHEDULED
}

data class DebtStrategyResult(
    val strategyName: String, // "snowball", "avalanche", "custom"
    val totalInterestCharged: Double = 0.0,
    val durationInMonths: Double,
    val estimatedDebtFreeDate: Long,
    val monthlyPaymentNeeded: Double,
    val paymentScheduleSummary: String? = null
)

data class TransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val feeAmount: Double? = null,
    val note: String? = null,
    val date: Long
)

data class TransferRecord(
    val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val feeAmount: Double? = null,
    val note: String? = null,
    val date: Long,
    val referenceId: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ExportReportRequest(
    val reportType: String, // "MONTHLY", "ANNUAL", "SAVINGS", "DEBTS", "ANALYTICS", "ACCOUNT_STATEMENT"
    val dateRange: Pair<Long, Long>? = null,
    val selectedAccounts: List<Long> = emptyList(),
    val selectedCategories: List<Long> = emptyList(),
    val includeCharts: Boolean = true,
    val includeTransactions: Boolean = true,
    val includeDebtSection: Boolean = true,
    val includeSavingsSection: Boolean = true,
    val language: String = "AR",
    val fileName: String? = null
)

data class ExportResult(
    val success: Boolean,
    val fileName: String,
    val fileUri: String? = null,
    val message: String? = null
)

data class ReportSection(
    val title: String,
    val content: String,
    val type: SectionType
)

enum class SectionType {
    KPI_SUMMARY, CHART, TABLE, TEXT
}

// Mapper extension functions

fun SavingsContributionEntity.toDomain() = SavingsContribution(
    id = id,
    savingGoalId = savingGoalId,
    accountId = accountId,
    amount = amount,
    type = SavingsContributionType.valueOf(type),
    note = note,
    date = date,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt
)

fun SavingsContribution.toEntity() = SavingsContributionEntity(
    id = id,
    savingGoalId = savingGoalId,
    accountId = accountId,
    amount = amount,
    type = type.name,
    note = note,
    date = date,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt
)

fun DebtEntity.toDomain() = Debt(
    id = id,
    title = title,
    creditorName = creditorName,
    totalAmount = totalAmount,
    remainingAmount = remainingAmount,
    interestRate = interestRate,
    dueDate = dueDate,
    minimumPayment = minimumPayment,
    recommendedPayment = recommendedPayment,
    paymentFrequency = paymentFrequency,
    linkedAccountId = linkedAccountId,
    priority = priority,
    notes = notes,
    color = color,
    icon = icon,
    createdAt = createdAt,
    isClosed = isClosed,
    debtType = DebtType.valueOf(debtType)
)

fun Debt.toEntity() = DebtEntity(
    id = id,
    title = title,
    creditorName = creditorName,
    totalAmount = totalAmount,
    remainingAmount = remainingAmount,
    interestRate = interestRate,
    dueDate = dueDate,
    minimumPayment = minimumPayment,
    recommendedPayment = recommendedPayment,
    paymentFrequency = paymentFrequency,
    linkedAccountId = linkedAccountId,
    priority = priority,
    notes = notes,
    color = color,
    icon = icon,
    createdAt = createdAt,
    isClosed = isClosed,
    debtType = debtType.name
)

fun DebtPaymentEntity.toDomain() = DebtPayment(
    id = id,
    debtId = debtId,
    accountId = accountId,
    amount = amount,
    paymentDate = paymentDate,
    paymentType = DebtPaymentType.valueOf(paymentType),
    note = note,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt
)

fun DebtPayment.toEntity() = DebtPaymentEntity(
    id = id,
    debtId = debtId,
    accountId = accountId,
    amount = amount,
    paymentDate = paymentDate,
    paymentType = paymentType.name,
    note = note,
    linkedTransactionId = linkedTransactionId,
    createdAt = createdAt
)

fun TransferEntity.toDomain() = TransferRecord(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amount = amount,
    feeAmount = feeAmount,
    note = note,
    date = date,
    referenceId = referenceId,
    createdAt = createdAt
)

fun TransferRecord.toEntity() = TransferEntity(
    id = id,
    fromAccountId = fromAccountId,
    toAccountId = toAccountId,
    amount = amount,
    feeAmount = feeAmount,
    note = note,
    date = date,
    referenceId = referenceId,
    createdAt = createdAt
)

data class ContextAwareSuggestion(
    val title: String,
    val suggestionText: String,
    val note: String,
    val defaultAmount: Double,
    val targetKeyword: String,
    val iconName: String
)

