import SwiftUI

struct UnitDetailView: View {
    let unit: ExerciseUnit
    @ObservedObject var viewModel: SensorViewModel

    var body: some View {
        List {
            ForEach(unit.exercises, id: \.self) { exerciseName in
                NavigationLink {
                    ExerciseView(exerciseName: exerciseName, unitId: unit.id, viewModel: viewModel)
                } label: {
                    ExerciseRow(name: exerciseName, completions: viewModel.completionCount(for: exerciseName, in: unit.id))
                }
            }
        }
        .navigationTitle(unit.name)
    }
}

struct ExerciseRow: View {
    let name: String
    let completions: Int

    var icon: String {
        switch name {
        case "Candle": return "flame.fill"
        case "Windmill": return "wind"
        case "Tissue": return "doc.fill"
        case "Dandelion": return "leaf.fill"
        case "Boat": return "sailboat.fill"
        case "Straw": return "straw.fill"
        case "MIE": return "lungs.fill"
        default: return "timer"
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(.teal)
                .frame(width: 32)

            VStack(alignment: .leading) {
                Text(name)
                    .font(.body)
                Text("\(completions) / 5 completions")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            ProgressView(value: Double(min(completions, 5)), total: 5)
                .frame(width: 60)
                .tint(.teal)
        }
        .padding(.vertical, 4)
    }
}
