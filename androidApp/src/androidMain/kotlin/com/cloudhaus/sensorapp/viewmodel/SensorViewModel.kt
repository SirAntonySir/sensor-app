package com.cloudhaus.sensorapp.viewmodel

import androidx.lifecycle.ViewModel
import com.cloudhaus.sensorapp.model.ExerciseUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SensorViewModel : ViewModel() {
    private val _units = MutableStateFlow(ExerciseUnit.allUnits)
    val units: StateFlow<List<ExerciseUnit>> = _units

    // unitId -> exerciseName -> count
    private val completions = mutableMapOf<Int, MutableMap<String, Int>>()

    fun completionCount(exerciseName: String, unitId: Int): Int {
        return completions[unitId]?.get(exerciseName) ?: 0
    }

    fun recordCompletion(exerciseName: String, unitId: Int) {
        val unitMap = completions.getOrPut(unitId) { mutableMapOf() }
        unitMap[exerciseName] = (unitMap[exerciseName] ?: 0) + 1
        updateProgression()
    }

    private fun updateProgression() {
        _units.update { units ->
            units.mapIndexed { index, unit ->
                val unitCompletions = completions[unit.id]
                val minCompletions = if (unitCompletions != null && unitCompletions.size >= unit.exercises.size) {
                    unit.exercises.mapNotNull { unitCompletions[it] }.minOrNull() ?: 0
                } else {
                    unit.completions
                }
                val prevUnlocked = if (index > 0) {
                    val prev = units[index - 1]
                    val prevComps = completions[prev.id]
                    val prevMin = if (prevComps != null && prevComps.size >= prev.exercises.size) {
                        prev.exercises.mapNotNull { prevComps[it] }.minOrNull() ?: 0
                    } else 0
                    prevMin >= 5
                } else true

                unit.copy(
                    completions = minOf(minCompletions, 5),
                    isUnlocked = unit.isUnlocked || prevUnlocked,
                )
            }
        }
    }
}
