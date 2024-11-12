package dev.typester.auth2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.typester.auth2.ui.theme.Auth2Theme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import uniffi.auth2.Logger
import uniffi.auth2.initLogger

private const val TAG = "MainActivity"
private const val LOGGER_TAG = "Core"

class DebugLogger() : Logger {
    override fun log(msg: String) {
        Log.d(LOGGER_TAG, msg)
    }
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedContext.setContext(applicationContext)
        initLogger(DebugLogger())

        enableEdgeToEdge()
        setContent {
            Auth2Theme {
                Surface {
                    RootGraph()
                }
            }
        }

        intent.data?.let { data ->
            Log.d(TAG, "handleUrl in onCreate: $data")
            lifecycleScope.launch {
                OtpLinkHandler.handleUrl(data.toString())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        intent.data?.let { data ->
            Log.d(TAG, "handleUrl: $data")
            lifecycleScope.launch {
                OtpLinkHandler.handleUrl(data.toString())
            }
        }
    }
}

object OtpLinkHandler {
    private val _urlFlow = MutableSharedFlow<String>()
    val urlFlow = _urlFlow.asSharedFlow()

    private var _latestUrl: String? = null

    suspend fun handleUrl(url: String) {
        _latestUrl = url
        _urlFlow.emit(url)
    }

    fun latest(): String? {
        val latest = _latestUrl
        _latestUrl = null
        return latest
    }
}
