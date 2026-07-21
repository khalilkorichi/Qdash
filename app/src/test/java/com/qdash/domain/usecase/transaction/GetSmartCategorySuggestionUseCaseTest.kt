package com.qdash.domain.usecase.transaction

import com.qdash.data.categorization.CategorizationEngine
import com.qdash.domain.model.CategorySuggestion
import com.qdash.domain.model.SuggestionSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GetSmartCategorySuggestionUseCaseTest {

    private lateinit var useCase: GetSmartCategorySuggestionUseCase
    private lateinit var mockEngine: FakeCategorizationEngine

    @Before
    fun setUp() {
        mockEngine = FakeCategorizationEngine()
        useCase = GetSmartCategorySuggestionUseCase(mockEngine)
    }

    @Test
    fun `empty or blank title returns NONE suggestion without calling engine`() = runBlocking {
        val result = useCase("")
        assertEquals(SuggestionSource.NONE, result.suggestionSource)
        assertEquals(null, result.suggestedCategoryId)

        val resultBlank = useCase("   ")
        assertEquals(SuggestionSource.NONE, resultBlank.suggestionSource)
        assertEquals(null, resultBlank.suggestedCategoryId)
    }

    @Test
    fun `valid title returns suggestion from engine`() = runBlocking {
        mockEngine.stubResult = CategorySuggestion(
            suggestedCategoryId = 50L,
            suggestionSource = SuggestionSource.KEYWORD,
            confidenceScore = 0.95f
        )

        val result = useCase("فليكسي جيزي")
        assertNotNull(result)
        assertEquals(50L, result.suggestedCategoryId)
        assertEquals(SuggestionSource.KEYWORD, result.suggestionSource)
        assertEquals(0.95f, result.confidenceScore, 0.01f)
    }

    @Test
    fun `historical mapping priority overrides keyword mapping`() = runBlocking {
        mockEngine.stubResult = CategorySuggestion(
            suggestedCategoryId = 100L,
            suggestionSource = SuggestionSource.HISTORY,
            confidenceScore = 1.0f
        )

        val result = useCase("قهوة العصر")
        assertEquals(100L, result.suggestedCategoryId)
        assertEquals(SuggestionSource.HISTORY, result.suggestionSource)
    }

    private class FakeCategorizationEngine : CategorizationEngine {
        var stubResult: CategorySuggestion = CategorySuggestion(null, SuggestionSource.NONE, 0.0f)

        override suspend fun suggestCategory(title: String, amount: Double?, accountId: Long?): CategorySuggestion {
            return stubResult
        }
    }
}
