//
//  CreateEncryptionKeyScreen.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/12/24.
//

import SwiftUI
import Security

enum KeychainKeys {
    static let serviceName = "dev.typester.auth2"
    static let encryptionKey = "encryptionKey"
}

class iOSKeyStore: KeyStore {
    func get() -> String? {
        let keychainQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: KeychainKeys.serviceName,
            kSecAttrAccount as String: KeychainKeys.encryptionKey,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: AnyObject?
        let status = SecItemCopyMatching(keychainQuery as CFDictionary, &item)
        
        if status != errSecSuccess {
            print("failed to get encryptionKey: status=\(status)")
            return nil
        }
        
        if let data = item as? Data, let encryptionKey = String(data: data, encoding: .utf8) {
            return encryptionKey
        } else {
            print("failed to decode encryption key")
            return nil
        }
    }
}

struct CreateEncryptionKeyScreen: View {
    @State private var encryptionKey: String = ""
    let onSave: () -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Encryption key")) {
                    TextField("Encryption key", text: $encryptionKey)
                    Text("Please set a strong encryption key to secure your 2FA secrets.\n" +
                         "Once set, this key cannot be changed, so we strongly recommend choosing a sufficiently strong and secure string from the start.")
                }
                Button("Create") {
                    saveKey()
                }.disabled(encryptionKey.isEmpty)
            }.navigationTitle("Set an encryption key")
        }
    }
    
    private func saveKey() {
        let keychainQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: KeychainKeys.serviceName,
            kSecAttrAccount as String: KeychainKeys.encryptionKey,
            kSecValueData as String: encryptionKey.data(using: .utf8)!
        ]
        
        let status = SecItemAdd(keychainQuery as CFDictionary, nil)
        
        if status != errSecSuccess {
            print("failed to save encryptionKey: status=\(status)")
            return
        }
        
        onSave()
    }
}

