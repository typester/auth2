//
//  DataMigrationScreen.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI

struct DataMigrationScreen: View {
    @Binding var isMigrationComplete: Bool
    @State private var migrating: Bool = false
    @State private var showAlert: Bool = false
    
    var body: some View {
        VStack {
            if migrating {
                ProgressView()
                Text("Data migration in progress...")
            }
        }
        .task {
            await migrate()
        }
        .alert(isPresented: $showAlert) {
            Alert(
                title: Text("Data migration error"),
                message: Text("Failed to upgrade database. Need to reset."),
                dismissButton: .destructive(Text("Reset")) {
                    showAlert = false
                    Task {
                        await reset()
                    }
                }
            )
        }
    }
    
    private func migrate() async {
        do {
            let migrationAvailable = try await Auth2Bridge.shared().dbIsMigrationAvailable()
            if migrationAvailable {
                migrating = true
                defer {
                    migrating = false
                }
                try await Auth2Bridge.shared().dbRunMigration()
            }
            isMigrationComplete = true
        } catch {
            print("migration error: \(error)")
            showAlert = true
        }
    }
    
    private func reset() async {
        do {
            try await Auth2Bridge.shared().dbReset()
            await migrate()
        } catch {
            print("reset failed: \(error)")
        }
    }
}
