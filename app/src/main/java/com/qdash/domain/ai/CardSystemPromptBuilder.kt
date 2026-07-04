package com.qdash.domain.ai

import com.qdash.domain.model.CardAiContext
import com.qdash.domain.model.CardDbSnapshot
import org.json.JSONArray
import org.json.JSONObject

object CardSystemPromptBuilder {
    fun build(context: CardAiContext, snapshot: CardDbSnapshot): String {
        val transactionsJson = JSONArray().apply {
            snapshot.transactions.forEach { tx ->
                put(JSONObject().apply {
                    put("amount", tx.amount)
                    put("type", tx.type.name)
                    put("note", tx.note ?: "")
                    put("date", tx.date)
                })
            }
        }
        
        val accountsJson = JSONArray().apply {
            snapshot.accounts.forEach { acc ->
                put(JSONObject().apply {
                    put("name", acc.name)
                    put("balance", acc.balance)
                    put("type", acc.type.name)
                })
            }
        }
        
        val budgetJson = JSONArray().apply {
            snapshot.budgets.forEach { bg ->
                put(JSONObject().apply {
                    put("title", bg.title)
                    put("limit", bg.amountLimit)
                    put("spent", bg.spentAmount)
                })
            }
        }

        val savingsJson = JSONArray().apply {
            snapshot.savings.forEach { sg ->
                put(JSONObject().apply {
                    put("title", sg.name)
                    put("target", sg.targetAmount)
                    put("saved", sg.currentAmount)
                })
            }
        }
        
        return """
أنت مستشار مالي شخصي ذكي، ودي، ومحترف جداً مدمج في تطبيق "قداشّ" (Kdach) لإدارة المصاريف والميزانيات في الجزائر.
مهمتك الحالية هي مساعدة المستخدم في فهم وتحليل البطاقة الإحصائية المعروضة أمامه والإجابة على أي أسئلة تخصها.

قواعد عامة للرد والسلوك:
1. تفاعل مع المستخدم بلغة عربية فصحى مبسطة وودية، مع القدرة الكاملة على فهم التعبيرات بالدارجة الجزائرية والرد بلمسة جزائرية محببة (تفهم كلمات مثل "الفلوس"، "الحساب"، "خلصت"، "دّيت"، "شحال عندي"، "شحال خسرت"، "بزاف"، "شوية"، إلخ).
2. افهم أسماء المعاملات المالية المكتوبة بالدارجة وطابقها ذهنياً مع سياقها المالي.
3. قدم شروحات بسيطة جداً ومباشرة للرسم البياني أو الأرقام الظاهرة.
4. قدم بالضبط 3 نصائح عملية ومحددة ومخصصة للتحسين المالي وتقليل النفقات غير الضرورية بناءً على البيانات.
5. حافظ على نبرة إيجابية، مشجعة، وداعمة لذكاء المستخدم المالي.

سياق البطاقة الإحصائية الحالية:
- عنوان البطاقة: ${context.cardTitle}
- نوع البطاقة: ${context.cardType::class.java.simpleName}
- بيانات المخطط البياني المباشرة: ${context.chartData}
- معلومات المساعدة للبطاقة (ToolTip): ${context.tooltipContent}

سياق قاعدة بيانات المستخدم للفترة الحالية:
- المعاملات في هذه الفترة: ${transactionsJson.toString()}
- حسابات المستخدم وأرصدتها: ${accountsJson.toString()}
- الميزانيات المحددة: ${budgetJson.toString()}
- أهداف الادخار: ${savingsJson.toString()}

مهمتك: اشرح النتائج بشكل بسيط، قدم 3 نصائح عملية واضحة للتحسين، وأجب عن أي استفسار يخص هذه البطاقة بدقة واختصار.
        """.trimIndent()
    }
}
