clau# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin Multiplatform breathing-exercise app with two consumer apps (Android, iOS) sharing engine, pipeline, and animation code from `:shared`. The barometer (or BLE pressure sensor) drives per-exercise state machines; visual feedback is rendered entirely in Compose Multiplatform — each exercise has its own dedicated `@Composable` that consumes a `breath: Float` (0..100) plus a one-shot end trigger and draws procedurally to a `Canvas`.

## Build & Run

Gradle wrapper builds Android and the shared Kotlin/Native framework. iOS is generated from `iosApp/project.yml` via XcodeGen; the Xcode build then triggers Gradle to (re)build the framework via a pre-build script.

```bash
# Android
./gradlew :androidApp:installDebug                  # build + install to connected device/emulator
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64  # build the iOS framework manually

# Tests
./gradlew :shared:allTests                          # run multiplatform tests (commonTest)
./gradlew :shared:testDebugUnitTest --tests com.cloudhaus.sensorapp.pipeline.BreathDetectorTest  # single class

# iOS — regenerate the Xcode project after touching project.yml
cd iosApp && xcodegen generate                      # then open SensorApp.xcodeproj
# Building the iOS target in Xcode runs the Gradle linkDebugFrameworkIosSimulatorArm64 task
# automatically (preBuildScripts) and copies Compose resources into the .app (postBuildScripts).

# Lint / format — not yet wired (ktlint/detekt/SwiftLint planned per docs/IMPLEMENTATION_PLAN.md)
```

JDK 21 is required (`jvmTarget = "21"`, `JavaVersion.VERSION_21`). Android `compileSdk = 36`, `minSdk = 26`. iOS deployment target 17.0, Swift 6.0, Xcode 26.2.

## Architecture

### Module layout

`settings.gradle.kts` only includes `:shared` and `:androidApp`. The `shared/sensor-api/`, `shared/pipeline/`, and `shared/animation-driver/` directories are scaffolding from `docs/IMPLEMENTATION_PLAN.md` Phase 3 but are **not** wired as Gradle subprojects — all shared code currently lives directly in `shared/src/commonMain/kotlin/com/cloudhaus/sensorapp/`. Treat them as empty placeholders unless explicitly splitting modules.

Active source roots:
- `shared/src/commonMain` — engines, sensor abstraction, pipeline, models, shared Compose UI
- `shared/src/{androidMain,iosMain}` — `expect`/`actual` (e.g. `Time.kt`) and iOS Compose entry points
- `androidApp/src/androidMain` — `MainActivity`, NavGraph, screens, `SensorViewModel`
- `iosApp/iosApp` — SwiftUI views, Swift `SensorViewModel`, `ComposeAnimationView` SwiftUI bridge
- `shared/src/commonMain/kotlin/com/cloudhaus/sensorapp/ui/animation/<exercise>/` — per-exercise Compose animation, one folder per exercise

### Exercise engine pipeline

This is the central abstraction; touching engines means understanding all of it.

`SensorSource` (`shared/.../sensor/SensorSource.kt`) emits `PressureReading(pressure: Double /* hPa */, timestampMs: Long)` at ~50 Hz. `MockSensorSource` is the only common implementation — real platform sensors are still TODO.

`BreathDetector` (`shared/.../pipeline/BreathDetector.kt`) consumes pressure readings and produces breath events used by engines.

`ExerciseEngineFactory.create()` (`shared/.../engine/ExerciseEngineFactory.kt`) dispatches `ExerciseType` → one of 6 engines (`Candle`, `Windmill`, `Dandelion`, `FloatBall`, `CountingBreaths`, `TimedBreaths`). Each engine exposes `state: StateFlow<ExerciseState>` (`Idle | Active(AnimationState) | Complete(ExerciseResultData)`).

Both platforms collect that `StateFlow` and route it to a single shared Compose renderer:
- **Android** calls `ExerciseAnimationRouter` directly from `ExerciseScreen.kt`.
- **iOS** embeds the same router via `ExerciseAnimationViewController(...)` in `shared/src/iosMain/.../ComposeViewControllers.kt` — wrapped on the SwiftUI side as `ComposeAnimationView` (a `UIViewControllerRepresentable`). There is no SwiftUI-specific animation code anymore.

`ExerciseAnimationRouter` (`shared/.../ui/animation/`) special-cases `CountingBreaths` and `TimedBreaths` (which are timer-based, no rig needed) and delegates everything else to `BreathRigView`, which `when`-dispatches on the `AnimationState` subclass to the matching exercise composable.

### Exercise animations

Each exercise has a self-contained `@Composable` under `shared/.../ui/animation/<name>/`. The composable owns its own particle state, frame loop (`withFrameNanos`), and `Canvas` drawing. There is no shared rig engine or `DrawList` indirection — by convention every animation accepts:

- `breath: Float` (0..100) — drives sway, depth, particle behaviour.
- `endTrigger: Int` — bump to fire a one-shot release/end-of-breath burst (Compose `LaunchedEffect(endTrigger)`). `BreathRigView` increments this on the rising edge into `DandelionStage.FullBlown` (or the equivalent stage for other exercises).
- `modifier: Modifier`.

Currently shipped: `Dandelion`, `Candle`, `Pinwheel` (drives the Windmill exercise), `FloatBall`. Anything else falls through to a "not yet implemented" placeholder in `BreathRigView`.

**Adding an exercise animation:** create `ui/animation/<name>/<Name>.kt` exposing `@Composable fun <Name>(breath: Float, endTrigger: Int, modifier: Modifier = Modifier, ...)`, then add one branch in `BreathRigView.kt` mapping the matching `AnimationState` subclass to it. Map any stage-based end-of-breath signal into an `endTrigger` counter via `LaunchedEffect(state.stage)`. Full step-by-step + patterns to reuse: `docs/ANIMATIONS.md`.

**Avoid JVM-only APIs.** `Math.toDegrees` etc. won't compile on iOS — use `kotlin.math` only.

**No determinism by default.** Animations use `kotlin.random.Random` directly. If you need reproducibility for screenshot tests, plumb a seeded `Random` through the composable.

### Compose resources on iOS

`shared/build.gradle.kts` sets `packageOfResClass = "com.cloudhaus.sensorapp.resources"`. The Xcode `postBuildScripts` step in `iosApp/project.yml` copies `shared/build/generated/compose/resourceGenerator/preparedResources/commonMain/composeResources` into the app bundle at `compose-resources/composeResources/com.cloudhaus.sensorapp.resources/` — this path is what Compose Multiplatform's resource loader expects at runtime on iOS. Don't rename either side without changing the other.

### Persistence

Supabase (`postgrest-kt`, `auth-kt`) is in shared `commonMain` deps but no client is wired up yet — both `SensorViewModel`s currently hold completion state in memory only. Schema and auth strategy are sketched in `docs/IMPLEMENTATION_PLAN.md` Phase 5.

## Versions & toolchain

`gradle/libs.versions.toml` is the single source of truth. Current pins: Kotlin 2.1.20, AGP 8.9.3, Compose Multiplatform 1.7.3, Coroutines 1.10.1, Ktor 3.1.1, Supabase 3.1.1, Koin 4.0.4. The version targets in `docs/IMPLEMENTATION_PLAN.md` (Kotlin 2.3.20, AGP 9.0.0) are aspirational — the catalog is what actually builds.
