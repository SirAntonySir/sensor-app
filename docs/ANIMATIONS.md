# Animation Guide

How breath-driven exercise animations are structured, and how to add new ones.

## Overview

Each exercise has its own dedicated `@Composable` under `shared/src/commonMain/kotlin/com/cloudhaus/sensorapp/ui/animation/<name>/`. The composable owns its own frame loop, particle/state structures, and procedural `Canvas` drawing. There is no shared rig engine or draw-list indirection — each exercise stands on its own. The cost is some boilerplate per exercise; the benefit is each animation can be styled independently and you never fight an engine abstraction.

## The data type: `AnimationState`

`shared/.../engine/AnimationState.kt` is the canonical data type the UI consumes. It's a sealed class with one subclass per exercise:

```kotlin
sealed class AnimationState {
    data class Candle(val isBlownOut: Boolean, val flameScale: Float, val colorZone: ColorZone, val pressure: Float) : AnimationState()
    data class Windmill(val rotationSpeed: RotationSpeed, val colorZone: ColorZone, val pressure: Float) : AnimationState()
    data class Dandelion(val stage: DandelionStage, val pressure: Float) : AnimationState()
    data class CountingBreaths(val currentCount: Int, val totalCount: Int, val breathPhase: BreathPhase) : AnimationState()
    data class TimedBreaths(val phase: TimedBreathPhase, val remainingSeconds: Int, val currentRepetition: Int, val totalRepetitions: Int) : AnimationState()
}
```

Each subclass typically carries:
- **A continuous numeric value** (`pressure`, `progress`, …) — the animation reads this every frame and maps it onto whatever moves continuously (sway, depth, brightness, particle intensity).
- **A discrete stage / flag** (`stage`, `isBlownOut`, `frame`, `breathPhase`) — the animation reacts to **changes** in this, typically with a `LaunchedEffect` that bumps a one-shot trigger.

Adding a new exercise means adding a new sealed subclass with whatever shape it needs.

## Data flow

```
MockSensorSource (or barometer / BLE)
   ↓  PressureReading (hPa, ms)            ~50 Hz
BreathDetector
   ↓  BreathEvent (Inhale/Exhale + velocity)
ExerciseEngine  (one per ExerciseType)
   ↓  ExerciseState  =  Idle | Active(AnimationState) | Complete(ResultData)
ExerciseAnimationRouter        special-cases CountingBreaths / TimedBreaths (timer UIs)
   ↓  AnimationState
BreathRigView                  when-dispatch on AnimationState subclass
   ↓
@Composable <Exercise>(breath, endTrigger, modifier, …)   — draws to Canvas
```

A virtual-input path also exists: `engine.onVirtualBlow()` (discrete) and `engine.onVirtualIntensity(Float)` (continuous slider). They write directly into the same `_state` flow, so the animation pipeline downstream is identical.

## The composable contract

By convention every exercise animation looks like this:

```kotlin
@Composable
fun Dandelion(
    breath: Float,            // 0..100, drives all continuous behaviour
    endTrigger: Int,          // bump on rising edge of "end" event
    modifier: Modifier = Modifier,
    palette: DandelionPalette = DandelionPalette.Default,
    // optional design knobs with sensible defaults
)
```

Rules:
- **`breath`** is normalized 0..100. The engine clamps to this range. Map non-linearly (`pow`, `sqrt`) when you need different feel at low vs. high intensity.
- **`endTrigger`** is the *only* event channel. Bumping it = "the discrete thing happened" (flame blown out, seeds released, tissue launched). Receive it with `LaunchedEffect(endTrigger) { if (endTrigger > 0) { … } }`. Bumping it again re-fires.
- **`modifier`** must be forwarded to the root `Canvas` so the parent layout can size you.
- Anything else is a tunable. Group related knobs into a data class (`DandelionPalette`, `DandelionConfig`, …) rather than a long parameter list.

## Wiring it up in `BreathRigView`

`shared/.../ui/animation/BreathRig.kt` is the dispatcher. Add one branch:

