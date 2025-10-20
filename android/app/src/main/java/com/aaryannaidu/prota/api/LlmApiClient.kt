package com.aaryannaidu.prota.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Simple client for Gemini AI API
 * Sends screenshots and gets AI insights back
 */
class LlmApiClient {

    companion object {
        private const val TAG = "LlmApiClient"
        private const val API_KEY = "AIzaSyD04A_EGELtGfydagu0ceZjH6xqPjiLHLU"
        private const val MODEL = "gemini-2.5-flash-lite"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Analyze screenshot with AI
     * @return List of insights (usually 3 items)
     */
    fun analyzeScreenshot(
        screenshotBase64: String,
        prompt: String = "Analyze this screen and provide 3 helpful insights or suggestions."
    ): List<String> {
        
        if (screenshotBase64.isEmpty()) {
            return listOf("Screenshot is empty", "Please try again")
        }

        try {
            // Build request with image + prompt
            val request = buildRequest(screenshotBase64, prompt)
            
            // Call API
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "API error: ${response.code}")
                return listOf("API error", "Please try again")
            }

            // Parse response
            val responseBody = response.body?.string() ?: return listOf("Empty response")
            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
            
            return extractInsights(geminiResponse)

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            return listOf("Network error", "Check connection")
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            return listOf("Error: ${e.message}", "Try again")
        }
    }

    /**
     * Build HTTP request for Gemini API
     */
    private fun buildRequest(screenshotBase64: String, prompt: String): Request {
        val requestBody = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = screenshotBase64
                        ))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7,
                maxOutputTokens = 500
            )
        )

        val jsonBody = gson.toJson(requestBody)
        val url = "$BASE_URL/$MODEL:generateContent"

        return Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", API_KEY)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
    }

    /**
     * Extract insights from AI response
     * Simple: just split by lines and take first 3 non-empty ones
     */
    private fun extractInsights(response: GeminiResponse): List<String> {
        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: return listOf("No response from AI")

        // Split by lines, clean up, take first 3
        val insights = text.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { cleanLine(it) } // Remove "1.", "•", etc.
            .take(3)

        return if (insights.isEmpty()) {
            listOf("No insights generated")
        } else {
            insights
        }
    }

    /**
     * Remove numbering/bullets from lines (1., •, -, etc.)
     */
    private fun cleanLine(line: String): String {
        return line
            .replaceFirst(Regex("^[0-9]+[.)\\s]+"), "") // Remove "1. " or "1) "
            .replaceFirst(Regex("^[•\\-*]\\s+"), "")    // Remove "• " or "- "
            .trim()
    }

    // ========== Data Classes ==========

    data class GeminiRequest(
        @SerializedName("contents") val contents: List<Content>,
        @SerializedName("generationConfig") val generationConfig: GenerationConfig
    )

    data class Content(
        @SerializedName("parts") val parts: List<Part>
    )

    data class Part(
        @SerializedName("text") val text: String? = null,
        @SerializedName("inlineData") val inlineData: InlineData? = null
    )

    data class InlineData(
        @SerializedName("mimeType") val mimeType: String,
        @SerializedName("data") val data: String
    )

    data class GenerationConfig(
        @SerializedName("temperature") val temperature: Double,
        @SerializedName("maxOutputTokens") val maxOutputTokens: Int
    )

    data class GeminiResponse(
        @SerializedName("candidates") val candidates: List<Candidate>?
    )

    data class Candidate(
        @SerializedName("content") val content: Content?
    )
}
