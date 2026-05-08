package com.cloudhaus.sensorapp.model

data class ExerciseUnit(
    val id: Int,
    val name: String,
    val exercises: List<String>,
    val completions: Int = 0,
    val isUnlocked: Boolean = false,
) {
    val medal: String?
        get() = when {
            completions >= 5 -> "\uD83E\uDD47" // gold
            completions == 4 -> "\uD83E\uDD48" // silver
            completions == 3 -> "\uD83E\uDD49" // bronze
            else -> null
        }

    companion object {
        val allUnits = listOf(
            ExerciseUnit(1, "Unit 1", listOf("MIE", "4-0-4", "FloatBall"), isUnlocked = true),
            ExerciseUnit(2, "Unit 2", listOf("4-1-5", "Candle", "Windmill")),
            ExerciseUnit(3, "Unit 3", listOf("4-2-5", "Windmill", "Dandelion")),
            ExerciseUnit(4, "Unit 4", listOf("4-2-5", "Candle")),
            ExerciseUnit(5, "Unit 5", listOf("4-7-11", "Windmill")),
            ExerciseUnit(6, "Unit 6", listOf("4-3-5", "Windmill", "Candle")),
            ExerciseUnit(7, "Unit 7", listOf("4-3-5", "Windmill", "Candle")),
            ExerciseUnit(8, "Unit 8", listOf("4-7-11", "Candle")),
            ExerciseUnit(9, "Unit 9", listOf("4-7-11", "Windmill", "Dandelion")),
        )
    }
}
