import SwiftUI
import RiveRuntime

/// Renders a Rive animation driven by exercise engine state.
/// Falls back to the Compose animation if no .riv file is found.
struct RiveExerciseAnimation: View {
    let exerciseName: String
    let pressure: Float
    let colorZone: Int // 0=Red, 1=Orange, 2=Green
    let progress: Float // 0-1 for sailboat
    let stage: Int // 0-3 for dandelion

    @State private var riveVM: RiveViewModel?

    init(exerciseName: String, pressure: Float = 0, colorZone: Int = 0, progress: Float = 0, stage: Int = 0) {
        self.exerciseName = exerciseName
        self.pressure = pressure
        self.colorZone = colorZone
        self.progress = progress
        self.stage = stage
    }

    var body: some View {
        Group {
            if let vm = riveVM {
                vm.view()
                    .frame(height: 300)
                    .onChange(of: pressure) { _, newValue in
                        updateInputs(vm: vm)
                    }
                    .onChange(of: progress) { _, newValue in
                        updateInputs(vm: vm)
                    }
                    .onChange(of: stage) { _, newValue in
                        updateInputs(vm: vm)
                    }
            } else {
                // No .riv file — show placeholder
                Image(systemName: exerciseIcon)
                    .font(.system(size: 100))
                    .foregroundStyle(.teal)
                    .frame(height: 300)
            }
        }
        .onAppear {
            loadRiveFile()
        }
    }

    private func loadRiveFile() {
        let fileName = riveFileName
        if Bundle.main.url(forResource: fileName, withExtension: "riv") != nil {
            riveVM = RiveViewModel(fileName: fileName)
        }
    }

    private func updateInputs(vm: RiveViewModel) {
        do {
            switch exerciseName {
            case "Candle":
                try vm.setInput("blowForce", value: Double(pressure.clamped(to: 0...100)))
            case "Windmill":
                let speed: Double = pressure > 30 ? (pressure > 60 ? 90 : 50) : 10
                try vm.setInput("speed", value: speed)
            case "Tissue":
                try vm.setInput("blowForce", value: Double(pressure.clamped(to: 0...100)))
            case "Dandelion":
                try vm.setInput("stage", value: Double(stage))
            case "Boat":
                try vm.setInput("progress", value: Double(progress * 100))
            default:
                break
            }
        } catch {
            // State machine input not found in this .riv file — expected for community placeholders
        }
    }

    private var riveFileName: String {
        switch exerciseName {
        case "Candle": return "candle"
        case "Windmill": return "windmill"
        case "Tissue": return "tissue"
        case "Dandelion": return "dandelion"
        case "Boat": return "sailboat"
        default: return exerciseName.lowercased()
        }
    }

    private var exerciseIcon: String {
        switch exerciseName {
        case "Candle": return "flame.fill"
        case "Windmill": return "wind"
        case "Tissue": return "doc.fill"
        case "Dandelion": return "leaf.fill"
        case "Boat": return "sailboat.fill"
        default: return "lungs.fill"
        }
    }
}

extension Float {
    func clamped(to range: ClosedRange<Float>) -> Float {
        return Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
