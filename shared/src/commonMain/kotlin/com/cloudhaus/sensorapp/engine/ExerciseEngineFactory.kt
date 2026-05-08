package com.cloudhaus.sensorapp.engine

import com.cloudhaus.sensorapp.model.ExerciseType
import com.cloudhaus.sensorapp.model.StepConfig
import com.cloudhaus.sensorapp.pipeline.BreathDetector
import kotlinx.coroutines.CoroutineScope

object ExerciseEngineFactory {

    fun create(
        type: ExerciseType,
        breathDetector: BreathDetector,
        config: StepConfig = StepConfig(),
        difficulty: Int = 0,
        scope: CoroutineScope,
    ): ExerciseEngine = when (type) {
        ExerciseType.Candle -> CandleEngine(breathDetector, difficulty, scope)
        ExerciseType.Windmill -> WindmillEngine(breathDetector, difficulty, scope)
        ExerciseType.Dandelion -> DandelionEngine(breathDetector, difficulty, scope)
        ExerciseType.FloatBall -> FloatBallEngine(breathDetector, difficulty, scope)
        ExerciseType.CountingBreaths -> CountingBreathsEngine(
            breathDetector, config.breathCount ?: 5, scope
        )
        ExerciseType.TimedBreaths -> TimedBreathsEngine(config, config.breathCount ?: 5, scope)
    }
}
