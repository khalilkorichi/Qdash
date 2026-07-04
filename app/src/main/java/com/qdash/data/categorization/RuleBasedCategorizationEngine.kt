package com.qdash.data.categorization

import com.qdash.data.local.dao.CategoryDao
import com.qdash.data.local.dao.CategoryRuleDao
import com.qdash.data.local.dao.UserCategoryMappingDao
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import kotlinx.coroutines.flow.first

class RuleBasedCategorizationEngine(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val userCategoryMappingDao: UserCategoryMappingDao,
    private val aiRepository: com.qdash.domain.repository.AiRepository
) : CategorizationEngine {

    private val matcher = KeywordMatcher()
    private val scorer = SuggestionScorer()

    override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion {
        val normalized = matcher.normalize(title)
        if (normalized.isEmpty()) {
            return CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
        }

        // 1. Check user persistent mappings first
        val userMapping = userCategoryMappingDao.getMappingByText(normalized)
        if (userMapping != null) {
            return CategorySuggestion(
                suggestedCategoryId = userMapping.categoryId,
                suggestionSource = SuggestionSource.HISTORY,
                confidenceScore = scorer.calculateConfidence(SuggestionSource.HISTORY, 1.0f),
                userAcceptedSuggestion = false
            )
        }

        // 2. Check DB Rules
        val dbRules = categoryRuleDao.getAllActiveRules()
        for (rule in dbRules) {
            if (matcher.containsKeyword(normalized, rule.keyword)) {
                return CategorySuggestion(
                    suggestedCategoryId = rule.categoryId,
                    suggestionSource = SuggestionSource.RULE,
                    confidenceScore = scorer.calculateConfidence(SuggestionSource.RULE, 0.9f),
                    userAcceptedSuggestion = false
                )
            }
        }

        // 3. Fallback: Heuristics matched to system categories
        val systemCategories = categoryDao.getAllCategories().first()
        
        // Let's map normalized keywords to Category names
        val keywordsToCategoryName = mapOf(
            listOf("netflix", "spotify", "نتفلكس", "نتفليكس", "سبوتيفاي", "steam", "playstation", "pubg", "gaming", "العاب", "ترفيه", "جيمينغ") to "ألعاب",
            listOf("uber", "taxi", "وجيز", "heetch", "سيارة", "سائق", "تاكسي", "حافلة", "قطار", "مواصلات", "كلوندستان", "كلونديستان", "بيس", "كار", "ترام", "ترامواي", "ياسير", "yassir", "وجيز", "weshare", "كورسا", "كورس") to "تاكسي",
            listOf("sonelgaz", "ade", "telecom", "mobilis", "djezzy", "ooredoo", "فواتير", "ماء", "كهرباء", "غاز", "انترنت", "شحن", "تريسيتي", "سيال", "مودم", "موديم", "فليكسي", "شحن هاتف") to "كهرباء وغاز",
            listOf("bim", "superette", "supermarket", "groceries", "خضار", "فواكه", "لحم", "دجاج", "حليب", "خبز", "طعام", "مطعم", "قهوة", "اكل", "قضيان", "سوبيرات", "حانوت", "جزار", "ماكلة", "فاست فود", "شواية", "محاجب", "كاران", "بيتزا", "قهاوي", "أتاي", "شاي") to "بقالة",
            listOf("pharmacy", "clinique", "دواء", "طبيب", "صيدلية", "مستشفي", "صحة", "فرماسي", "دوا", "راديو", "تحاليل", "ليزاناليز") to "صحة",
            listOf("school", "كتاب", "ادوات", "دراسة", "جامعة", "تعليم", "مدرسة", "دورة", "كور", "ليكوش", "كوش", "روضة", "كراش") to "تعليم",
            listOf("شراء", "تسوق", "لباس", "حذاء", "ملابس", "shop", "boutique", "هدايا", "حوايج", "قش", "صباط", "جلابة", "قاطو", "عرس", "حفلة", "كادو") to "ملابس",
            listOf("dar", "اجار", "كراء", "اثاث", "منزل", "كرية", "تريسيان", "بلومبي", "صباغ", "بريكولاج", "شونطي") to "إيجار",
            listOf("عائلة", "اولاد", "زوجة", "اطفال") to "عائلي",
            listOf("شخصي", "حلاقة", "شعر", "كوسميتيك", "حجام", "دوش", "حمام") to "عناية شخصية",
            listOf("راتب", "سيرك", "salary", "paye", "خلصة", "الرصيد", "الراتب", "الشهري") to "راتب",
            listOf("كبش", "علف", "اضحية", "عيد الكبير", "الاضحى", "كبش العيد") to "عيد الأضحى",
            listOf("رمضان", "زلابية", "قلب اللوز", "ديول", "سحور") to "رمضان",
            listOf("طابلي", "محفظة", "مئزر", "كراس", "كتب مدرسية", "مستلزمات مدرسية") to "الدخول المدرسي",
            listOf("ذهبية", "الذهبية", "حقوق ccp", "سي سي بي", "دروا سي سي بي", "كارني شيك", "شيك", "اقتطاع") to "مستحقات البريد (CCP)"
        )

        for ((keywords, categoryName) in keywordsToCategoryName) {
            if (keywords.any { matcher.containsKeyword(normalized, it) }) {
                // Find matching category in DB
                val matchedCategory = systemCategories.find { it.name == categoryName }
                if (matchedCategory != null) {
                    return CategorySuggestion(
                        suggestedCategoryId = matchedCategory.id,
                        suggestionSource = SuggestionSource.RULE,
                        confidenceScore = scorer.calculateConfidence(SuggestionSource.RULE, 0.7f),
                        userAcceptedSuggestion = false
                    )
                }
            }
        }

        // 4. AI-Based Fallback
        try {
            val systemCategories = categoryDao.getAllCategories().first()
            val categoriesListStr = systemCategories.map { "${it.id}: ${it.name} (${it.type})" }.joinToString("\n")
            val prompt = """
                You are a smart financial categorizer. Classify the transaction title "$title" (amount: ${amount ?: "unknown"}) into exactly one of these category IDs if it is a reasonable fit (confidence > 0.5):
                $categoriesListStr
                
                If one of these existing categories is a good fit, return ONLY a valid JSON object in this format:
                {
                  "categoryId": Long,
                  "confidence": Float,
                  "suggestNewCategory": false
                }
                
                If NO existing category is a good match and a new category is needed to describe this transaction properly, suggest a new category name (in Arabic), type (EXPENSE or INCOME), a hex color string, and a standard material design icon name:
                {
                  "categoryId": null,
                  "confidence": Float,
                  "suggestNewCategory": true,
                  "newCategoryName": "Suggested Name (Arabic)",
                  "newCategoryType": "EXPENSE",
                  "newCategoryColor": "#HexColorString",
                  "newCategoryIcon": "IconName (e.g. sports_esports, build, account_balance)"
                }
            """.trimIndent()

            val response = aiRepository.generateResponse(prompt, "gemini-2.5-flash")
            val replyText = response.replyText
            val cleanJson = replyText.substringAfter("{").substringBeforeLast("}").let { "{$it}" }
            val json = org.json.JSONObject(cleanJson)
            
            val suggestNew = json.optBoolean("suggestNewCategory", false)
            val confidence = json.optDouble("confidence", 0.8).toFloat()
            
            if (suggestNew) {
                return CategorySuggestion(
                    suggestedCategoryId = null,
                    suggestionSource = SuggestionSource.AI,
                    confidenceScore = confidence,
                    userAcceptedSuggestion = false,
                    newCategoryName = json.optString("newCategoryName", null),
                    newCategoryType = json.optString("newCategoryType", "EXPENSE"),
                    newCategoryColor = json.optString("newCategoryColor", "#E0E0E0"),
                    newCategoryIcon = json.optString("newCategoryIcon", "category")
                )
            } else if (!json.isNull("categoryId")) {
                val categoryId = json.getLong("categoryId")
                return CategorySuggestion(
                    suggestedCategoryId = categoryId,
                    suggestionSource = SuggestionSource.AI,
                    confidenceScore = confidence,
                    userAcceptedSuggestion = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
    }
}
