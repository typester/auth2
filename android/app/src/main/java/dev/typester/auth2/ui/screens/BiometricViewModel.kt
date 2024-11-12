package dev.typester.auth2.ui.screens

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class BiometricUiState(
    val authenticated: Boolean = false,
    val error: String? = null,
)

class BiometricUiStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BiometricUiState())
    val uiState = _uiState.asStateFlow()

    fun authenticate(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }

            authenticateUser(
                context = context,
                onAuthenticated = {
                    _uiState.update { it.copy(authenticated = true) }
                },
                onError = { error ->
                    _uiState.update { it.copy(error = error) }
                }
            )
        }
    }
}

fun authenticateUser(
    context: Context,
    onAuthenticated: () -> Unit,
    onError: (String) -> Unit
) {
    val executor: Executor = Executors.newSingleThreadExecutor()
    val biometricPrompt = BiometricPrompt(
        context as FragmentActivity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Authentication failed")
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Authenticate to continue")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
