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
        prompt: String = "you are a chat assistant and you are an expert at analyzing android app screens.  You are given a screenshot of an android app screen and you need to analyze it and provide 3 helpful insights or suggestions."
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
     * Build HTTP request for Gemini API with JSON response format
     */
    private fun buildRequest(screenshotBase64: String, prompt: String): Request {
        val jsonPrompt = """
            $prompt

            Respond with a valid JSON object in this exact format:
            {
              "insights": [
                "First insight",
                "Second insight",
                "Third insight"
              ]
            }

            Make sure the response is valid JSON and contains exactly 3 insights.
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = jsonPrompt),
                        Part(inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = screenshotBase64
                        ))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7,
                maxOutputTokens = 1000,
                responseMimeType = "application/json"
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
     * Extract insights from AI JSON response
     * Parse the structured JSON response instead of text splitting
     */
    private fun extractInsights(response: GeminiResponse): List<String> {
        val jsonText = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: return listOf("No response from AI")

        try {
            // Parse the JSON response
            val jsonResponse = gson.fromJson(jsonText, JsonResponse::class.java)

            // Extract insights from the structured response
            val insights = jsonResponse.insights ?: emptyList()

            return if (insights.isEmpty()) {
                listOf("No insights generated")
            } else {
                // Take up to 3 insights and ensure they're clean
                insights.take(3).map { it.trim() }.filter { it.isNotEmpty() }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON response: ${e.message}")
            Log.e(TAG, "Raw response: $jsonText")

            // Return error message if JSON parsing fails
            return listOf("Failed to parse AI response", "Please try again", "Check logs for details")
        }
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
        @SerializedName("maxOutputTokens") val maxOutputTokens: Int,
        @SerializedName("responseMimeType") val responseMimeType: String? = null
    )

    /**
     * Data class for parsing the structured JSON response from Gemini
     */
    data class JsonResponse(
        @SerializedName("insights") val insights: List<String>?
    )

    data class GeminiResponse(
        @SerializedName("candidates") val candidates: List<Candidate>?
    )

    data class Candidate(
        @SerializedName("content") val content: Content?
    )
}
