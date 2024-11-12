package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.typester.auth2.OtpLinkHandler
import dev.typester.auth2.Shared
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.auth2.Exception
import uniffi.auth2.Token

private const val TAG = "ListTokenViewModel"

data class ListTokenUiState(
    val tokens: List<Token> = emptyList(),
    val loading: Boolean = true,
)

class ListTokenViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(ListTokenUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            OtpLinkHandler.latest()?.let { url ->
                handleUrl(url)
            }

            Log.d(TAG, "start listening OTPLink")
            OtpLinkHandler.urlFlow.collect { _ ->
                OtpLinkHandler.latest()?.let { url ->
                    Log.d(TAG, "OtpURL received: $url")
                    handleUrl(url)
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            val core = Shared.instance()
            _uiState.update { it.copy(loading = true) }

            try {
                val tokens = core.listTokens()
                _uiState.update { it.copy(tokens = tokens) }
            } catch (e: Exception) {
                Log.e(TAG, "failed to read tokens", e)
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun handleUrl(url: String) {
        viewModelScope.launch {
            try {
                val core = Shared.instance()
                core.addTokenFromUrl(url)
                load()
            } catch (e: Exception) {
                Log.e(TAG, "failed to add token from url", e)
            }
        }
    }

    fun deleteToken(id: ULong) {
        viewModelScope.launch {
            val core = Shared.instance()
            try {
                core.removeToken(id)
                load()
            } catch (e: Exception) {
                Log.e(TAG, "failed to delete token", e)
            }
        }
    }
}