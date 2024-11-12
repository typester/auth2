//
//  ContentView.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI

struct ContentView: View {
    @State private var isMigrationComplete: Bool = false
    @State private var isEncryptionKeyAbailable: Bool = false

    var body: some View {
        Group {
            if !isMigrationComplete {
                DataMigrationScreen(isMigrationComplete: $isMigrationComplete)
            } else if !isEncryptionKeyAbailable {
                CreateEncryptionKeyScreen {
                    isEncryptionKeyAbailable = true
                }
            } else {
                TokenListScreen()
            }
        }
        .onAppear {
            if let _ = iOSKeyStore().get() {
                isEncryptionKeyAbailable = true
            }
        }
    }

}

#Preview {
    ContentView()
}
