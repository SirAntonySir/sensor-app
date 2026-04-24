# Reference: Breathing Exercise App (React Native)

This KMP sensor app draws inspiration from an existing React Native breathing exercise app.
The original source code is available for reference when implementing exercise logic, animations, and sensor interactions.

## Source

- **Local path:** `/Users/anton/Downloads/breathingapp-main`
- **GitHub:** https://github.com/SirAntonySir/breathingapp (private)
- **Stack:** React Native 0.72 / Expo 49 / TypeScript

## App Concept

A guided breathing rehabilitation app ("Mrs. Brenda") with 9 progressive units.
Users perform breathing exercises using the phone's barometer sensor to detect actual blowing.
Progress is tracked with a medal system (bronze/silver/gold).

## Architecture Overview

```
App.tsx                         Entry point, providers
src/navigation/AppRouting.tsx   Drawer navigator, onboarding vs dashboard routing
src/components/
  screens/
    Dashboard/                  Timeline of 9 units with medal progress
    UnitOverview/               3 exercises per unit, completion pills
    Exercise/                   Core exercise engine (see below)
    Onboarding/                 Welcome, consent, survey, personal info form
    Settings/                   Edit user profile
    Statistics/                 Bar charts of exercise history
    Certification/              Completion certificate
  common/
    VirtualBlowingButton/       Barometer-based blow detection with button fallback
    CustomSlider/               Swipeable slide container for exercise flow
```

## Exercise Engine

The exercise flow is orchestrated by `Exercise.tsx`:

```
KeepStill -> Calibration -> (Instruction -> Countdown -> Exercise) x N -> EndScreen
```

Exercise definitions are fetched from the database. Each exercise has:
- `title` (localized, en/de)
- `name` (identifier: MIE, Candle, Windmill, etc.)
- `exercise` (numbered steps with type + config)
- `instructions.step` (localized instruction slides)

## Exercise Types

| Type | Description | Sensor Usage | Key File |
|------|-------------|-------------|----------|
| `countingBreaths` | Count inhale/exhale cycles | Barometer detects breath direction | `CountingBreaths.tsx` |
| `timedBreaths` | Timed patterns (4-0-4, 4-7-11, etc.) | Timer + barometer validation | `TimedBreaths.tsx` |
| `tissue` | Blow tissue animation | Barometer pressure delta | `Tissue.tsx` |
| `candle` | Blow out candle flame | Barometer spike detection | `Candle.tsx` |
| `windmill` | Spin windmill blades | Continuous barometer reading | `Windmill.tsx` |
| `dandelion` | Blow dandelion seeds | Barometer capacity measurement | `Dandelion.tsx` |
| `boat` | Propel sailboat | Sustained barometer pressure | `Sailboat.tsx` |
| `straw` | Shoot paper ball | Barometer impulse detection | `Straw.tsx` |

## Barometer Service

`barometerService.tsx` is the core sensor layer. Key functions to port:

- `readCurrentPressure()` -- current barometer reading
- `pressureGenerator()` -- generator yielding pressure samples at fixed intervals
- `listenForCalibration(breathType)` -- detect inhale/exhale peak for calibration
- `listenForBreaths(breathType)` -- detect breath and compute trapezoidal integral of velocity
- `listenForFullBreath(duration)` -- capture both inhale and exhale over a duration
- `listenForCandleBlow()` -- detect single strong exhale
- `listenForDandelionBlow()` -- detect sustained blow over 3 seconds

**Thresholds (from constants.tsx):**
- `MINIMUM_BAROMETER_TOLERANCE = 0.10` hPa
- `SMALL_BAROMETER_TOLERANCE = 0.10` hPa
- `MEDIUM_BAROMETER_TOLERANCE = 0.5` hPa
- `BAROMETER_READING_INTERVAL = 20` ms (50 Hz)

**Velocity calculation:**
```
velocity = sqrt(2 * pressureDelta * 100)
```

## Unit/Exercise Definitions

Defined in `assets/exercises-info/exercises-info.ts`:

| Unit | Exercises |
|------|-----------|
| 1 | MIE, 4-0-4, Tissue |
| 2 | 4-1-5, Candle, Windmill |
| 3 | 4-2-5, Windmill, Dandelion |
| 4 | 4-2-5, Boat, Candle |
| 5 | 4-7-11, Tissue, Windmill |
| 6 | 4-3-5, Windmill, Candle |
| 7 | 4-3-5, Windmill, Candle |
| 8 | 4-7-11, Boat, Candle |
| 9 | 4-7-11, Windmill, Dandelion |

## Progression System

- Each unit has 3 exercises
- `noOfCompletions = min(completion count across all 3 exercises)`
- Medal thresholds: 3 = bronze, 4 = silver, 5 = gold
- Gold unlocks the next unit (unless `enableAllUnits` flag is set)
- Per-exercise difficulty is tracked and adjusted:
  - Success: difficulty + 10
  - Failure: max(0, difficulty - 5)

## Data Model (was Firebase RTDB)

```
users/{deviceId}/
  personalInfo: { firstName, lastName, email, gender, age, weight, height }
  survey: [response1, response2, ...]
  progression: [
    {
      unitId: 1,
      noOfCompletions: 0-5,
      data: {
        "ExerciseName": [[exerciseResult], ...],
      },
      difficulties: {
        Candle: number,
        Tissue: number,
        ...
      }
    },
    ...
  ]
```

This will be migrated to Supabase PostgreSQL -- see `IMPLEMENTATION_PLAN.md` Phase 5.

## What to Reuse in KMP

1. **Exercise definitions** -- unit/exercise mapping, exercise flow structure
2. **Barometer thresholds and algorithms** -- pressure delta detection, velocity calc, trapezoidal integration
3. **Progression math** -- completion counting, difficulty adjustment, medal thresholds
4. **Calibration flow** -- inhale/exhale peak detection for per-user calibration
5. **Animations concepts** -- candle flame, windmill spin, dandelion seeds, sailboat movement (reimplement in Compose)
6. **i18n structure** -- EN/DE translations pattern

## What NOT to Reuse

- NativeBase/react-native-paper UI components (replaced by Compose Multiplatform)
- Firebase integration (replaced by Supabase)
- React Navigation (replaced by Compose Navigation / Decompose)
- The polling-based device ID approach (use proper async in KMP)
