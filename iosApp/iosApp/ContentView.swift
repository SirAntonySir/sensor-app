import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = SensorViewModel()

    var body: some View {
        NavigationStack {
            DashboardView(viewModel: viewModel)
        }
    }
}

#Preview {
    ContentView()
}
