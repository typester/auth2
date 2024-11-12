package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.typester.auth2.Shared
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.auth2.Exception

private const val TAG = "DataMigrationViewModel"

data class DataMigrationUiState(
    val migrating: Boolean = false,
    val finished: Boolean = false,
    val error: String? = null,
)

class DataMigrationViewModel: ViewModel() {
    private val _uiState = MutableStateFlow(DataMigrationUiState())
    val uiState = _uiState.asStateFlow()

    fun migrate() {
        viewModelScope.launch {
            val core = Shared.instance()

            try {
                if (core.dbIsMigrationAvailable()) {
                    _uiState.update { it.copy(migrating = true) }
                    withContext(Dispatchers.IO) {
                        core.dbRunMigration()
                    }
                }
                _uiState.update { it.copy(finished = true) }
            } catch (e: Exception) {
                Log.e(TAG, "migration error", e)
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(migrating = false) }
            }
        }
    }

    fun resetAndMigrate() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                withContext(Dispatchers.IO) {
                    val core = Shared.instance()
                    core.dbReset()
                    core.dbRunMigration()
                }
                _uiState.update { it.copy(finished = true) }
            } catch (e: Exception) {
                Log.e(TAG, "database reset error", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}