import Foundation
import SwiftUI

@MainActor
class SensorViewModel: ObservableObject {
    @Published var units: [ExerciseUnit] = ExerciseUnit.allUnits
    @Published private var exerciseCompletions: [String: [String: Int]] = [:] // unitId -> exerciseName -> count

    func completionCount(for exercise: String, in unitId: Int) -> Int {
        exerciseCompletions["\(unitId)"]?[exercise] ?? 0
    }

    func recordCompletion(exercise: String, unitId: Int) {
        let key = "\(unitId)"
        if exerciseCompletions[key] == nil {
            exerciseCompletions[key] = [:]
        }
        let current = exerciseCompletions[key]?[exercise] ?? 0
        exerciseCompletions[key]?[exercise] = current + 1

        updateProgression()
    }

    private func updateProgression() {
        for i in units.indices {
            let unit = units[i]
            let key = "\(unit.id)"
            guard let completions = exerciseCompletions[key] else { continue }

            if completions.count >= unit.exercises.count {
                let minCompletions = unit.exercises.compactMap { completions[$0] }.min() ?? 0
                units[i].completions = min(minCompletions, 5)
            }

            // Unlock next unit if current has 5+ completions
            if units[i].completions >= 5, i + 1 < units.count {
                units[i + 1].isUnlocked = true
            }
        }
    }
}
