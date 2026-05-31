package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EyeExercise(
    val exercise_title: String,
    val steps: String,
    val duration_seconds: Int,
    val benefit: String
)
