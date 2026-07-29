package com.qdash.domain.usecase.currency

import com.qdash.domain.model.ExchangeRate
import com.qdash.domain.model.MarketType
import com.qdash.domain.model.RateDirection
import com.qdash.domain.model.RateTrend
import com.qdash.domain.model.TradeDirection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    private lateinit var useCase: ConvertCurrencyUseCase

    private val testRates = listOf(
        ExchangeRate("USD", "دولار أمريكي", "🇺🇸", 137.50, 138.00, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), 242.00, 244.50, null, null, 0L),
        ExchangeRate("EUR", "يورو", "🇪🇺", 148.00, 148.60, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), 260.00, 262.50, null, null, 0L),
        ExchangeRate("GBP", "جنيه إسترليني", "🇬🇧", 172.00, 173.00, RateTrend(RateDirection.STABLE, 0.0), RateTrend(RateDirection.STABLE, 0.0), 305.00, 308.00, null, null, 0L)
    )

    @Before
    fun setUp() {
        useCase = ConvertCurrencyUseCase()
    }

    // ── 4 Scenario Combinations (MarketType x TradeDirection) ───────────────

    @Test
    fun `OFFICIAL x BUY uses official buy rate`() {
        val result = useCase(
            amount = 100.0,
            fromCode = "USD",
            toCode = "DZD",
            rates = testRates,
            marketType = MarketType.OFFICIAL,
            tradeDirection = TradeDirection.BUY
        )
        assertEquals(13750.0, result, 0.01)
    }

    @Test
    fun `OFFICIAL x SELL uses official sell rate`() {
        val result = useCase(
            amount = 100.0,
            fromCode = "USD",
            toCode = "DZD",
            rates = testRates,
            marketType = MarketType.OFFICIAL,
            tradeDirection = TradeDirection.SELL
        )
        assertEquals(13800.0, result, 0.01)
    }

    @Test
    fun `PARALLEL x BUY uses parallel buy rate`() {
        val result = useCase(
            amount = 100.0,
            fromCode = "USD",
            toCode = "DZD",
            rates = testRates,
            marketType = MarketType.PARALLEL,
            tradeDirection = TradeDirection.BUY
        )
        assertEquals(24200.0, result, 0.01)
    }

    @Test
    fun `PARALLEL x SELL uses parallel sell rate`() {
        val result = useCase(
            amount = 100.0,
            fromCode = "USD",
            toCode = "DZD",
            rates = testRates,
            marketType = MarketType.PARALLEL,
            tradeDirection = TradeDirection.SELL
        )
        assertEquals(24450.0, result, 0.01)
    }

    // ── Cross-currency ────────────────────────────────────────────────────────

    @Test
    fun `USD to EUR pivots through DZD`() {
        val result = useCase(
            amount = 100.0,
            fromCode = "USD",
            toCode = "EUR",
            rates = testRates,
            marketType = MarketType.OFFICIAL,
            tradeDirection = TradeDirection.BUY
        )
        assertTrue("Expected ~92.9 but was $result", result in 92.0..94.0)
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `zero amount returns 0`() {
        val result = useCase(0.0, "USD", "DZD", testRates)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `negative amount returns 0`() {
        val result = useCase(-50.0, "USD", "DZD", testRates)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `same currency returns same amount`() {
        val result = useCase(500.0, "USD", "USD", testRates)
        assertEquals(500.0, result, 0.001)
    }

    // ── Isolated Manual rate ──────────────────────────────────────────────────

    @Test
    fun `manual rate calculation overrides MarketType and TradeDirection`() {
        val result = useCase(
            amount = 10.0,
            fromCode = "USD",
            toCode = "DZD",
            rates = testRates,
            marketType = MarketType.PARALLEL,
            tradeDirection = TradeDirection.SELL,
            manualRate = 200.0
        )
        assertEquals(2000.0, result, 0.01)
    }
}
