package com.example.data

import com.example.model.GeminiAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun validateApiKey(apiKey: String, model: String = "gemini-2.5-flash"): Result<Pair<Long, String>> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty."))
        }

        val startTime = System.currentTimeMillis()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmedKey"

        try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Respond with a single word: READY")
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val body = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Result.success(Pair(latency, "Gemini $model connected successfully (${latency}ms)"))
            } else {
                val errorMsg = try {
                    val json = JSONObject(body)
                    val errorObj = json.optJSONObject("error")
                    errorObj?.optString("message") ?: "HTTP ${response.code}: $body"
                } catch (e: Exception) {
                    "HTTP ${response.code}: $body"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Failed to connect to Google Gemini API"))
        }
    }

    suspend fun analyzeMarketIntelligence(
        apiKey: String,
        symbol: String,
        price: Double,
        rsi: Double,
        emaCross: String,
        volume24h: Double,
        model: String = "gemini-2.5-flash"
    ): Result<GeminiAnalysisResult> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API Key missing."))
        }

        val prompt = """
            You are a professional quantitative crypto derivatives risk and market analyst.
            Analyze the following live market metrics for $symbol on Delta Exchange:
            - Symbol: $symbol
            - Current Price: $$price
            - Relative Strength Index (RSI 14): ${String.format(Locale.US, "%.1f", rsi)}
            - Exponential Moving Average Signal: $emaCross
            - 24h Trading Volume: $$volume24h
            
            Provide a strictly valid JSON response with no markdown fences, formatted as:
            {
              "sentiment": "BULLISH" | "BEARISH" | "NEUTRAL",
              "confidence": 85,
              "summary": "Brief 1-sentence synopsis",
              "technicalRationale": "2-sentence technical rationale referencing RSI, EMA, and market momentum",
              "riskWarning": "Key volatility/liquidation hazard to watch",
              "suggestedAction": "BUY" | "SELL" | "HOLD" | "REDUCE_RISK"
            }
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$trimmedKey"

        try {
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API Error (HTTP ${response.code}): $body"))
            }

            val rootJson = JSONObject(body)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Clean up any possible markdown formatting
            val cleaned = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val parsed = JSONObject(cleaned)
            val nowIso = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

            val result = GeminiAnalysisResult(
                timestamp = nowIso,
                symbol = symbol,
                price = price,
                sentiment = parsed.optString("sentiment", if (rsi > 55) "BULLISH" else "BEARISH"),
                confidence = parsed.optInt("confidence", 78),
                summary = parsed.optString("summary", "Market momentum displaying active trend continuation."),
                technicalRationale = parsed.optString("technicalRationale", "RSI at ${String.format(Locale.US, "%.1f", rsi)} with $emaCross structure."),
                riskWarning = parsed.optString("riskWarning", "Watch order book depth and maintain max notional limit."),
                suggestedAction = parsed.optString("suggestedAction", if (rsi > 55) "BUY" else "WAIT")
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("Gemini Analysis parse error: ${e.message}"))
        }
    }
}