```kotlin
is AnimationState.Candle -> {
    var endTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.isBlownOut) {
        if (state.isBlownOut) endTrigger++
    }
    Candle(
        breath = state.pressure,
        endTrigger = endTrigger,
        modifier = modifier,
    )
}
```

The trick: key the `LaunchedEffect` on the *discrete* field (here `state.isBlownOut`). The body only re-runs when that field actually changes, so `endTrigger` only increments on the rising edge — never on every recomposition.

## Adding a new animation, step by step

1. **Add or extend the `AnimationState` subclass** in `engine/AnimationState.kt` if its data shape is new.
2. **Create the composable file**: `shared/.../ui/animation/<name>/<Name>.kt`.
3. **Implement the composable** with the standard skeleton (see below).
4. **Add a branch in `BreathRigView`** mapping the `AnimationState` subclass to your composable.
5. **Make the engine emit it.** The matching engine in `shared/.../engine/<Name>Engine.kt` writes into `_state.value = ExerciseState.Active(AnimationState.<Name>(…))`. For dev/manual testing, also implement `onVirtualIntensity` and/or `onVirtualBlow`.

That's it — no engine config or draw-list registration.

## The standard skeleton

```kotlin
@Composable
fun MyAnim(
    breath: Float,
    endTrigger: Int,
    modifier: Modifier = Modifier,
    config: MyAnimConfig = MyAnimConfig.Default,
) {
    // 1. Persistent particle / scene state. Plain Kotlin classes — NOT Compose state.
    val particles = remember { buildParticles(config) }

    // 2. Compose-state read by Canvas to drive redraws.
    val frameTime = remember { mutableFloatStateOf(0f) }
    val dt = remember { mutableFloatStateOf(0f) }

    // 3. Frame clock. withFrameNanos drives the animation; cap dt so a paused
    //    app doesn't snap forward 30 seconds on resume.
    LaunchedEffect(Unit) {
        var start = 0L; var prev = -1f
        while (true) withFrameNanos { now ->
            if (start == 0L) start = now
            val t = (now - start) / 1e9f
            dt.floatValue = if (prev < 0f) 0f else (t - prev).coerceAtMost(0.05f)
            prev = t
            frameTime.floatValue = t
        }
    }

    // 4. End trigger — one-shot reactions only.
    LaunchedEffect(endTrigger) {
        if (endTrigger > 0) {
            // Stagger using coroutineScope { launch { delay(i*8L); ... } }.
            // Sequential delay() accumulates and freezes the animation — see
            // the lesson learned in the Dandelion port.
        }
    }

    // 5. Draw. Read frameTime/dt to subscribe Canvas to ticks. Update particle
    //    state in plain `var` fields (no Compose state writes from inside draw).
    Canvas(modifier = modifier) {
        val t = frameTime.floatValue
        val deltaT = dt.floatValue
        particles.forEach { it.update(deltaT, t, breath) }
        // ... drawPath / drawCircle / withTransform { ... } ...
    }
}
```

## Patterns worth reusing

**Particle state lives in plain classes**, not Compose state:
```kotlin
private class Seed(val homeAngle: Float, val homeRadius: Float) {
    var x = 0f; var y = 0f
    var vx = 0f; var vy = 0f
    var attached = true
    fun update(dt: Float, t: Float, breath: Float) { … }
}
```
Mutating a `var` doesn't trigger recomposition. The Canvas redraws because `frameTime.floatValue` changed; while it's drawing it walks the (mutated) particles. This avoids per-frame recomposition pressure.

**Procedural mapping from breath:**
```kotlin
val breathN = breath / 100f
val sway = sin(phase) * (1.5f + breathN * 4f)        // amplitude grows with breath
val foreshorten = 1f + breathN * 0.04f               // tiny perspective tilt
val tremble = breathN.pow(1.4f) * jitterAmp          // non-linear: subtle low, intense high
```
Use `pow` to bias — `pow(1.4)` gives "barely there at low breath, suddenly intense at high".

**Concurrent staggered events** (the JSX `setTimeout(release, i*8)` pattern in Kotlin):
```kotlin
coroutineScope {
    particles.forEachIndexed { i, p ->
        launch { delay(i * 8L); p.release() }
    }
}
```
Each `launch` waits its own absolute delay. **Don't** use sequential `delay(i * 8L)` in a loop — it accumulates.

