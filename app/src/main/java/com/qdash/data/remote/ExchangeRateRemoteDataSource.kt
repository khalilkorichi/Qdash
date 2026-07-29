package com.qdash.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.qdash.data.local.entities.ExchangeRateEntity
import com.qdash.domain.model.CURRENCY_ARABIC_NAMES
import com.qdash.domain.model.SUPPORTED_CURRENCIES
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

// ── DTOs ────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ExchangeRateApiResponse(
    /** Map of currency code → exchange rate relative to USD base. */
    @Json(name = "rates") val rates: Map<String, Double>
)

// ── Retrofit Service ─────────────────────────────────────────────────────────

/** Free, no-key-required API from exchangerate-api.com */
interface ExchangeRateApiService {
    /**
     * Returns rates where USD = 1.0 and all others are relative to USD.
     * e.g., { "DZD": 137.50, "EUR": 0.92, … }
     */
    @GET("v4/latest/USD")
    suspend fun getRatesRelativeToUsd(): ExchangeRateApiResponse
}

// ── Remote Data Source ───────────────────────────────────────────────────────

class ExchangeRateRemoteDataSource {

    private val service: ExchangeRateApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.exchangerate-api.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRateApiService::class.java)
    }

    /**
     * Fetch official exchange rates.
     * Strategy: request rates relative to USD, extract DZD rate, then compute
     * each supported currency's rate in DZD = usdInDzd / currencyInUsd.
     *
     * Returns a [Result] wrapping entities ready for Room upsert.
     * Skips currencies not present in API response.
     */
    suspend fun fetchOfficialRates(): Result<List<ExchangeRateEntity>> = runCatching {
        val response = service.getRatesRelativeToUsd()
        val rates = response.rates

        val usdInDzd = rates["DZD"] ?: error("DZD not found in API response")
        val now = System.currentTimeMillis()

        // Map of ISO code → flag emoji
        val flagEmojis = mapOf(
            "USD" to "🇺🇸", "EUR" to "🇪🇺", "GBP" to "🇬🇧",
            "CAD" to "🇨🇦", "SAR" to "🇸🇦", "AED" to "🇦🇪", "TND" to "🇹🇳"
        )

        SUPPORTED_CURRENCIES.mapNotNull { code ->
            val rateVsUsd = rates[code] ?: return@mapNotNull null
            // 1 USD = usdInDzd DZD → 1 [code] = (usdInDzd / rateVsUsd) DZD
            val dzdPerUnit = usdInDzd / rateVsUsd
            // Use a small spread (0.2%) to simulate buy/sell difference
            val spread = dzdPerUnit * 0.002
            ExchangeRateEntity(
                currencyCode = code,
                countryFlagEmoji = flagEmojis[code] ?: "🏳",
                officialBuyRate = dzdPerUnit - spread,
                officialSellRate = dzdPerUnit + spread,
                parallelBuyRate = null,
                parallelSellRate = null,
                source = "REMOTE",
                lastUpdatedAt = now
            )
        }
    }
}
