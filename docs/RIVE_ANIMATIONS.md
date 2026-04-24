# Rive Animation Guide

## Overview

Each exercise uses a Rive (.riv) animation file driven by sensor data in real-time.
The exercise engine produces state values that are fed into the Rive state machine inputs.

## Current Placeholder Animations

Community .riv files are used as placeholders (CC BY 4.0, credited to Rive community):
- `candle.riv` — Fire animation (from Rive Fire by yurat)
- `dandelion.riv` — Flower animation (from flower animation by singh)
- `tissue.riv` — Liquid animation (from Liquid Download by guidorosso)
- `sailboat.riv` — Rocket animation (from Rocket Ship Animation by garys)

These are visual placeholders only — they don't respond to state machine inputs.
Replace them with custom .riv files that implement the input contracts below.

## State Machine Input Contract

Each custom .riv file should have a state machine named `"State Machine 1"` with these inputs:

### Candle (`candle.riv`)
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `blowForce` | Number | 0-100 | Current blow pressure. Drive flame flicker intensity, extinguish at high values |
| `isBlownOut` | Boolean | — | True when candle is extinguished, trigger smoke animation |

Design: Candle with flame that flickers faster/harder as blowForce increases.
At ~80+ the flame should shrink dramatically. When isBlownOut triggers, show smoke wisps.

### Windmill (`windmill.riv`)
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `speed` | Number | 0-100 | Blade rotation speed. 0=still, 50=moderate, 100=maximum |

Design: Windmill with rotating blades. Speed input directly controls rotation rate.
Add subtle body sway at high speeds for polish.

### Tissue (`tissue.riv`)
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `blowForce` | Number | 0-100 | Current blow pressure. Drives tissue flutter intensity |

Design: Tissue paper that flutters/lifts based on blow force.
Low values = gentle ripple, high values = tissue flies upward.

### Dandelion (`dandelion.riv`)
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `stage` | Number | 0-3 | 0=still, 1=first blow (some seeds fly), 2=partial, 3=all seeds gone |

Design: Dandelion with seeds. Each stage releases more seeds with particle-like motion.
Seeds should drift naturally with slight randomness in trajectory.

### Sailboat (`sailboat.riv`)
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `progress` | Number | 0-100 | Boat position across water. 0=start, 100=finish |

Design: Ocean scene with sailboat. Boat translates left-to-right as progress increases.
Add wave animation, sail billowing, and wake trail behind boat.

### Countdown Timer (`countdown.riv`) — optional
| Input | Type | Range | Description |
|-------|------|-------|-------------|
| `progress` | Number | 0-1 | Timer progress (1=full, 0=done) |
| `phase` | Number | 0-2 | 0=inhale(green), 1=hold(orange), 2=exhale(blue) |

## File Locations

### Android
Place .riv files in: `androidApp/src/androidMain/res/raw/`
- `candle.riv`, `windmill.riv`, `tissue.riv`, `dandelion.riv`, `sailboat.riv`

### iOS
Place .riv files in: `iosApp/iosApp/RiveAssets/`
- Same filenames as Android

## Creating Custom Animations

1. Open [rive.app](https://rive.app) (free account)
2. Create a new file
3. Design the animation artboard
4. Add a State Machine named `"State Machine 1"`
5. Add the inputs from the contract table above
6. Create states and transitions driven by the inputs
7. Export as .riv
8. Place in both platform directories

## Fallback Behavior

If a .riv file is missing for an exercise:
- **Android**: Falls back to the shared Compose Multiplatform animation (PNG-based)
- **iOS**: Shows a static SF Symbol icon as placeholder
