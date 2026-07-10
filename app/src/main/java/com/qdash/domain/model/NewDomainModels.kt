package com.qdash.domain.model

import com.qdash.data.local.entities.*
import com.qdash.domain.model.common.*

data class SavingsContribution(
    override val /* contract */ id: Long = 0,
    val savingGoalId: Long,
    override val /* contract */ accountId: Long,
    override val /* contract */ amount: Double,
    override val /* contract */ type: SavingsContributionType,
    override val /* contract */ note: String? = null,
    val date: Long,
    val linkedTransactionId: Long? = null,
    override val /* contract */ createdAt: Long = System.currentTimeMillis()
) : Identifiable, AccountLinkedAmount, TypeHolder<SavingsContributionType>, Notable, Timestamped

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
    override val /* contract */ id: Long = 0,
    override val /* contract */ title: String,
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
    override val /* contract */ notes: String? = null,
    override val /* contract */ color: String,
    override val /* contract */ icon: String,
    override val /* contract */ createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val debtType: DebtType = DebtType.INSTALLMENT
) : Identifiable, Titled, NotesHolder, ColorTagged, IconTagged, Timestamped

data class DebtPayment(
    override val /* contract */ id: Long = 0,
    val debtId: Long,
    override val /* contract */ accountId: Long,
    override val /* contract */ amount: Double,
    val paymentDate: Long,
    val paymentType: DebtPaymentType,
    override val /* contract */ note: String? = null,
    val linkedTransactionId: Long? = null,
    override val /* contract */ createdAt: Long = System.currentTimeMillis()
) : Identifiable, AccountLinkedAmount, Notable, Timestamped

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
    override val /* contract */ amount: Double,
    val feeAmount: Double? = null,
    override val /* contract */ note: String? = null,
    val date: Long
) : AmountHolder, Notable

data class TransferRecord(
    override val /* contract */ id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    override val /* contract */ amount: Double,
    val feeAmount: Double? = null,
    override val /* contract */ note: String? = null,
    val date: Long,
    val referenceId: String,
    override val /* contract */ createdAt: Long = System.currentTimeMillis()
) : Identifiable, AmountHolder, Notable, Timestamped

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
    override val /* contract */ title: String,
    val content: String,
    val type: SectionType
) : Titled

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
    override val /* contract */ title: String,
    val suggestionText: String,
    override val /* contract */ note: String,
    val defaultAmount: Double,
    val targetKeyword: String,
    val iconName: String
) : Titled, Notable

