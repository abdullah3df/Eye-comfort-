package com.example.data

import com.example.network.GeminiClient
import kotlinx.coroutines.flow.Flow

class EyeRepository(private val dao: EyeBreakLogDao) {
    // Flow of all historical eye care breaks sorted by latest first
    val allLogs: Flow<List<EyeBreakLog>> = dao.getAllLogs()

    // Query Gemini (or fallback locally) to generate a personalized physical wellbeing exercise
    suspend fun fetchEyeExercise(langCode: String): EyeExercise {
        return GeminiClient.getEyeExercise(langCode)
    }

    // Instantly retrieve a local medically-backed exercise
    fun fetchLocalExercise(langCode: String): EyeExercise {
        val backup = LocalExercises.getRandomFallback(langCode)
        return EyeExercise(
            exercise_title = backup.title,
            steps = backup.steps,
            duration_seconds = backup.durationSeconds,
            benefit = backup.benefit
        )
    }

    // Insert completed eye break session into local storage
    suspend fun saveCompletedBreak(
        exercise: EyeExercise,
        langCode: String,
        preStrain: String,
        postStrain: String
    ) {
        val log = EyeBreakLog(
            exerciseTitle = exercise.exercise_title,
            steps = exercise.steps,
            benefit = exercise.benefit,
            durationSeconds = exercise.duration_seconds,
            languageCode = langCode,
            preStrainLevel = preStrain,
            postStrainLevel = postStrain
        )
        dao.insertLog(log)
    }

    // Instantly wipe all logs if the user wants to start fresh
    suspend fun clearHistory() {
        dao.clearAllLogs()
    }
}
