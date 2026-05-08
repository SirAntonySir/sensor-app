import Foundation

struct ExerciseUnit: Identifiable {
    let id: Int
    let name: String
    let exercises: [String]
    var completions: Int
    var isUnlocked: Bool

    var medal: String? {
        switch completions {
        case 5...: return "🥇"
        case 4: return "🥈"
        case 3: return "🥉"
        default: return nil
        }
    }

    static let allUnits: [ExerciseUnit] = [
        ExerciseUnit(id: 1, name: "Unit 1", exercises: ["MIE", "4-0-4", "FloatBall"], completions: 0, isUnlocked: true),
        ExerciseUnit(id: 2, name: "Unit 2", exercises: ["4-1-5", "Candle", "Windmill"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 3, name: "Unit 3", exercises: ["4-2-5", "Windmill", "Dandelion"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 4, name: "Unit 4", exercises: ["4-2-5", "Candle"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 5, name: "Unit 5", exercises: ["4-7-11", "Windmill"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 6, name: "Unit 6", exercises: ["4-3-5", "Windmill", "Candle"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 7, name: "Unit 7", exercises: ["4-3-5", "Windmill", "Candle"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 8, name: "Unit 8", exercises: ["4-7-11", "Candle"], completions: 0, isUnlocked: false),
        ExerciseUnit(id: 9, name: "Unit 9", exercises: ["4-7-11", "Windmill", "Dandelion"], completions: 0, isUnlocked: false),
    ]
}
