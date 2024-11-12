package dev.typester.auth2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Serializable
data object DataMigration

@Composable
fun DataMigrationScreen(
    viewModel: DataMigrationViewModel = viewModel(),
    navigateToMain: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.migrate()
    }

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            navigateToMain()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (uiState.migrating) {
            CircularProgressIndicator()
            Text(text = "Upgrading database...")
        }
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.resetAndMigrate()
                    }) {
                        Text(text = "Reset database")
                    }
                },
                title = {
                    Text(text = "Data migration error")
                },
                text = {
                    Text(text = "Failed to upgrade database. Need to reset.")
                },
            )
        }
    }
}