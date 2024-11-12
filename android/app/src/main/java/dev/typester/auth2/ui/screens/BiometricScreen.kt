package dev.typester.auth2.ui.screens

import android.annotation.SuppressLint
import androidx.biometric.BiometricViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

@Serializable
object Biometric

@Composable
fun BiometricScreen(
    viewModel: BiometricUiStateViewModel = viewModel(),
    onFinishAuthentication: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(true) {
        viewModel.authenticate(context)
    }

    LaunchedEffect(uiState.authenticated) {
        if (uiState.authenticated) {
            onFinishAuthentication()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()

        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.authenticate(context)
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.authenticate(context)
                    }) {
                        Text("Retry")
                    }
                },
                title = {
                    Text("Failed to authentication")
                },
                text = {
                    Text(uiState.error.toString())
                }
            )
        }
    }
}