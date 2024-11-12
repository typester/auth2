//
//  TokenDetailScreen.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/12/24.
//

import SwiftUI

struct TokenDetailScreen: View {
    let id: UInt64
    @State var token: TokenDetail?
    
    @State private var timer: Timer?
    @State private var current: String?
    @State private var remaining: Int = 0
    
    var body: some View {
        NavigationStack {
            Form {
                if let token = token {
                    Section("token") {
                        VStack {
                            Text(current ?? "")
                                .frame(maxWidth: .infinity, alignment: .center)
                                .font(.system(size: 42))
                            Text("\(remaining) s")
                                .frame(maxWidth: .infinity, alignment: .center)
                        }.padding()
                    }
                    
                    Section("account") {
                        Text(token.account)
                    }
                    if let service = token.service {
                        Section("service") {
                            Text(service)
                        }
                    }
                    Section("algorithm") {
                        let label = switch token.algorithm {
                            case .sha1: "SHA1"
                            case .sha256: "SHA256"
                            case .sha512: "SHA512"
                        }
                        Text(label)
                    }
                    Section("digits") {
                        Text(String(token.digits))
                    }
                    Section("period") {
                        Text(String(token.period) + "s")
                    }
                }
            }.navigationTitle(title())
        }
        .onAppear {
            startListeningAppLifecycle()
            Task {
                await load()
            }
        }
        .onDisappear {
            timer?.invalidate()
            stopListeningAppLifecycle()
        }
        .onChange(of: token) { _, newToken in
            if newToken != nil {
                Task {
                    await generate()
                }
            }
        }
        .onChange(of: current) { _, newToken in
            if newToken != nil {
                startTimer()
            }
        }
    }
    
    private func title() -> String {
        if let token = token {
            if let service = token.service {
                return "\(service): \(token.account)"
            } else {
                return token.account
            }
        } else {
            return ""
        }
    }
    
    private func load() async {
        do {
            token = try await Auth2Bridge.shared().tokenDetail(id: id)
        } catch {
            print("failed to load token detail: \(error)")
        }
    }
    
    private func generate() async {
        if let token = token {
            do {
                let bridge = Auth2Bridge.shared()
                let res = try await bridge.generateCurrent(id: token.id)
                remaining = Int(res.expires)
                current = res.current
            } catch {
                print("failed to generate token: \(error)")
            }
        }
    }
    
    private func startTimer() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if remaining > 0 {
                remaining -= 1
            }
            if remaining == 0 {
                Task {
                    await generate()
                }
            }
        }
    }
    
    private func startListeningAppLifecycle() {
        NotificationCenter.default.addObserver(forName: UIApplication.willEnterForegroundNotification, object: nil, queue: .main) { _ in
            Task {
                await generate()
            }
        }
    }
    
    private func stopListeningAppLifecycle() {
        NotificationCenter.default.removeObserver(self, name: UIApplication.willEnterForegroundNotification, object: nil)
    }
}

#Preview {
    TokenDetailScreen(
        id: 123,
        token: TokenDetail(id: 123, account: "dameleon", service: "Foo", algorithm: .sha1, digits: 6, period: 30)
    )
}
