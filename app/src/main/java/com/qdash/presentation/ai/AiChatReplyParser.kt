package com.qdash.presentation.ai

import com.qdash.domain.model.Transaction
import com.qdash.domain.model.TransactionType
import com.qdash.domain.model.Account
import com.qdash.domain.model.Category
import com.qdash.domain.model.TransferDraftState
import com.qdash.domain.model.SelectedAccountDetailsState
import com.qdash.domain.model.RecentActivitySummary
import com.qdash.domain.model.WalletDistributionSuggestion
import com.qdash.domain.model.LowBalanceAlertState
import com.qdash.domain.model.QuickImpactPreviewState
import com.qdash.domain.repository.TransactionRepository
import com.qdash.domain.repository.SavingRepository
import com.qdash.domain.usecase.ai.GetRecentActivitySummaryUseCase
import com.qdash.domain.usecase.ai.GetWalletDistributionUseCase
import com.qdash.domain.usecase.ai.EvaluateLowBalanceAlertsUseCase
import com.qdash.domain.usecase.ai.GetQuickImpactPreviewUseCase
import kotlinx.coroutines.flow.first

class AiChatReplyParser(
    private val transactionRepository: TransactionRepository,
    private val savingRepository: SavingRepository,
    private val getRecentActivitySummaryUseCase: GetRecentActivitySummaryUseCase,
    private val getWalletDistributionUseCase: GetWalletDistributionUseCase,
    private val evaluateLowBalanceAlertsUseCase: EvaluateLowBalanceAlertsUseCase,
    private val getQuickImpactPreviewUseCase: GetQuickImpactPreviewUseCase
) {

    fun isGeneralBalanceReply(text: String): Boolean {
        val lower = text.lowercase()
        val generalMarkers = listOf(
            "إجمالي رصيد", "رصيد المحفظة", "رصيدك الإجمالي", "مجموع أرصدة", "إجمالي أرصدة",
            "رصيد", "الرصيد", "أرصدة", "الأرصدة", "رصيدي", "أموالي", "فلوسي", "ميزانيتي", "الميزانية", "المحفظة",
            "إجمالي الدخل", "إجمالي المصاريف",
            "balance", "balances", "portfolio", "wallet", "total balance", "cash", "money", "dzd"
        )
        return generalMarkers.any { lower.contains(it) }
    }

    fun isTransactionDraftReply(text: String): Boolean {
        val lower = text.lowercase()
        val draftMarkers = listOf(
            "مسودة معاملة", "معاملة مقترحة", "تأكيد المعاملة", "هل تريد تأكيد", "سأقوم بتسجيل",
            "تم فهم المعاملة", "سجلت لك", "اقتراح تسجيل", "مسودة مصروف", "مسودة دخل", "معاملة جديدة",
            "هل تأكد", "أرغب في تسجيل", "سأسجل", "قم بتسجيل", "معاملة بـ", "مبلغ المعاملة",
            "transaction draft", "proposed transaction", "confirm transaction", "record transaction"
        )
        val actionMarkers = listOf("شراء", "شريت", "صرفت", "دخل", "راتب", "أودعت", "مصروف", "مصاريف",
            "سجل", "سأقوم", "دفع", "دفعت", "اشتريت", "خصمت", "أضفت", "حصلت على", "استلمت", "ربحت")
        return draftMarkers.any { lower.contains(it) } ||
            (actionMarkers.any { lower.contains(it) } && parseDarijaAmount(text) != null)
    }

    fun isRecentActivityReply(text: String): Boolean {
        val lower = text.lowercase()
        val activityMarkers = listOf(
            "آخر المعاملات", "آخر معاملات", "آخر حركة", "آخر الحركات", "النشاط الأخير",
            "المعاملات الأخيرة", "سجل المعاملات", "تاريخ المعاملات", "الحركات الأخيرة",
            "آخر 10", "آخر 5", "أخيرة", "المعاملات السابقة", "سجل الإنفاق",
            "recent activity", "last transactions", "recent transactions", "transaction history",
            "last 10", "last 5", "previous transactions"
        )
        return activityMarkers.any { lower.contains(it) }
    }

    fun isWalletDistributionReply(text: String): Boolean {
        val lower = text.lowercase()
        val distributionMarkers = listOf(
            "توزيع المحفظة", "توزيع أموالك", "توزيع الحسابات", "نسبة توزيع", "كيف تتوزع", "توزيع أرصدتك",
            "نسبة كل حساب", "توزيع مدخراتك", "توزيع ثروتك", "نسبة الأموال", "حصة كل حساب",
            "wallet distribution", "portfolio distribution", "distribution of funds", "fund allocation"
        )
        return distributionMarkers.any { lower.contains(it) }
    }

    fun isLowBalanceAlertReply(text: String): Boolean {
        val lower = text.lowercase()
        val alertMarkers = listOf(
            "رصيد منخفض", "الأرصدة المنخفضة", "تنبيه رصيد", "حد الرصيد", "تنبيه الرصيد",
            "رصيد ضعيف", "تحذير رصيد", "الحساب منخفض", "رصيد قليل", "رصيد صغير",
            "حد أدنى", "تحت الحد", "تجاوز الحد", "يحذر", "خطر الرصيد",
            "low balance", "balance alert", "low balance alert", "minimum balance", "balance warning"
        )
        return alertMarkers.any { lower.contains(it) }
    }

    fun isTransferDraftReply(text: String): Boolean {
        val lower = text.lowercase()
        val transferMarkers = listOf(
            "مسودة تحويل", "تحويل مقترح", "تأكيد التحويل", "حول من", "تحويل من", "نقل من",
            "نقل أموال", "تحويل مبلغ", "سأقوم بتحويل", "تحويل داخلي", "نقل داخلي",
            "تحويل إلى", "تحويل بين الحسابات", "تحويل الرصيد",
            "transfer draft", "proposed transfer", "confirm transfer", "internal transfer"
        )
        return transferMarkers.any { lower.contains(it) }
    }

    fun isSelectedAccountDetailsReply(text: String, accounts: List<Account>): Boolean {
        val lower = text.lowercase()
        val detailsMarkers = listOf(
            "تفاصيل الحساب", "معلومات الحساب", "كشف الحساب", "account details",
            "رصيد حساب", "رصيد الحساب", "تفاصيل حساب", "حساب الـ", "حساب ال",
            "معلومات عن حساب", "بيانات الحساب", "إحصائيات الحساب"
        )
        val nameMatch = accounts.any {
            val accName = it.name.lowercase()
            lower.contains(accName) ||
            (it.type.name == "CCP" && (lower.contains("ccp") || lower.contains("بريدي"))) ||
            (it.type.name == "CASH" && (lower.contains("كاش") || lower.contains("نقدي") || lower.contains("نقد"))) ||
            (it.type.name == "BARIDIMOB" && (lower.contains("بريدي موب") || lower.contains("baridimob"))) ||
            (it.type.name == "BANK" && lower.contains("بنك"))
        }
        return (detailsMarkers.any { lower.contains(it) } || lower.contains("رصيد")) && nameMatch
    }

    fun isQuickImpactPreviewReply(text: String): Boolean {
        val lower = text.lowercase()
        val impactMarkers = listOf(
            "التأثير المالي", "تأثير سريع", "تأثير على ميزانيتك", "تأثير على رصيدك",
            "التأثير على", "quick impact", "financial impact", "budget impact",
            "ماذا سيحدث", "بعد المعاملة", "بعد الإضافة", "سيتغير رصيدك",
            "نتيجة المعاملة", "تداعيات مالية"
        )
        return impactMarkers.any { lower.contains(it) }
    }

    fun buildWalletSnapshot(accounts: List<Account>): WalletSnapshot {
        val totalBalance = accounts.filter { !it.isArchived }.sumOf { it.balance }
        val items = accounts.filter { !it.isArchived }.map { acc ->
            val typeLabel = when (acc.type) {
                com.qdash.domain.model.AccountType.BANK -> "بنك"
                com.qdash.domain.model.AccountType.CCP -> "CCP"
                com.qdash.domain.model.AccountType.BARIDIMOB -> "بريدي موب"
                com.qdash.domain.model.AccountType.CASH -> "نقداً"
                com.qdash.domain.model.AccountType.SAVINGS -> "ادخار"
                com.qdash.domain.model.AccountType.WALLET -> "محفظة"
                com.qdash.domain.model.AccountType.OTHER -> "أخرى"
            }
            AccountBalanceItem(
                id = acc.id,
                name = acc.name,
                typeLabel = typeLabel,
                balance = acc.balance,
                currency = acc.currency,
                color = acc.color
            )
        }
        return WalletSnapshot(totalBalance = totalBalance, currency = "دج", accounts = items)
    }

    suspend fun parseTransferDraftFromText(text: String, accounts: List<Account>): TransferDraftState? {
        val lowerText = text.lowercase()
        val isTransfer = lowerText.contains("تحويل") || lowerText.contains("حول")
        if (isTransfer) {
            val amount = parseDarijaAmount(text) ?: 500.0
            
            var fromAcc = accounts.firstOrNull { lowerText.contains(it.name.lowercase()) }
            var toAcc = accounts.filter { it.id != fromAcc?.id }.firstOrNull { lowerText.contains(it.name.lowercase()) }
            
            if (fromAcc == null) {
                fromAcc = accounts.firstOrNull()
            }
            if (toAcc == null) {
                toAcc = accounts.firstOrNull { it.id != fromAcc?.id } ?: accounts.firstOrNull()
            }
            
            val note = "تحويل مسجل عن طريق المساعد الذكي"
            
            return TransferDraftState(
                amount = amount,
                fromAccountId = fromAcc?.id ?: 1L,
                toAccountId = toAcc?.id ?: 2L,
                note = note,
                fromAccountName = fromAcc?.name ?: "غير محدد",
                toAccountName = toAcc?.name ?: "غير محدد"
            )
        }
        return null
    }

    suspend fun buildSelectedAccountDetails(text: String, accounts: List<Account>): SelectedAccountDetailsState? {
        val matchedAccount = accounts.firstOrNull { acc ->
            text.lowercase().contains(acc.name.lowercase())
        } ?: accounts.firstOrNull() ?: return null
        
        val recentTxs = transactionRepository.getTransactionsByAccount(matchedAccount.id).first().take(3)
        val goals = savingRepository.getAllSavingGoals().first().filter { it.accountId == matchedAccount.id }
        
        return SelectedAccountDetailsState(
            account = matchedAccount,
            recentTransactions = recentTxs,
            activeGoals = goals
        )
    }

    fun parseDarijaAmount(text: String): Double? {
        val clean = text.replace("،", "").replace(",", "").trim()
        
        val millionRegex = """(\d+)\s*(?:مليون|ملاين|ملايين)""".toRegex()
        val millionMatch = millionRegex.find(clean)
        if (millionMatch != null) {
            return millionMatch.groupValues[1].toDouble() * 10000.0
        }
        if (clean.contains("زوج ملاين") || clean.contains("زوج ملايين") || clean.contains("2 ملاين")) {
            return 20000.0
        }
        if (clean.contains("مليون") && !clean.contains("مليونين")) {
            return 10000.0
        }

        val thousandRegex = """(\d+)\s*(?:الف|ألف|الاف|آلاف)""".toRegex()
        val thousandMatch = thousandRegex.find(clean)
        if (thousandMatch != null) {
            return thousandMatch.groupValues[1].toDouble() * 10.0
        }
        
        if (clean.contains("عشرة الاف") || clean.contains("عشرة آلاف") || clean.contains("عشرتلاف") || clean.contains("عشرتالاف")) {
            return 100.0
        }
        if (clean.contains("عشرين الف") || clean.contains("عشرين ألف")) {
            return 200.0
        }
        if (clean.contains("خمسين الف") || clean.contains("خمسين ألف")) {
            return 500.0
        }
        if (clean.contains("مية الف") || clean.contains("مية ألف") || clean.contains("مائة ألف")) {
            return 1000.0
        }

        val francRegex = """(\d+)\s*(?:فرنك|فرانك)""".toRegex()
        val francMatch = francRegex.find(clean)
        if (francMatch != null) {
            return francMatch.groupValues[1].toDouble() / 100.0
        }

        val doroRegex = """(\d+)\s*(?:دورو)""".toRegex()
        val doroMatch = doroRegex.find(clean)
        if (doroMatch != null) {
            return doroMatch.groupValues[1].toDouble() * 0.05
        }

        val dzdRegex = """(\d+(?:\.\d+)?)\s*(?:دج|دينار|da|dzd)""".toRegex(RegexOption.IGNORE_CASE)
        val dzdMatch = dzdRegex.find(clean)
        if (dzdMatch != null) {
            return dzdMatch.groupValues[1].toDouble()
        }

        val genericRegex = """\b(\d+(?:\.\d+)?)\b""".toRegex()
        val genericMatch = genericRegex.find(clean)
        if (genericMatch != null) {
            return genericMatch.groupValues[1].toDouble()
        }

        return null
    }

    fun findCategoryByKeywords(text: String, categories: List<Category>, type: TransactionType): Category? {
        val clean = text.lowercase()
        val transportKeywords = listOf("مازوت", "توموبيل", "طوموبيل", "ترونسبور", "كار", "طاكسي", "مواصلات", "بنزين", "ايسونس", "سيارة")
        val foodKeywords = listOf("حليب", "خبز", "قضيان", "ماكلة", "مطعم", "قهوة", "شاي", "عشاء", "غداء", "فطور", "سوبرماركت", "خضار", "فواكه")
        val billsKeywords = listOf("تريسيتي", "ماء", "غاز", "فليكسي", "انترنت", "كونيكسيو", "كارط", "فاتورة", "كهرباء", "هاتف")
        val shoppingKeywords = listOf("شريت", "حوايج", "لبسة", "سباط", "مشتريات", "تيكنولوجيا", "تليفون")
        
        val targetKeywordGroup = when {
            transportKeywords.any { clean.contains(it) } -> transportKeywords
            foodKeywords.any { clean.contains(it) } -> foodKeywords
            billsKeywords.any { clean.contains(it) } -> billsKeywords
            shoppingKeywords.any { clean.contains(it) } -> shoppingKeywords
            else -> emptyList()
        }
        
        if (targetKeywordGroup.isNotEmpty()) {
            val matched = categories.firstOrNull { cat ->
                cat.type.name == type.name && (
                    targetKeywordGroup.any { keyword -> cat.name.contains(keyword) || keyword.contains(cat.name) }
                )
            }
            if (matched != null) return matched
        }
        
        return categories.firstOrNull { it.type.name == type.name } ?: categories.firstOrNull()
    }

    fun parseDraftTransactionFromText(text: String, accounts: List<Account>, categories: List<Category>): Transaction? {
        val lowerText = text.lowercase()
        val containsKeywords = lowerText.contains("مسودة") || 
                lowerText.contains("سجل") || 
                lowerText.contains("شريت") || 
                lowerText.contains("صرفت") || 
                lowerText.contains("دخلت") || 
                lowerText.contains("أودعت") || 
                lowerText.contains("حول") || 
                lowerText.contains("فرنك") || 
                lowerText.contains("دج")
                
        if (containsKeywords) {
            val amount = parseDarijaAmount(text) ?: return null
            
            var extractedNote = "عملية مسجلة بالصوت/الأمر الذكي"
            val noteKeywords = listOf("شريت", "سجل", "سجلي", "صرفت", "لشراء", "مقابل", "على", "بخصوص", "غرض", "شراء")
            for (keyword in noteKeywords) {
                if (text.contains(keyword)) {
                    val index = text.indexOf(keyword) + keyword.length
                    val rest = text.substring(index).trim()
                    if (rest.isNotEmpty()) {
                        val endIdx = rest.indexOfAny(charArrayOf('.', '\n', ',', '،', '؛', '!', 'ب', 'd', 'D', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'))
                        val rawNote = if (endIdx != -1) rest.substring(0, endIdx).trim() else rest
                        if (rawNote.isNotEmpty()) {
                            extractedNote = rawNote.take(40)
                            break
                        }
                    }
                }
            }
            if (extractedNote.length < 2) {
                extractedNote = "عملية مالية"
            }

            val type = if (lowerText.contains("راتب") || lowerText.contains("دخل") || lowerText.contains("وارد") || lowerText.contains("أودع") || lowerText.contains("خلصت")) {
                TransactionType.INCOME
            } else if (lowerText.contains("تحويل") || lowerText.contains("حول")) {
                TransactionType.TRANSFER
            } else {
                TransactionType.EXPENSE
            }

            val category = findCategoryByKeywords(text, categories, type)

            return Transaction(
                amount = amount,
                type = type,
                categoryId = category?.id ?: 1L,
                accountId = accounts.firstOrNull()?.id ?: 1L,
                note = extractedNote,
                date = System.currentTimeMillis()
            )
        }
        return null
    }

    suspend fun getQuickImpactPreview(amount: Double, type: TransactionType, categoryId: Long?, accountId: Long?): QuickImpactPreviewState? {
        return getQuickImpactPreviewUseCase(amount, type, categoryId, accountId)
    }

    suspend fun getRecentActivitySummary(): RecentActivitySummary {
        return getRecentActivitySummaryUseCase()
    }

    suspend fun getWalletDistributionSuggestion(): WalletDistributionSuggestion {
        return getWalletDistributionUseCase()
    }

    suspend fun evaluateLowBalanceAlerts(): LowBalanceAlertState {
        return evaluateLowBalanceAlertsUseCase()
    }
}
