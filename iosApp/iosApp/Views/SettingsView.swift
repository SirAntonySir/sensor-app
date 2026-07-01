import SwiftUI
import shared

struct SettingsView: View {
    private let logger = IosSensorLogger()

    @AppStorage("forceMockSensor") private var forceMock = false
    @State private var logSize: Int64 = 0
    @State private var shareURL: ShareItem?

    var body: some View {
        Form {
            Section("Sensor data") {
                Text(logSize > 0
                     ? "Logged \(logSize / 1024) KB of readings"
                     : "No sensor data logged yet")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                Button {
                    if let path = logger.filePath() {
                        shareURL = ShareItem(url: URL(fileURLWithPath: path))
                    }
                } label: {
                    Label("Download sensor data", systemImage: "square.and.arrow.up")
                }
                .disabled(logSize == 0)

                Button(role: .destructive) {
                    logger.clear()
                    logSize = logger.sizeBytes()
                } label: {
                    Label("Clear log", systemImage: "trash")
                }
                .disabled(logSize == 0)
            }

            #if DEBUG
            Section("Developer") {
                Toggle("Force simulated sensor", isOn: $forceMock)
                Text("Use the mock breathing signal even if a barometer is present.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            #endif
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { logSize = logger.sizeBytes() }
        .sheet(item: $shareURL) { item in
            ActivityView(activityItems: [item.url])
        }
    }
}

/// Identifiable wrapper so `.sheet(item:)` can present a share sheet for a file URL.
private struct ShareItem: Identifiable {
    let url: URL
    var id: String { url.path }
}

/// Bridges UIActivityViewController (the native share sheet) into SwiftUI.
private struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}