**End-trigger from discrete state changes**: see the `LaunchedEffect(state.X)` pattern in the BreathRigView wiring above. Always key the effect on the discrete field, never on `state` (which changes every frame because of `pressure`).

## Tunables: palettes and configs

Wrap design knobs in data classes with `Default` companions so call sites can override only what they need. The convention is two classes per rig:

- **`<Name>Palette`** — colors only. One field per semantic role (`stem`, `leafDark`, `seedFluff`, …). All fields default; callers can copy and tweak with `palette.copy(stem = …)`.
- **`<Name>Config`** — counts, sizes, thresholds. Use nested data classes for repeated structures (rings, leaves, particles).

Example from `Dandelion.kt`:

```kotlin
data class DandelionConfig(
    val rings: List<Ring> = DefaultRings,            // 6 rings, 99 seeds total
    val leaves: List<Leaf> = DefaultLeaves,          // 3 leaves at the stem base
    val stemWidth: Float = 6f,
    val stemLengthFraction: Float = 0.55f,
    val swayAmplitudeBase: Float = 1.5f,
    val swayAmplitudePerBreath: Float = 4f,
    val leafTeeth: Int = 5,
    val leafToothDepth: Float = 14f,
    val haloRadius: Float = 90f,
    val seedFluffRadius: Float = 11f,
    val seedSpokes: Int = 11,
    val detachThresholdMin: Float = 78f,
    val detachThresholdJitter: Float = 20f,
) {
    data class Ring(val count: Int, val radius: Float, val phase: Float = 0f)
    data class Leaf(val offsetX: Float, val offsetY: Float, val baseAngleDeg: Float, val swayCoefficient: Float, val length: Float)
    companion object { val Default = DandelionConfig(); /* + DefaultRings, DefaultLeaves */ }
}
```

A caller (e.g. `BreathRigView`) overrides only what's interesting:

```kotlin
Dandelion(
    breath = state.pressure,
    endTrigger = endTrigger,
    modifier = modifier,
    config = DandelionConfig(
        rings = listOf(
            DandelionConfig.Ring(count = 1, radius = 0f),
            DandelionConfig.Ring(count = 12, radius = 30f),
        ),
        stemWidth = 8f,
    ),
)
```

Two implementation notes:
- **Key `remember` on the config**: `val particles = remember(config) { build(config) }`. A new config rebuilds the particle list; otherwise particles persist across recomposition.
- **Only lift to config what a real caller wants to change.** Per-particle physics constants and draw-helper hardcodes can stay in the rig file. Speculative knobs are dead weight.

## Constraints

- **No JVM-only APIs.** `Math.toDegrees`, `java.util.Random`, `Float.NaN`-checks via `Float.isNaN()` from `java.lang.Float`, etc. break the iOS build. Use `kotlin.math.*` and `kotlin.random.Random`.
- **No Compose-state writes from inside `Canvas { … }`.** That block runs in the draw phase. Plain class field mutations are fine. Move `MutableState` writes into the `withFrameNanos` block.
- **Cap `dt`.** Without it, an app that was backgrounded for 30 s will jump the simulation forward 30 s of physics on the next frame.
- **No determinism by default.** `kotlin.random.Random` is non-reproducible. If you ever want screenshot tests, hoist a seeded `Random` parameter on the composable.
- **Keep the rig file self-contained.** Don't reach into other rigs' classes. If two animations need the same helper (e.g. a noise function), put it in `shared/.../ui/animation/_common/`.

## Reference implementations

- `shared/.../ui/animation/dandelion/Dandelion.kt` — particle system (99 seeds in 6 rings, all configurable via `DandelionConfig`), procedural stem with Bezier curves, end-trigger driving a staggered seed release, color theming via `DandelionPalette`. Read this end-to-end before writing a new animation; it covers every pattern in this guide.

For the engine side of the contract, see `engine/DandelionEngine.kt`, `engine/CandleEngine.kt`, and `engine/WindmillEngine.kt` — all three implement `onVirtualIntensity` (slider input) and `onVirtualBlow` (discrete button input).
