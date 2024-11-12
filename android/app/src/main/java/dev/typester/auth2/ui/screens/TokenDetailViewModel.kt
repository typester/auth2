package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.typester.auth2.Shared
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.auth2.Exception
import uniffi.auth2.TokenDetail

private const val Tag = "TokenDetailViewModel"

data class TokenDetailUiState(
    val token: TokenDetail? = null,
    val current: String? = null,
    val next: UInt? = null,
    val updatedAt: Int = 0,
)

class TokenDetailViewModel : ViewModel() {
    private var _uiState = MutableStateFlow(TokenDetailUiState())
    var uiState = _uiState.asStateFlow()

    fun load(id: ULong) {
        viewModelScope.launch {
            try {
                val core = Shared.instance()
                val token = core.tokenDetail(id)
                token?.let { token ->
                    _uiState.update { it.copy(token = token) }
                    generate()
                }

            } catch (e: Exception) {
                Log.e(Tag, "load", e)
            }
        }
    }

    fun generate() {
        Log.d(Tag, "generate")
        _uiState.value.token?.let { token ->
            viewModelScope.launch {
                try {
                    val current = Shared.instance().generateCurrent(token.id)
                    Log.d(Tag, "generated: $current")
                    _uiState.update {
                        it.copy(
                            current = current.current,
                            next = current.expires,
                        )
                    }
                    _uiState.update {
                        it.copy(updatedAt = System.currentTimeMillis().toInt())
                    }
                } catch (e: Exception) {
                    Log.e(Tag, "generate", e)
                }
            }
        }
    }
}