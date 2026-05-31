package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.EyeExercise
import com.example.data.LocalExercises
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Query Gemini API for structured exercise and process result
    suspend fun getEyeExercise(langCode: String): EyeExercise = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is unconfigured. Utilizing offline highly-optimized preset exercise.")
            return@withContext getLocalBackup(langCode)
        }

        val systemPrompt = """
            You are an expert eye health assistant integrated into a digital wellbeing application.
            Your sole purpose is to provide short, effective, and medically sound eye exercises to relieve digital eye strain.

            MEDICAL KNOWLEDGE BASE (Strictly use these rules):
            1. 20-20-20 Rule: Look at an object 20 feet (6 meters) away for 20 seconds. Relaxes the ciliary muscle.
            2. Intentional Blinking: Squeeze eyes gently and open to restore tear film. (People blink 66% less at screens).
            3. Palming: Rub hands to warm them, place gently over closed eyes for 30-60 seconds. Darkness and warmth relax the optic nerve.
            4. Screen Ergonomics: Hold the phone 30-40 cm away and keep the screen slightly below eye level (10-15 cm) to reduce tear evaporation.
        """.trimIndent()

        val userPrompt = """
            INPUT:
            The app sends the target language code (e.g., "ar", "en", "fr", "de", "es").
            Target language code: "$langCode"

            OUTPUT REQUIREMENTS:
            - Respond ONLY with a valid JSON object. Absolutely NO markdown or conversational text outside the JSON block.
            - Translate the content accurately into the requested language.
            - Randomly select ONE tip/exercise from the MEDICAL KNOWLEDGE BASE above for the response.

            JSON SCHEMA:
            {
              "exercise_title": "Short name",
              "steps": "Clear actionable steps based on the selected knowledge base rule.",
              "duration_seconds": 20,
              "benefit": "Medical benefit explained simply."
            }
        """.trimIndent()

        try {
            // Construct the REST API request JSON payload for Gemini manually for stability and light-footprint
            val requestPayload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.8) // Rotate randomly & creatively
                })
            }

            val requestBody = requestPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val requestUrl = "$BASE_URL?key=$apiKey"

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Unsuccessful response from server: ${response.code} ${response.message}")
                return@withContext getLocalBackup(langCode)
            }

            val responseBody = response.body?.string() ?: ""
            if (responseBody.isEmpty()) {
                Log.e(TAG, "Empty response body received from Gemini")
                return@withContext getLocalBackup(langCode)
            }

            // Parse response json structure to find the generated text
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            var rawText = firstPart?.optString("text")?.trim() ?: ""

            if (rawText.isEmpty()) {
                Log.e(TAG, "No generated text found in candidate parts")
                return@withContext getLocalBackup(langCode)
            }

            // Clean markdown tags if the model didn't perfectly obey rule 2
            rawText = cleanMarkdownJson(rawText)

            // Parse into EyeExercise using Moshi
            val exerciseAdapter = moshi.adapter(EyeExercise::class.java)
            val parsedExercise = exerciseAdapter.fromJson(rawText)
            
            if (parsedExercise == null || parsedExercise.exercise_title.isBlank() || parsedExercise.steps.isBlank()) {
                Log.e(TAG, "Moshi parsing generated null or incomplete fields")
                return@withContext getLocalBackup(langCode)
            }

            Log.d(TAG, "Successfully generated and parsed model exercise: ${parsedExercise.exercise_title} in $langCode")
            return@withContext parsedExercise

        } catch (e: Exception) {
            Log.e(TAG, "Exception encountered during generation, falling back to offline preset", e)
            return@withContext getLocalBackup(langCode)
        }
    }

    // Helper to clean off standard markdown json indicators if present
    private fun cleanMarkdownJson(input: String): String {
        var clean = input.trim()
        if (clean.startsWith("```json")) {
            clean = clean.substring("```json".length)
        } else if (clean.startsWith("```")) {
            clean = clean.substring("```".length)
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length - "```".length)
        }
        return clean.trim()
    }

    // Fallback getter to local medically sound rotations
    fun getLocalBackup(langCode: String): EyeExercise {
        val backup = LocalExercises.getRandomFallback(langCode)
        return EyeExercise(
            exercise_title = backup.title,
            steps = backup.steps,
            duration_seconds = backup.durationSeconds,
            benefit = backup.benefit
        )
    }
}
