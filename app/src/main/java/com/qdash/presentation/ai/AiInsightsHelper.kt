package com.qdash.presentation.ai

import com.qdash.domain.model.Category
import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType

/**
 * Pure stateless helper that computes proactive AI insights from transaction data.
 * Extracted from AiChatViewModel to keep the ViewModel under the 500-line SIZE-001 threshold.
 */
object AiInsightsHelper {

    /**
     * Computes a list of proactive insight strings from historical transaction data.
     * All logic is pure — no suspend calls, no side effects.
     */
    fun computeInsights(
        transactions: List<Transaction>,
        categories: List<Category>
    ): List<String> {
        val insights = mutableListOf<String>()

        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000L
        val twoWeeksAgo = now - 14 * 24 * 60 * 60 * 1000L

        val thisWeekTxs = transactions.filter { it.date in oneWeekAgo..now && it.type == TransactionType.EXPENSE }
        val lastWeekTxs = transactions.filter { it.date in twoWeeksAgo..oneWeekAgo && it.type == TransactionType.EXPENSE }

        val thisWeekTotal = thisWeekTxs.sumOf { it.amount }
        val lastWeekTotal = lastWeekTxs.sumOf { it.amount }

        // Week-over-week change
        if (lastWeekTotal > 0.0) {
            val percentChange = ((thisWeekTotal - lastWeekTotal) / lastWeekTotal) * 100.0
            when {
                percentChange > 10.0 ->
                    insights.add("⚠️ لاحظت زيادة بنسبة %.1f%% في إجمالي مصاريفك هذا الأسبوع مقارنة بالأسبوع الماضي.".format(percentChange))
                percentChange < -10.0 ->
                    insights.add("🎉 ممتاز! انخفضت مصاريفك بنسبة %.1f%% هذا الأسبوع مقارنة بالأسبوع الماضي.".format(-percentChange))
            }
        }

        // Top spending category this week
        val topCategoryEntry = thisWeekTxs.groupBy { it.categoryId }
            .maxByOrNull { it.value.sumOf { tx -> tx.amount } }
        if (topCategoryEntry != null) {
            val catName = categories.find { it.id == topCategoryEntry.key }?.name ?: "أخرى"
            val catTotal = topCategoryEntry.value.sumOf { it.amount }
            insights.add("📊 فئة \"$catName\" هي الأعلى إنفاقاً هذا الأسبوع بإجمالي %s.".format(
                com.qdash.core.utils.FormatterUtils.formatCurrency(catTotal)
            ))
        }

        // High spend alert
        if (thisWeekTotal > 50000.0) {
            insights.add("💡 لقد أنفقت أكثر من 50,000 دج في الـ 7 أيام الأخيرة. قد ترغب في مراجعة ميزانيتك.")
        }

        // Fallback tips
        if (insights.isEmpty()) {
            insights.add("💡 تلميحة: تقسيم راتبك بنسبة 50/30/20 (الاحتياجات، الرغبات، الادخار) هو البداية الصحيحة للحرية المالية.")
            insights.add("💡 حافظ على تدوين كل المصاريف اليومية الصغيرة لتكشف أين تذهب أموالك بدقة.")
        }

        return insights
    }
}
