package com.qdash.data.remote.scraper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ParallelRateScraperTest {

    private lateinit var scraper: ParallelRateScraper

    @Before
    fun setUp() {
        scraper = ParallelRateScraper()
    }

    @Test
    fun `scrapeRates executes safely without throwing unhandled exceptions`() = runBlocking {
        val result = scraper.scrapeRates()
        assertNotNull(result)
    }
}
