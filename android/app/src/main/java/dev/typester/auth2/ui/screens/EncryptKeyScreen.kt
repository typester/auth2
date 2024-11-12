package dev.typester.auth2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun EncryptKeyScreen(
    viewModel: EncryptKeyViewModel = viewModel(),
    editMode: Boolean = false,
    onKeyIsAvailable: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val autofill = LocalAutofill.current
    val autofillMode = remember {
        AutofillNode(
            onFill = { viewModel.updateKey(it) },
            autofillTypes = listOf(AutofillType.Password)
        )
    }

    if (!editMode) {
        LaunchedEffect(uiState.restored) {
            if (uiState.restored == true) {
                onKeyIsAvailable()
            }
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onKeyIsAvailable()
        }
    }

    if (uiState.restored == null || uiState.restored == true) {
        Surface(modifier = Modifier.fillMaxSize()) {  }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text("Set an encryption key")
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        enabled = uiState.encryptKey.isNotBlank(),
                        onClick = {
                            viewModel.saveKey()
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Set an Encryption Key for Your 2FA Secrets",
                    style = MaterialTheme.typography.titleSmall,
                )
                TextField(
                    value = uiState.encryptKey,
                    singleLine = true,
                    onValueChange = { viewModel.updateKey(it) },
                    label = {
                        Text("Encryption key")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardActions = KeyboardActions(onDone = { viewModel.saveKey() }),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Please set a strong encryption key to secure your 2FA secrets.\n" +
                            "Once set, this key cannot be changed, so we strongly recommend choosing a sufficiently strong and secure string from the start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}