package com.qdash.domain.usecase.currency

import com.qdash.domain.model.RateDirection
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateRateTrendUseCaseTest {

    private lateinit var useCase: CalculateRateTrendUseCase

    @Before
    fun setUp() {
        useCase = CalculateRateTrendUseCase()
    }

    @Test
    fun `rate increase returns UP direction with correct percentage`() {
        val result = useCase(currentRate = 138.0, previousRate = 136.0)
        assertEquals(RateDirection.UP, result.direction)
        assertEquals(1.5, result.changePercentage, 0.01)
    }

    @Test
    fun `rate decrease returns DOWN direction with correct percentage`() {
        val result = useCase(currentRate = 135.0, previousRate = 138.0)
        assertEquals(RateDirection.DOWN, result.direction)
        assertEquals(-2.2, result.changePercentage, 0.01)
    }

    @Test
    fun `equal rates return STABLE direction with zero percentage`() {
        val result = useCase(currentRate = 137.5, previousRate = 137.5)
        assertEquals(RateDirection.STABLE, result.direction)
        assertEquals(0.0, result.changePercentage, 0.001)
    }

    @Test
    fun `null previous rate returns STABLE direction`() {
        val result = useCase(currentRate = 148.2, previousRate = null)
        assertEquals(RateDirection.STABLE, result.direction)
        assertEquals(0.0, result.changePercentage, 0.001)
    }
}
