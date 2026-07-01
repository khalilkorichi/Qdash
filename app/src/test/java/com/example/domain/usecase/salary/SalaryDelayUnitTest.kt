package com.example.domain.usecase.salary

import com.example.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryDelayUnitTest {

    @Test
    fun testAnalyzeSalaryDelayImpact() {
        val useCase = AnalyzeSalaryDelayImpactUseCase()
        
        val salary = IncomeSource(
            id = 1,
            name = "راتبي",
            amount = 50000.0,
            type = "SALARY",
            accountId = 1,
            dayOfMonth = 25,
            isActive = true,
            nextExpectedDate = 1000000000L // mock timestamp
        )

        // Subscriptions
        val subscriptions = listOf(
            Subscription(
                id = 10,
                name = "Netflix",
                amount = 1500.0,
                billingCycle = "MONTHLY",
                nextBillingDate = 1000200000L, // affected: in 1000000000L..1000864000L
                accountId = 1,
                categoryId = 2,
                isAutoShiftableBySalary = true // flexible: +10 severity
            ),
            Subscription(
                id = 11,
                name = "Gym",
                amount = 3000.0,
                billingCycle = "MONTHLY",
                nextBillingDate = 1000300000L, // affected
                accountId = 1,
                categoryId = 2,
                isAutoShiftableBySalary = false // strict: +20 severity
            ),
            Subscription(
                id = 12,
                name = "Internet",
                amount = 2500.0,
                billingCycle = "MONTHLY",
                nextBillingDate = 2000000000L, // not affected
                accountId = 1,
                categoryId = 2,
                isAutoShiftableBySalary = false
            )
        )

        // Debts
        val debts = listOf(
            Debt(
                id = 20,
                title = "قرض سيارة",
                creditorName = "Creditor",
                totalAmount = 500000.0,
                remainingAmount = 200000.0,
                minimumPayment = 10000.0,
                paymentFrequency = "MONTHLY",
                priority = 1,
                color = "#FF0000",
                icon = "car",
                dueDate = 1000400000L // affected: +25 severity
            ),
            Debt(
                id = 21,
                title = "قرض عقاري",
                creditorName = "Creditor",
                totalAmount = 5000000.0,
                remainingAmount = 3000000.0,
                minimumPayment = 40000.0,
                paymentFrequency = "MONTHLY",
                priority = 1,
                color = "#FF0000",
                icon = "house",
                dueDate = 3000000000L // not affected
            )
        )

        // 1. Test 0 days delay
        val impactZero = useCase(salary, 0, subscriptions, debts)
        assertEquals(0, impactZero.affectedCount)
        assertEquals(0, impactZero.severityScore)
        assertEquals(DelaySeverity.LOW, impactZero.severity)

        // 2. Test 10 days delay (Netflix + Gym + Car Loan + >7 days penalty = 10 + 20 + 25 + 15 = 70 severity)
        val impactTen = useCase(salary, 10, subscriptions, debts)
        assertEquals(3, impactTen.affectedCount)
        assertEquals(70, impactTen.severityScore)
        assertEquals(DelaySeverity.HIGH, impactTen.severity)
        assertEquals(204500.0, impactTen.totalAmount, 0.01) // 1500 + 3000 + 200000 (remaining debt amount is sumOf)

        // 3. Test clamping to 100
        val extremeDebts = debts + debts + debts + debts // adds many debts to exceed 100 score
        val impactExtreme = useCase(salary, 10, subscriptions, extremeDebts)
        assertEquals(100, impactExtreme.severityScore)
        assertEquals(DelaySeverity.CRITICAL, impactExtreme.severity)
    }
}
