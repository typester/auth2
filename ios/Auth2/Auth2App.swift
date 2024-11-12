//
//  Auth2App.swift
//  Auth2
//
//  Created by Daisuke Murase on 11/11/24.
//

import SwiftUI
import os

@main
struct Auth2App: App {
    init() {
        initLogger(logger: DebugLogger())
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

class DebugLogger: Logger {
    private let logger: os.Logger

    init() {
        self.logger = os.Logger(subsystem: "dev.typester.auth2", category: "core")
    }

    func log(msg: String) {
        logger.debug("\(msg)")
    }
}

extension Auth2Bridge {
    private static var sharedInstance: Auth2Bridge = {
        let databaseUrl = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("database.db")
        if !FileManager.default.fileExists(atPath: databaseUrl.path()) {
            FileManager.default.createFile(atPath: databaseUrl.path(), contents: nil)
        }

        let config = Config(databaseUrl: "sqlite://\(databaseUrl.path())", keyStore: iOSKeyStore())
        let bridge = try! Auth2Bridge(config: config)
        return bridge
    }()
    
    class func shared() -> Auth2Bridge {
        return sharedInstance
    }
}
