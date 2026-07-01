package com.example.domain.usecase.salary

import com.example.domain.model.*

class AnalyzeSalaryDelayImpactUseCase {
    operator fun invoke(
        salary: IncomeSource,
        delayDays: Int,
        subscriptions: List<Subscription>,
        debts: List<Debt>,
        originalDateOverride: Long? = null,
        oldDelayDays: Int = 0
    ): SalaryDelayImpact {
        val originalDate = originalDateOverride ?: salary.nextExpectedDate
        val delayMillis = delayDays * 86400000L
        val newDate = originalDate + delayMillis

        val affectedObligations = mutableListOf<AffectedObligation>()

        // Analyze subscriptions
        for (sub in subscriptions) {
            val originalBillingDate = if (sub.isAutoShiftableBySalary && oldDelayDays > 0) {
                sub.nextBillingDate - (oldDelayDays * 86400000L)
            } else {
                sub.nextBillingDate
            }

            if (originalBillingDate in originalDate..newDate) {
                affectedObligations.add(
                    AffectedObligation(
                        id = sub.id,
                        name = sub.name,
                        amount = sub.amount,
                        originalDueDate = originalBillingDate,
                        type = "SUBSCRIPTION",
                        isAutoShiftable = sub.isAutoShiftableBySalary
                    )
                )
            }
        }

        // Analyze debts
        for (debt in debts) {
            val dueDate = debt.dueDate
            if (dueDate != null && dueDate in originalDate..newDate) {
                affectedObligations.add(
                    AffectedObligation(
                        id = debt.id,
                        name = debt.title,
                        amount = debt.remainingAmount,
                        originalDueDate = dueDate,
                        type = "DEBT",
                        isAutoShiftable = false
                    )
                )
            }
        }

        // Calculate severity score
        var score = 0
        for (obs in affectedObligations) {
            score += when (obs.type) {
                "DEBT" -> 25
                "SUBSCRIPTION" -> if (obs.isAutoShiftable) 10 else 20
                else -> 10
            }
        }

        if (delayDays > 7) {
            score += 15
        }

        val finalScore = score.coerceAtMost(100)

        val severity = when {
            finalScore <= 20 -> DelaySeverity.LOW
            finalScore <= 45 -> DelaySeverity.MEDIUM
            finalScore <= 70 -> DelaySeverity.HIGH
            else -> DelaySeverity.CRITICAL
        }

        return SalaryDelayImpact(
            newDate = newDate,
            affectedCount = affectedObligations.size,
            totalAmount = affectedObligations.sumOf { it.amount },
            affectedObligations = affectedObligations.sortedBy { it.originalDueDate },
            severityScore = finalScore,
            severity = severity
        )
    }
}
