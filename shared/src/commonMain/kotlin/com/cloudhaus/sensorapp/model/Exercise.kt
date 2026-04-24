package com.cloudhaus.sensorapp.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val name: String,
    val title: LocalizedText,
    val type: ExerciseType,
    val steps: List<ExerciseStep>,
)

@Serializable
data class LocalizedText(
    val en: String,
    val de: String,
)

@Serializable
enum class ExerciseType {
    CountingBreaths,
    TimedBreaths,
    Tissue,
    Candle,
    Windmill,
    Dandelion,
    Boat,
    Straw,
}

@Serializable
data class ExerciseStep(
    val title: LocalizedText,
    val instructions: List<LocalizedText>,
    val config: StepConfig,
)

@Serializable
data class StepConfig(
    val breathType: String? = null,      // "inhale" or "exhale"
    val breathCount: Int? = null,
    val inhaleDuration: Int? = null,      // seconds
    val exhaleDuration: Int? = null,      // seconds
    val holdDuration: Int? = null,        // seconds
)

@Serializable
data class Unit(
    val id: Int,
    val name: LocalizedText,
    val exercises: List<String>,         // exercise names
)

@Serializable
data class Progression(
    val unitId: Int,
    val noOfCompletions: Int = 0,
    val difficulties: Map<String, Int> = emptyMap(),
)

@Serializable
data class ExerciseResult(
    val exerciseName: String,
    val inhale: Double? = null,
    val exhale: Double? = null,
    val capacity: Double? = null,
    val duration: Double? = null,
    val recordedAt: Long = System.currentTimeMillis(),
)
