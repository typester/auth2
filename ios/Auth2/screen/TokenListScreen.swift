//
//  TokenListScreen.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI

struct TokenListScreen: View {
    @State private var tokens: [Token] = []
    @State private var showAddTokenScreen = false
    
    var body: some View {
        NavigationStack {
            List(tokens, id: \.id) { token in
                NavigationLink {
                    TokenDetailScreen(id: token.id)
                } label: {
                    Text(token.account)
                }
            }
            .navigationTitle("Auth2")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showAddTokenScreen = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showAddTokenScreen) {
                AddTokenScreen(
                    onCancel: {
                        showAddTokenScreen = false
                    }, onSave: {
                        showAddTokenScreen = false
                        Task {
                            await load()
                        }
                    }
                )
            }
            .task {
                await load()
            }
        }
    }
    
    private func load() async {
        do {
            tokens = try await Auth2Bridge.shared().listTokens()
        } catch {
            print("failed to load tokens: \(error)")
        }
    }
}

#Preview {
    TokenListScreen()
}
