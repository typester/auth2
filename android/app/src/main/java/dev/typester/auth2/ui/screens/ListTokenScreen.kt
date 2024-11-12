package dev.typester.auth2.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uniffi.auth2.Token

private const val TAG = "ListTokenScreen"

@Serializable
data object ListToken

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTokenScreen(
    viewModel: ListTokenViewModel = viewModel(),
    navigateToAddToken: () -> Unit,
    navigateToDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var bottomSheetItem by remember { mutableStateOf<Token?>(null) }
    var deleteItem by remember { mutableStateOf<Token?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(true) {
        viewModel.load()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            CenterAlignedTopAppBar(
                title = {
                    Text("Auth2")
                },
                actions = {
                    IconButton(onClick = {
                        navigateToAddToken()
                    }) {
                        Icon(Icons.Filled.Add, "Add")
                    }
                }
            )
        }

        if (bottomSheetItem != null) {
            item {
                ModalBottomSheet(
                    onDismissRequest = {
                        bottomSheetItem = null
                    },
                    sheetState = sheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        ListItem(
                            headlineContent = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "delete")
                                    Text("Delete")
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                            ),
                            modifier = Modifier.clickable {
                                deleteItem = bottomSheetItem
                                bottomSheetItem = null
                            }
                        )
                    }
                }
            }
        }

        if (deleteItem != null) {
            item {
                AlertDialog(
                    title = {
                        if (!deleteItem?.service.isNullOrBlank()) {
                            Text("Delete ${deleteItem?.service}: ${deleteItem?.account}?")
                        } else {
                            Text("Delete ${deleteItem?.account}?")
                        }
                    },
                    text = {
                        Text("Are you sure you want to delete this entry? This action cannot be undone.")
                    },
                    onDismissRequest = {
                        deleteItem = null
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            deleteItem?.let { viewModel.deleteToken(it.id) }
                            deleteItem = null
                        }) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            deleteItem = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        items(uiState.tokens) { item ->
            ListItem(
                headlineContent = {
                    if (!item.service.isNullOrBlank()) {
                        Text("${item.service}: ${item.account}")
                    } else {
                        Text(item.account)
                    }
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            bottomSheetItem = item
                        }
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "menu")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        Log.d(TAG, "click item: ${item.id}")
                        navigateToDetail(item.id.toString())
                    }
            )
            HorizontalDivider()
        }
    }
}