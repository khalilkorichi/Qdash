package com.qdash.data.categorization

import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.data.local.dao.CategoryDao
import com.qdash.data.local.dao.CategoryRuleDao
import com.qdash.data.local.dao.UserCategoryMappingDao
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RuleBasedCategorizationEngine(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val userCategoryMappingDao: UserCategoryMappingDao,
    private val aiRepository: com.qdash.domain.repository.AiRepository
) : CategorizationEngine {

    private val matcher = KeywordMatcher()
    private val scorer = SuggestionScorer()

    override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion =
        withContext(Dispatchers.Default) {
            val normalized = matcher.normalize(title)
            if (normalized.isEmpty()) {
                return@withContext CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
            }

            // 1. Stage 1: Check historical user persistent mappings first (USER_HISTORY)
            val userMapping = userCategoryMappingDao.getMappingByText(normalized)
            if (userMapping != null) {
                return@withContext CategorySuggestion(
                    suggestedCategoryId = userMapping.categoryId,
                    suggestionSource = SuggestionSource.HISTORY,
                    confidenceScore = scorer.calculateConfidence(SuggestionSource.HISTORY, 1.0f),
                    userAcceptedSuggestion = false
                )
            }

            // 2. Stage 2: Check Algerian local dialect keyword dictionary (KEYWORD)
            val systemCategories = categoryDao.getAllCategories().first()
            val kwMatch = AlgerianKeywordDictionary.findMatchingCategory(normalized)
            if (kwMatch != null) {
                // Try finding by subcategory name first if provided, else category name
                val targetCategory = if (kwMatch.subcategoryName != null) {
                    systemCategories.find { it.name == kwMatch.subcategoryName }
                        ?: systemCategories.find { it.name == kwMatch.categoryName }
                } else {
                    systemCategories.find { it.name == kwMatch.categoryName }
                }

                if (targetCategory != null) {
                    return@withContext CategorySuggestion(
                        suggestedCategoryId = targetCategory.id,
                        suggestionSource = SuggestionSource.KEYWORD,
                        confidenceScore = scorer.calculateConfidence(SuggestionSource.KEYWORD, 0.95f),
                        userAcceptedSuggestion = false
                    )
                }
            }

            // 3. Stage 3: Check Active DB Rules (RULE)
            val dbRules = categoryRuleDao.getAllActiveRules()
            for (rule in dbRules) {
                if (matcher.containsKeyword(normalized, rule.keyword)) {
                    return@withContext CategorySuggestion(
                        suggestedCategoryId = rule.categoryId,
                        suggestionSource = SuggestionSource.RULE,
                        confidenceScore = scorer.calculateConfidence(SuggestionSource.RULE, 0.9f),
                        userAcceptedSuggestion = false
                    )
                }
            }

            // 4. Stage 4: AI-Based Fallback (AI)
            try {
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
                    return@withContext CategorySuggestion(
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
                    return@withContext CategorySuggestion(
                        suggestedCategoryId = categoryId,
                        suggestionSource = SuggestionSource.AI,
                        confidenceScore = confidence,
                        userAcceptedSuggestion = false
                    )
                }
            } catch (e: Exception) {
                // Silently fallback if offline/no AI response
            }

            return@withContext CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
        }
}
