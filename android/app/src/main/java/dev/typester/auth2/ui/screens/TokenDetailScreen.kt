package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import uniffi.auth2.TokenAlg

private const val TAG = "TokenDetailScreen"

@Serializable
data class TokenDetail(val id: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenDetailScreen(
    id: String,
    viewModel: TokenDetailViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var remaining by remember { mutableStateOf(0) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.load(id.toLong().toULong())
    }

    LaunchedEffect(uiState.updatedAt) {
        Log.d(TAG, "effect: ${uiState.next}")
        uiState.next?.let { next ->
            Log.d(TAG, "next: $next")
            remaining = next.toInt()

            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining--
            }
            viewModel.generate()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    uiState.token?.let { token ->
                        if (!token.service.isNullOrBlank()) {
                            Text("${token.service}: ${token.account}")
                        } else {
                            Text("${token.account}")
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .clickable {
                        uiState.current?.let {
                            clipboardManager.setText(AnnotatedString(it))
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (!uiState.current.isNullOrBlank()) { uiState.current!! } else { "" },
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = "$remaining s",
                )
            }
        }

        item {
            HorizontalDivider()

            ListItem(
                headlineContent = {
                    uiState.token?.let { token ->
                        Text(token.account)
                    }
                },
                overlineContent = {
                    Text("account")
                }
            )
        }

        if (!uiState.token?.service.isNullOrBlank()) {
            item {
                ListItem(
                    headlineContent = {
                        uiState.token?.let { token ->
                            Text(token.service ?: "")
                        }
                    },
                    overlineContent = {
                        Text("service")
                    }
                )
            }
        }

        item {
            ListItem(
                headlineContent = {
                    uiState.token?.let { token ->
                        when (token.algorithm) {
                            TokenAlg.SHA1 -> Text("SHA1")
                            TokenAlg.SHA256 -> Text("SHA256")
                            TokenAlg.SHA512 -> Text("SHA512")
                        }
                    }
                },
                overlineContent = {
                    Text("algorithm")
                }
            )
        }

        item {
            ListItem(
                headlineContent = {
                    uiState.token?.let { token ->
                        Text(token.digits.toString())
                    }
                },
                overlineContent = {
                    Text("digits")
                }
            )
        }

        item {
            ListItem(
                headlineContent = {
                    uiState.token?.let { token ->
                        Text(token.period.toString() + "s")
                    }
                },
                overlineContent = {
                    Text("period")
                }
            )
        }

    }
}