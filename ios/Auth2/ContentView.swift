//
//  ContentView.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI

struct ContentView: View {
    @State private var isMigrationComplete: Bool = false

    var body: some View {
        Group {
            if isMigrationComplete {
                TokenListScreen()
            } else {
                DataMigrationScreen(isMigrationComplete: $isMigrationComplete)
            }
        }
    }
}

#Preview {
    ContentView()
}
