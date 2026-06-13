package com.example.data.categorization

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CategoryRuleDao
import com.example.data.local.dao.UserCategoryMappingDao
import com.example.domain.model.CategorySuggestion
import com.example.domain.model.SuggestionSource
import kotlinx.coroutines.flow.first

class RuleBasedCategorizationEngine(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val userCategoryMappingDao: UserCategoryMappingDao
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
            listOf("netflix", "spotify", "نتفلكس", "نتفليكس", "سبوتيفاي", "steam", "playstation", "pubg", "gaming", "العاب", "ترفيه") to "ترفيه",
            listOf("uber", "taxi", "وجيز", "heetch", "سيارة", "سائق", "تاكسي", "حافلة", "قطار", "مواصلات") to "مواصلات",
            listOf("sonelgaz", "ade", "telecom", "mobilis", "djezzy", "ooredoo", "فواتير", "ماء", "كهرباء", "غاز", "انترنت", "شحن") to "فواتير",
            listOf("bim", "superette", "supermarket", "groceries", "خضار", "فواكه", "لحم", "دجاج", "حليب", "خبز", "طعام", "مطعم", "قهوة", "اكل") to "طعام",
            listOf("pharmacy", "clinique", "دواء", "طبيب", "صيدلية", "مستشفي", "صحة") to "صحة",
            listOf("school", "كتاب", "ادوات", "دراسة", "جامعة", "تعليم", "مدرسة", "دورة") to "تعليم",
            listOf("شراء", "تسوق", "لباس", "حذاء", "ملابس", "shop", "boutique", "ملابس", "هدايا") to "تسوق",
            listOf("دار", "اجار", "كراء", "اثاث", "منزل") to "منزلي",
            listOf("عائلة", "اولاد", "زوجة", "اطفال") to "عائلي",
            listOf("شخصي", "حلاقة", "شعر", "كوسميتيك") to "شخصي",
            listOf("راتب", "سيرك", "salary", "paye") to "راتب"
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

        return CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
    }
}
