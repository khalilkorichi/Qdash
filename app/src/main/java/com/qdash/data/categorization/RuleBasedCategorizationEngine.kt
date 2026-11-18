package com.qdash.data.categorization

import com.qdash.core.preferences.PreferencesManager
import com.qdash.data.local.dao.CategoryDao
import com.qdash.data.local.dao.CategoryRuleDao
import com.qdash.data.local.dao.UserCategoryMappingDao
import com.qdash.data.local.entities.CategoryEntity
import com.qdash.domain.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class RuleBasedCategorizationEngine(
    private val categoryDao: CategoryDao,
    private val categoryRuleDao: CategoryRuleDao,
    private val userCategoryMappingDao: UserCategoryMappingDao,
    private val aiRepository: com.qdash.domain.repository.AiRepository,
    private val preferencesManager: PreferencesManager? = null
) : CategorizationEngine {

    private val matcher = KeywordMatcher()
    private val scorer = SuggestionScorer()

    override suspend fun suggestCategory(
        title: String,
        amount: Double?,
        accountId: Long?
    ): CategorySuggestion = suggestCategory(
        title = title,
        amount = amount,
        accountId = accountId,
        transactionType = null,
        forceAi = false
    )

    override suspend fun suggestCategory(
        title: String,
        amount: Double?,
        accountId: Long?,
        transactionType: String?,
        forceAi: Boolean
    ): CategorySuggestion = withContext(Dispatchers.Default) {
        val normalized = matcher.normalize(title)
        if (normalized.isEmpty() && !forceAi) {
            return@withContext CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
        }

        val expectedType = transactionType?.trim()?.uppercase()?.takeIf {
            it == "EXPENSE" || it == "INCOME"
        }

        val allCategories = categoryDao.getAllCategories().first()
        val candidateCategories = if (expectedType != null) {
            allCategories.filter { it.type.equals(expectedType, ignoreCase = true) }
        } else {
            allCategories
        }
        val candidateIds = candidateCategories.map { it.id }.toSet()

        fun computeLocalMatch(): CategorySuggestion? {
            if (normalized.isEmpty()) return null

            // Stage 1: Historical persistent user mappings (strictly matching transaction type)
            return null // Evaluated asynchronously below
        }

        var fallbackSuggestion: CategorySuggestion? = null

        if (normalized.isNotEmpty()) {
            // 1. Stage 1: Check historical user persistent mappings first (USER_HISTORY)
            val userMapping = userCategoryMappingDao.getMappingByText(normalized)
            if (userMapping != null && userMapping.categoryId in candidateIds) {
                val historyMatch = CategorySuggestion(
                    suggestedCategoryId = userMapping.categoryId,
                    suggestionSource = SuggestionSource.HISTORY,
                    confidenceScore = scorer.calculateConfidence(SuggestionSource.HISTORY, 1.0f),
                    userAcceptedSuggestion = false
                )
                if (!forceAi) return@withContext historyMatch
                fallbackSuggestion = historyMatch
            }

            // 2. Stage 2: Check Algerian Darja & Franco-Algerian keyword dictionary (KEYWORD)
            val kwMatch = AlgerianKeywordDictionary.findMatchingCategory(normalized, expectedType)
            if (kwMatch != null) {
                val targetCategory = resolveKeywordCategory(kwMatch, candidateCategories)
                if (targetCategory != null) {
                    val keywordSuggestion = CategorySuggestion(
                        suggestedCategoryId = targetCategory.id,
                        suggestionSource = SuggestionSource.KEYWORD,
                        confidenceScore = scorer.calculateConfidence(SuggestionSource.KEYWORD, 0.95f),
                        userAcceptedSuggestion = false
                    )
                    if (!forceAi) return@withContext keywordSuggestion
                    if (fallbackSuggestion == null) fallbackSuggestion = keywordSuggestion
                }
            }

            // 3. Stage 3: Check Active DB Rules (RULE) filtered by candidate type
            val dbRules = categoryRuleDao.getAllActiveRules()
            for (rule in dbRules) {
                if (rule.categoryId in candidateIds && matcher.containsKeyword(normalized, rule.keyword)) {
                    val ruleSuggestion = CategorySuggestion(
                        suggestedCategoryId = rule.categoryId,
                        suggestionSource = SuggestionSource.RULE,
                        confidenceScore = scorer.calculateConfidence(SuggestionSource.RULE, 0.9f),
                        userAcceptedSuggestion = false
                    )
                    if (!forceAi) return@withContext ruleSuggestion
                    if (fallbackSuggestion == null) fallbackSuggestion = ruleSuggestion
                    break
                }
            }
        }

        // 4. Stage 4: Algerian Darja & Type-Aware AI Categorization (AI)
        if (candidateCategories.isNotEmpty()) {
            try {
                val parentMap = allCategories.associateBy { it.id }
                val categoriesListStr = candidateCategories
                    .sortedWith(compareBy<CategoryEntity> { it.parentId ?: it.id }.thenBy { it.parentId != null }.thenBy { it.sortOrder })
                    .joinToString("\n") { cat ->
                        val parent = cat.parentId?.let { parentMap[it] }
                        val hierarchyLabel = if (parent != null) {
                            "${parent.name} > ${cat.name} [فئة فرعية]"
                        } else {
                            "${cat.name} [فئة رئيسية]"
                        }
                        "- ID=${cat.id} | $hierarchyLabel | النوع=${cat.type}"
                    }

                val typeArabicDesc = when (expectedType) {
                    "INCOME" -> "دخل / إيراد مالي وارد للمحفظة (INCOME) — مثل شهرية، بريم، مبيعات، فريلانس، بريكول، منحة، كراء عقار، هدية، استرجاع سلف"
                    "EXPENSE" -> "مصروف / نفقة خارجة من المحفظة (EXPENSE) — مثل قضيان، مارشي، فليكسي، سونلغاز، ياسير، ليسونس، فرماسي، قهوة، قش، كرية، صدقة"
                    else -> "معاملة مالية (مصروف أو دخل حسب السياق)"
                }

                val effectiveNote = title.trim().ifEmpty { "معاملة بدون وصف نصي — اعتمد على المبلغ ونوع العملية" }
                val amountDesc = if (amount != null && amount > 0.0) "$amount دج (DZD)" else "غير محدد"

                val prompt = """
                    أنت خبير مالي جزائري متخصص في تصنيف المعاملات المالية بالدارجة الجزائرية، العربية الفصحى، الفرنسية المتداولة في الجزائر، وكتابة العربيزي (Arabizi مثل 3=ع، 7=ح، 9=ق، 5=خ، ch=ش، gh=غ).
                    
                    بيانات العملية المطلوب تصنيفها بدقة:
                    - نوع العملية المحدد من المستخدم: $typeArabicDesc
                    - وصف / ملاحظة العملية: "$effectiveNote"
                    - المبلغ: $amountDesc
                    
                    قائمة الفئات المتاحة المسموح الاختيار منها حصراً:
                    $categoriesListStr
                    
                    قواعد التصنيف الصارمة:
                    1. التزم تماماً بنوع العملية (${expectedType ?: "EXPENSE/INCOME"}) ولا تختر فئة خارج القائمة أعلاه.
                    2. فضّل دائماً اختيار معرّف الفئة الفرعية الأدق [فئة فرعية] عند انطباق المعنى (مثلاً: "فليكسي 200" -> فليكسي ورصيد هاتف، "قرنطيطة ومحاجب" -> أكل سريع وبلاطو، "ليسونس نفطال" -> وقود ونفطال، "بريم المردودية" -> مردودية ومنح العمل).
                    3. راعِ المصطلحات الجزائرية (مثل: قضيان، مارشي، سوبيرات، بوشي، حفاف، لاصال، لي كور، كراش، ليكوش، دروغري، بريكول، تريسيتي، سونلغاز، سيال، الذهبية، بريدي موب، ياليدين، واد كنيس، كناص، شيفة، عدل، شهرية، خلصة، رابال، منحة البطالة، مهيبة، قفة).
                    4. إذا وجدت فئة مناسبة في القائمة (ثقة >= 0.45)، أرجع JSON صالح فقط بهذا الشكل:
                    {
                      "categoryId": 123,
                      "confidence": 0.92,
                      "suggestNewCategory": false
                    }
                    5. فقط إذا لم تناسب أي فئة موجودة إطلاقاً وكان الوصف واضحاً ويتطلب فئة جديدة باللغة العربية تناسب المستخدم الجزائري، أرجع JSON صالح فقط بهذا الشكل:
                    {
                      "categoryId": null,
                      "confidence": 0.85,
                      "suggestNewCategory": true,
                      "newCategoryName": "اسم الفئة المقترح بالعربية",
                      "newCategoryType": "${expectedType ?: "EXPENSE"}",
                      "newCategoryColor": "#3B82F6",
                      "newCategoryIcon": "shopping_bag"
                    }
                    الأيقونات المسموحة لـ newCategoryIcon: (person, ic_cat_family, home, restaurant, ic_cat_transport, ic_cat_flexy, shopping_bag, ic_cat_health, ic_cat_education, sports_esports, celebration, account_balance, volunteer_activism, storefront, work, trending_up, fitness_center, smartphone, local_shipping, build, coffee, checkroom).
                    أجب بكائن JSON خام فقط بدون أي شرح إضافي.
                """.trimIndent()

                val modelToUse = preferencesManager?.selectedAiModel ?: "gemini-2.5-flash"
                val response = aiRepository.generateResponse(prompt, modelToUse)
                val replyText = response.replyText
                val firstBrace = replyText.indexOf('{')
                val lastBrace = replyText.lastIndexOf('}')
                if (firstBrace >= 0 && lastBrace > firstBrace) {
                    val cleanJson = replyText.substring(firstBrace, lastBrace + 1)
                    val json = org.json.JSONObject(cleanJson)

                    val suggestNew = json.optBoolean("suggestNewCategory", false)
                    val confidence = json.optDouble("confidence", 0.85).toFloat().coerceIn(0f, 1f)

                    if (suggestNew) {
                        val proposedName = json.optString("newCategoryName", "").trim().takeIf { it.isNotBlank() }
                        if (proposedName != null) {
                            return@withContext CategorySuggestion(
                                suggestedCategoryId = null,
                                suggestionSource = SuggestionSource.AI,
                                confidenceScore = confidence,
                                userAcceptedSuggestion = false,
                                newCategoryName = proposedName,
                                newCategoryType = expectedType ?: json.optString("newCategoryType", "EXPENSE"),
                                newCategoryColor = json.optString("newCategoryColor", "#3B82F6"),
                                newCategoryIcon = json.optString("newCategoryIcon", "ic_cat_default")
                            )
                        }
                    } else if (!json.isNull("categoryId")) {
                        val categoryId = json.optLong("categoryId", -1L)
                        if (categoryId in candidateIds) {
                            return@withContext CategorySuggestion(
                                suggestedCategoryId = categoryId,
                                suggestionSource = SuggestionSource.AI,
                                confidenceScore = confidence,
                                userAcceptedSuggestion = false
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Gracefully fall back to local match when offline or AI unavailable
            }
        }

        return@withContext fallbackSuggestion ?: CategorySuggestion(null, SuggestionSource.NONE, 0.0f)
    }

    companion object {
        fun resolveKeywordCategory(
            kwMatch: AlgerianKeywordDictionary.KeywordCategoryMapping,
            candidates: List<CategoryEntity>
        ): CategoryEntity? {
            fun findFlexible(targetName: String?, requireSub: Boolean = false): CategoryEntity? {
                if (targetName.isNullOrBlank()) return null
                val aliases = getCategoryAliases(targetName)
                val pool = if (requireSub) candidates.filter { it.parentId != null } else candidates
                return pool.firstOrNull { it.name.equals(targetName, ignoreCase = true) }
                    ?: pool.firstOrNull { it.name.contains(targetName) || targetName.contains(it.name) }
                    ?: pool.firstOrNull { cat -> aliases.any { alias -> cat.name.contains(alias) } }
            }

            return findFlexible(kwMatch.seededSubcategoryTarget, requireSub = true)
                ?: findFlexible(kwMatch.subcategoryName, requireSub = true)
                ?: findFlexible(kwMatch.seededCategoryTarget, requireSub = false)
                ?: findFlexible(kwMatch.categoryName, requireSub = false)
        }

        fun getCategoryAliases(targetName: String): List<String> = when (targetName) {
            "خردوات وعتاد" -> listOf("خردوات ودروغري", "خردوات")
            "خدمات محلية" -> listOf("خدمات محلية وتصليح")
            "عناية شخصية" -> listOf("كوسميتيك وعناية", "حلاقة وتجميل")
            "عائلي" -> listOf("عائلي وكفالة")
            "فواتير" -> listOf("فواتير وشحن")
            "طعام" -> listOf("طعام ومأكولات")
            "بقالة" -> listOf("بقالة ومواد غذائية", "مارشي وخضار")
            "مطاعم" -> listOf("مطاعم ومقاهي", "أكل سريع وبلاطو")
            "مواصلات" -> listOf("مواصلات وتاكسي")
            "تاكسي" -> listOf("تاكسي وتطبيقات", "حافلة وترامواي")
            "وقود" -> listOf("وقود ونفطال")
            "ملابس" -> listOf("ملابس وقش", "تسوق وملابس")
            "إنترنت" -> listOf("إنترنت و ADSL", "فليكسي ورصيد هاتف")
            "صحة" -> listOf("صحة وصيدلية")
            "تعليم" -> listOf("تعليم ودروس")
            "ترفيه" -> listOf("ترفيه وألعاب")
            "ألعاب" -> listOf("ألعاب وتسلية")
            "الخدمات البريدية" -> listOf("الخدمات البريدية (CCP)")
            else -> emptyList()
        }
    }
}
