package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.typester.auth2.Shared
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.auth2.Exception
import uniffi.auth2.TokenAlg

private const val TAG = "AddTokenViewModel"

data class AddTokenUiState(
    val account: String = "",
    val service: String = "",
    val secret: String = "",
    val algorithm: TokenAlg = TokenAlg.SHA1,
    val digits: UInt = 6u,
    val period: UInt = 30u,
    val saved: Boolean = false,
)

class AddTokenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AddTokenUiState())
    val uiState = _uiState.asStateFlow()

    fun updateAccount(account: String) {
        _uiState.value = _uiState.value.copy(account = account)
    }

    fun updateService(service: String) {
        _uiState.value = _uiState.value.copy(service = service)
    }

    fun updateSecret(secret: String) {
        _uiState.value = _uiState.value.copy(secret = secret)
    }

    fun updateAlgorithm(algorithm: TokenAlg) {
        _uiState.value = _uiState.value.copy(algorithm = algorithm)
    }

    fun updateDigits(digits: UInt) {
        _uiState.value = _uiState.value.copy(digits = digits)
    }

    fun updatePeriod(period: UInt) {
        _uiState.value = _uiState.value.copy(period = period)
    }

    fun save() {
        viewModelScope.launch {
            val saved = AddEntry(
                account = uiState.value.account,
                service = uiState.value.service,
                secret = uiState.value.secret,
            )
            if (saved) {
                _uiState.value = _uiState.value.copy(saved = saved)
            }
        }
    }
}

suspend fun AddEntry(
    account: String,
    service: String,
    secret: String,
) : Boolean {
    try {
        val core = Shared.instance()
        core.addToken(
            account = account,
            service = if (service.isEmpty()) null else service,
            secret = secret,
            algorithm = null,
            digits = null,
            period = null,
        )
        return true
    } catch (e: Exception) {
        Log.e(TAG, "Error adding token", e)
    }
    return false
}