package com.moneytracker.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

data class StockQuote(
    val symbol: String,
    val currentPrice: Double,
    val dayChangePercent: Double?,
    val currency: String? = null
)

@Singleton
class StockPriceService @Inject constructor() {

    suspend fun fetchQuote(symbol: String): StockQuote? = withContext(Dispatchers.IO) {
        if (symbol.isBlank()) return@withContext null

        val encodedSymbol = URLEncoder.encode(symbol.trim(), "UTF-8")
        val endpoints = listOf(
            "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=1d",
            "https://query2.finance.yahoo.com/v8/finance/chart/$encodedSymbol?interval=1d"
        )

        for (endpoint in endpoints) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(endpoint)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                    )
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode in 200..299) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val result = json.optJSONObject("chart")?.optJSONArray("result")?.optJSONObject(0)
                    val meta = result?.optJSONObject("meta")
                    if (meta != null) {
                        val price = meta.optDouble("regularMarketPrice", Double.NaN)
                        if (!price.isNaN() && price > 0.0) {
                            val changePercent = meta.optDouble("regularMarketChangePercent", Double.NaN)
                                .takeIf { !it.isNaN() }
                            val currency = meta.optString("currency", "").takeIf { it.isNotBlank() }
                            return@withContext StockQuote(
                                symbol = symbol,
                                currentPrice = price,
                                dayChangePercent = changePercent,
                                currency = currency
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint
            } finally {
                connection?.disconnect()
            }
        }
        null
    }

    suspend fun fetchQuotesBatch(symbols: List<String>): Map<String, StockQuote> = withContext(Dispatchers.IO) {
        if (symbols.isEmpty()) return@withContext emptyMap()
        val distinctSymbols = symbols.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        coroutineScope {
            val deferredList = distinctSymbols.map { sym ->
                async {
                    sym to fetchQuote(sym)
                }
            }
            deferredList.awaitAll()
                .mapNotNull { (sym, quote) -> if (quote != null) sym to quote else null }
                .toMap()
        }
    }
}

