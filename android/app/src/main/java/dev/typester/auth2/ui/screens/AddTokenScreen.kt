package dev.typester.auth2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Serializable
object AddToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTokenScreen(
    viewModel: AddTokenViewModel = viewModel(),
    onBack: () -> Unit,
    onAddToken: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onAddToken()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text("Add Entry Manually")
            },
            navigationIcon = {
                IconButton(onClick = {
                    onBack()
                }) {
                    Icon(Icons.Filled.ArrowBack, null)
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        viewModel.save()
                    },
                    enabled = uiState.account.isNotEmpty() && uiState.secret.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Check, null)
                }
            },
            scrollBehavior = scrollBehavior,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                value = uiState.account,
                onValueChange = { viewModel.updateAccount(it) },
                label = {
                    Text("Account")
                }
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                value = uiState.service,
                onValueChange = { viewModel.updateService(it) },
                label = {
                    Text("Service (optional)")
                }
            )
            TextField(
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                value = uiState.secret,
                onValueChange = { viewModel.updateSecret(it) },
                label = {
                    Text("Secret")
                }
            )

            Text(
                text = "If you want to add an entry using a QR code, please scan the QR code with a QR code reader or camera app. This app supports otpauth:// links, so you can add entries directly through those apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}