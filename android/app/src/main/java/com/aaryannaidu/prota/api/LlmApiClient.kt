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

class LlmApiClient {

    companion object {
        private const val TAG = "LlmApiClient"
        
        // TODO: Replace with your actual Gemini API key
        // Get your free API key from: https://aistudio.google.com/apikey
        private const val API_KEY = "AIzaSyD04A_EGELtGfydagu0ceZjH6xqPjiLHLU"
        
        // Using Gemini 2.5 Flash Lite - fastest and most cost-efficient model
        private const val MODEL = "gemini-2.5-flash-lite"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        
        // Timeout settings
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 30L
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Main method to get chat response suggestions from Gemini API
     * 
     * @param messages The chat messages extracted from WhatsApp
     * @return List of 3 suggested responses, or empty list if API call fails
     */
    fun getSuggestions(messages: String): List<String> {
        if (API_KEY == "YOUR_API_KEY_HERE") {
            Log.e(TAG, "API key not set! Please add your Gemini API key in LlmApiClient.kt")
            return listOf(
                "Error: API key not configured",
                "Please add your Gemini API key",
                "Get one at: aistudio.google.com/apikey"
            )
        }

        if (messages.isEmpty()) {
            Log.w(TAG, "No messages to analyze")
            return listOf(
                "No messages found",
                "Please open a WhatsApp chat",
                "Try again"
            )
        }

        try {
            // Create the prompt for Gemini
            val prompt = buildPrompt(messages)
            
            // Build the request payload
            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.7,
                    topP = 0.9,
                    topK = 40
                )
            )

            val jsonBody = gson.toJson(requestBody)
            Log.d(TAG, "Request payload: $jsonBody")

            // Build the HTTP request
            val url = "$BASE_URL/$MODEL:generateContent"
            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .build()

            // Execute the request
            Log.d(TAG, "Sending request to Gemini API...")
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "API request failed: ${response.code} - ${response.message}")
                Log.e(TAG, "Response body: ${response.body?.string()}")
                return listOf(
                    "API error: ${response.code}",
                    "Failed to get suggestions",
                    "Please try again"
                )
            }

            // Parse the response
            val responseBody = response.body?.string()
            Log.d(TAG, "Response received: $responseBody")

            if (responseBody == null) {
                Log.e(TAG, "Empty response body")
                return listOf("Error: Empty response", "Please try again", "Check your connection")
            }

            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
            
            // Extract suggestions from the response
            val suggestions = parseGeminiResponse(geminiResponse)
            Log.d(TAG, "Parsed ${suggestions.size} suggestions")
            
            return suggestions

        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            return listOf(
                "Network error",
                "Check your internet connection",
                "Try again"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            return listOf(
                "Error: ${e.message ?: "Unknown"}",
                "Failed to process response",
                "Please try again"
            )
        }
    }

    /**
     * Build the prompt for Gemini API
     */
    private fun buildPrompt(messages: String): String {
        return """
You are a helpful assistant that suggests replies to WhatsApp messages.

Here are the recent chat messages:
$messages
Based on these messages, provide exactly 3 short, appropriate reply suggestions. Each reply should be:
- Natural and conversational
- Contextually relevant
- Short (1-2 sentences max)
- Ready to copy and paste

Format your response as numbered suggestions (1., 2., 3.) with each suggestion on a new line.

Example format:
1. First suggestion here
2. Second suggestion here
3. Third suggestion here

Now provide 3 suggestions:
        """.trimIndent()
    }

    /**
     * Parse the Gemini API response and extract suggestions
     */
    private fun parseGeminiResponse(response: GeminiResponse): List<String> {
        try {
            // Get the text content from the first candidate
            val text = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()
                ?.text
                ?: return listOf("No response generated", "Try different messages", "Please retry")

            // Parse the numbered suggestions
            val suggestions = mutableListOf<String>()
            val lines = text.split("\n")
            
            for (line in lines) {
                val trimmed = line.trim()
                // Look for numbered suggestions (1. 2. 3. or 1) 2) 3))
                if (trimmed.matches(Regex("^[123][.)].*"))) {
                    // Remove the number prefix and clean up
                    val suggestion = trimmed.substring(2).trim()
                    if (suggestion.isNotEmpty()) {
                        suggestions.add(suggestion)
                    }
                }
            }

            // Ensure we have exactly 3 suggestions
            return when {
                suggestions.size >= 3 -> suggestions.take(3)
                suggestions.isEmpty() -> {
                    // Fallback: use the whole text split by newlines
                    text.split("\n")
                        .filter { it.trim().isNotEmpty() }
                        .take(3)
                        .ifEmpty { 
                            listOf("Thanks!", "Sounds good", "Let's do it") 
                        }
                }
                else -> {
                    // Pad with generic suggestions if we got fewer than 3
                    val padded = suggestions.toMutableList()
                    while (padded.size < 3) {
                        padded.add(when (padded.size) {
                            1 -> "Thanks for sharing!"
                            2 -> "Let me get back to you"
                            else -> "Sounds good!"
                        })
                    }
                    padded
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            return listOf("Error parsing response", "Please try again", "Retry")
        }
    }

    // ========== Data Classes for Gemini API ==========

    /**
     * Request structure for Gemini API
     * Based on: https://ai.google.dev/gemini-api/docs#java
     */
    data class GeminiRequest(
        @SerializedName("contents")
        val contents: List<Content>,
        
        @SerializedName("generationConfig")
        val generationConfig: GenerationConfig? = null
    )

    data class Content(
        @SerializedName("parts")
        val parts: List<Part>
    )

    data class Part(
        @SerializedName("text")
        val text: String
    )

    data class GenerationConfig(
        @SerializedName("temperature")
        val temperature: Double = 0.7,
        
        @SerializedName("maxOutputTokens")
        val maxOutputTokens: Int = 200,
        
        @SerializedName("topP")
        val topP: Double = 0.9,
        
        @SerializedName("topK")
        val topK: Int = 40
    )

    /**
     * Response structure from Gemini API
     */
    data class GeminiResponse(
        @SerializedName("candidates")
        val candidates: List<Candidate>?
    )

    data class Candidate(
        @SerializedName("content")
        val content: Content?
    )
}

