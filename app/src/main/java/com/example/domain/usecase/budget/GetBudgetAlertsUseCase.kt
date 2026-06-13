package com.example.domain.usecase.budget

import com.example.domain.model.BudgetGoal
import com.example.domain.model.BudgetStatus
import java.util.concurrent.TimeUnit

data class BudgetAlert(
    val budgetId: Long,
    val title: String,
    val message: String,
    val status: BudgetStatus,
    val percentageUsed: Int
)

class GetBudgetAlertsUseCase {
    operator fun invoke(goals: List<BudgetGoal>): List<BudgetAlert> {
        val alerts = mutableListOf<BudgetAlert>()
        val now = System.currentTimeMillis()

        for (goal in goals) {
            if (!goal.isActive) continue

            val usagePercent = goal.usagePercent
            val percentageInt = (usagePercent * 100).toInt()

            // 1. Check direct limit reached or exceeded
            if (usagePercent >= 1.0) {
                alerts.add(
                    BudgetAlert(
                        budgetId = goal.id,
                        title = goal.title,
                        message = "لقد تجاوزت ميزانية '${goal.title}' بمقدار ${(goal.spentAmount - goal.amountLimit).toInt()} د.ج.",
                        status = BudgetStatus.EXCEEDED,
                        percentageUsed = percentageInt
                    )
                )
                continue
            }

            // 2. Check threshold alerts (Warning / Critical)
            if (usagePercent >= (goal.alertThresholdPercent.toDouble() / 100.0)) {
                alerts.add(
                    BudgetAlert(
                        budgetId = goal.id,
                        title = goal.title,
                        message = "تنبيه حرج! لقد استهلكت $percentageInt% من ميزانية '${goal.title}'. المتبقي: ${(goal.amountLimit - goal.spentAmount).toInt()} د.ج.",
                        status = BudgetStatus.CRITICAL,
                        percentageUsed = percentageInt
                    )
                )
            } else if (usagePercent >= 0.50) {
                alerts.add(
                    BudgetAlert(
                        budgetId = goal.id,
                        title = goal.title,
                        message = "تنبيه: لقد استهلكت نصف ميزانية '${goal.title}' ($percentageInt%).",
                        status = BudgetStatus.WARNING,
                        percentageUsed = percentageInt
                    )
                )
            }

            // 3. Predictive / Pace Warning: Elapsed time vs usage
            val totalDuration = goal.endDate - goal.startDate
            val elapsed = now - goal.startDate
            if (totalDuration > 0 && elapsed > 0 && elapsed < totalDuration) {
                val timePassedPercent = elapsed.toDouble() / totalDuration.toDouble()
                
                if (usagePercent > 0.30 && (usagePercent - timePassedPercent) > 0.15) {
                    val timePassedInt = (timePassedPercent * 100).toInt()
                    alerts.add(
                        BudgetAlert(
                            budgetId = goal.id,
                            title = goal.title,
                            message = "معدل إنفاق سريع! لقد استهلكت $percentageInt% من ميزانية '${goal.title}' بينما مضى $timePassedInt% فقط من المدة.",
                            status = BudgetStatus.WARNING,
                            percentageUsed = percentageInt
                        )
                    )
                }
                
                val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsed).coerceAtLeast(1)
                val totalDays = TimeUnit.MILLISECONDS.toDays(totalDuration).coerceAtLeast(1)
                val projected = (goal.spentAmount / elapsedDays) * totalDays
                if (projected > goal.amountLimit) {
                    alerts.add(
                        BudgetAlert(
                            budgetId = goal.id,
                            title = goal.title,
                            message = "توقع إنفاق: قد تتجاوز ميزانية '${goal.title}' لتصل لـ ${projected.toInt()} د.ج قبل نهاية الفترة بالوتيرة الحالية.",
                            status = BudgetStatus.WARNING,
                            percentageUsed = percentageInt
                        )
                    )
                }
            }
        }
        return alerts
    }
}
