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

sealed interface Debt : Identifiable, Titled, NotesHolder, ColorTagged, IconTagged, Timestamped, RemainingAmountTrackable {
    override val id: Long
    override val title: String
    val creditorName: String
    val totalAmount: Double
    override val remainingAmount: Double
    val dueDate: Long?
    val linkedAccountId: Long?
    override val notes: String?
    override val color: String
    override val icon: String
    override val createdAt: Long
    val isClosed: Boolean
    val debtType: DebtType
}

data class RegularDebt(
    override val id: Long = 0,
    override val title: String,
    override val creditorName: String,
    override val totalAmount: Double,
    override val remainingAmount: Double,
    override val dueDate: Long? = null,
    override val linkedAccountId: Long? = null,
    override val notes: String? = null,
    override val color: String,
    override val icon: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isClosed: Boolean = false
) : Debt {
    override val debtType: DebtType = DebtType.REGULAR
}

data class InstallmentDebt(
    override val id: Long = 0,
    override val title: String,
    override val creditorName: String,
    override val totalAmount: Double,
    override val remainingAmount: Double,
    override val dueDate: Long? = null,
    override val linkedAccountId: Long? = null,
    override val notes: String? = null,
    override val color: String,
    override val icon: String,
    override val createdAt: Long = System.currentTimeMillis(),
    override val isClosed: Boolean = false,
    val interestRate: Double,
    val minimumPayment: Double,
    val recommendedPayment: Double? = null,
    val paymentFrequency: String,
    val priority: Int
) : Debt {
    override val debtType: DebtType = DebtType.INSTALLMENT
}

fun Debt.copyDebt(
    remainingAmount: Double = this.remainingAmount,
    isClosed: Boolean = this.isClosed,
    notes: String? = this.notes
): Debt {
    return when (this) {
        is RegularDebt -> this.copy(remainingAmount = remainingAmount, isClosed = isClosed, notes = notes)
        is InstallmentDebt -> this.copy(remainingAmount = remainingAmount, isClosed = isClosed, notes = notes)
    }
}

val Debt.minimumPayment: Double
    get() = (this as? InstallmentDebt)?.minimumPayment ?: 0.0

val Debt.interestRate: Double
    get() = (this as? InstallmentDebt)?.interestRate ?: 0.0

val Debt.priority: Int
    get() = (this as? InstallmentDebt)?.priority ?: 3

val Debt.paymentFrequency: String
    get() = (this as? InstallmentDebt)?.paymentFrequency ?: "MONTHLY"

val Debt.recommendedPayment: Double?
    get() = (this as? InstallmentDebt)?.recommendedPayment

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
    val date: Long,
    // Precise execution timestamp to stamp on all transfer legs and TransferEntity.
    val occurredAt: Long? = null
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
    override val /* contract */ createdAt: Long = System.currentTimeMillis(),
    // Precise execution timestamp shared with all linked transaction legs.
    val occurredAt: Long? = null
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

fun DebtWithInstallmentDetails.toDomain(): Debt {
    return if (debt.debtType == "REGULAR") {
        RegularDebt(
            id = debt.id,
            title = debt.title,
            creditorName = debt.creditorName,
            totalAmount = debt.totalAmount,
            remainingAmount = debt.remainingAmount,
            dueDate = debt.dueDate,
            linkedAccountId = debt.linkedAccountId,
            notes = debt.notes,
            color = debt.color,
            icon = debt.icon,
            createdAt = debt.createdAt,
            isClosed = debt.isClosed
        )
    } else {
        val details = installmentDetails ?: throw IllegalStateException("Installment details missing for debt ID: ${debt.id}")
        InstallmentDebt(
            id = debt.id,
            title = debt.title,
            creditorName = debt.creditorName,
            totalAmount = debt.totalAmount,
            remainingAmount = debt.remainingAmount,
            dueDate = debt.dueDate,
            linkedAccountId = debt.linkedAccountId,
            notes = debt.notes,
            color = debt.color,
            icon = debt.icon,
            createdAt = debt.createdAt,
            isClosed = debt.isClosed,
            interestRate = details.interestRate,
            minimumPayment = details.minimumPayment,
            recommendedPayment = details.recommendedPayment,
            paymentFrequency = details.paymentFrequency,
            priority = details.priority
        )
    }
}

fun Debt.toEntity() = DebtEntity(
    id = id,
    title = title,
    creditorName = creditorName,
    totalAmount = totalAmount,
    remainingAmount = remainingAmount,
    dueDate = dueDate,
    linkedAccountId = linkedAccountId,
    notes = notes,
    color = color,
    icon = icon,
    createdAt = createdAt,
    isClosed = isClosed,
    debtType = debtType.name
)

fun Debt.toInstallmentDetailsEntity(): DebtInstallmentDetailsEntity? {
    return if (this is InstallmentDebt) {
        DebtInstallmentDetailsEntity(
            debtId = id,
            interestRate = interestRate,
            minimumPayment = minimumPayment,
            recommendedPayment = recommendedPayment,
            paymentFrequency = paymentFrequency,
            priority = priority
        )
    } else {
        null
    }
}

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
    createdAt = createdAt,
    occurredAt = occurredAt
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
    createdAt = createdAt,
    occurredAt = occurredAt
)

data class ContextAwareSuggestion(
    override val /* contract */ title: String,
    val suggestionText: String,
    override val /* contract */ note: String,
    val defaultAmount: Double,
    val targetKeyword: String,
    val iconName: String
) : Titled, Notable

