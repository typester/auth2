package dev.typester.auth2

import android.content.Context
import android.net.Uri
import dev.typester.auth2.ui.screens.AndroidKeyStore
import uniffi.auth2.Auth2Bridge
import uniffi.auth2.Config
import java.io.File

class Shared private constructor() {
    companion object {
        private var obj: Auth2Bridge? = null

        fun instance(): Auth2Bridge {
            if (obj == null) {
                synchronized(this) {
                    if (obj == null) {
                        val databaseFile = File(SharedContext.context().filesDir, "database.db")
                        if (!databaseFile.exists()) {
                            databaseFile.createNewFile()
                        }
                        val databaseUri = Uri.fromFile(databaseFile)

                        val config = Config(
                            databaseUrl = "sqlite://" + databaseUri.path,
                            keyStore = AndroidKeyStore(),
                        )
                        obj = Auth2Bridge(config)
                    }
                }
            }
            return obj!!
        }

    }
}

class SharedContext private constructor() {
    companion object {
        private var context: Context? = null

        fun context(): Context {
            return context!!
        }

        fun setContext(context: Context) {
            this.context = context
        }
    }
}