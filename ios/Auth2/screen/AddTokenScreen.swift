//
//  AddTokenScreen.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI

struct AddTokenScreen: View {
    @State private var adding: Bool = false
    @State private var account: String = ""
    @State private var service: String = ""
    @State private var secret: String = ""
    let onCancel: () -> Void
    let onSave: () -> Void
    
    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Account")) {
                    TextField("Enter account", text: $account)
                }
                Section(header: Text("Service")) {
                    TextField("Enter service (optional)", text: $service)
                }
                Section(header: Text("Secret")) {
                    TextField("Enter secret", text: $secret)
                }
                Button("Add") {
                    Task {
                        await save()
                    }
                }.disabled(adding || account.isEmpty || secret.isEmpty)
            }
            .navigationTitle("Add entry manually")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        onCancel()
                    }
                }
            }
        }
    }
    
    private func save() async {
        do {
            adding = true
            defer {
                adding = false
            }
            let s: String? = service.isEmpty ? nil : service
            let _ = try await Auth2Bridge.shared().addToken(account: account, service: s, secret: secret, algorithm: nil, digits: nil, period: nil)
            onSave()
        } catch {
            print("failed to add entry: \(error)")
        }
    }
}
