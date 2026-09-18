package com.awindyendprod.storage_manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.ArchivedItem
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.services.ArchiveStore
import com.awindyendprod.storage_manager.services.toLongDisplayFormat
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel

/**
 * The record of deleted items, newest first. Search is deliberately narrow: client name or item
 * name, which is all the user has to go on when a client asks where their order went.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val archivedItems by viewModel.archivedItems.collectAsState()

    // Applies the retention window on entry, so a changed setting takes effect without a restart.
    LaunchedEffect(Unit) { viewModel.reloadArchive() }

    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var itemPendingDelete by remember { mutableStateOf<ArchivedItem?>(null) }

    // Opening search should land the cursor in the field with the keyboard already up, rather than
    // leaving the user to find and tap it.
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(searchActive) {
        if (searchActive) {
            runCatching { searchFocusRequester.requestFocus() }
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    val visibleItems = remember(archivedItems, searchQuery) {
        archivedItems.filter { ArchiveStore.matches(it, searchQuery) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.archive_search_hint)) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            // Filtering is live, so the search key just gets the keyboard out of the way.
                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            colors = TextFieldDefaults.colors(
                                // A filled, rounded container so it reads as a field; the indicator
                                // line underneath would be hidden by the app bar anyway.
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(R.string.archive))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        }
                    ) {
                        Icon(
                            imageVector = if (searchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = stringResource(R.string.search)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (visibleItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(
                        if (archivedItems.isEmpty()) R.string.archive_empty else R.string.archive_no_matches
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleItems, key = { it.item.id }) { archived ->
                    ArchivedItemCard(
                        archived = archived,
                        settings = settings,
                        onDelete = { itemPendingDelete = archived }
                    )
                }
            }
        }
    }

    itemPendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.archive_confirm_delete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteArchivedItem(pending.item.id)
                        itemPendingDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemPendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ArchivedItemCard(
    archived: ArchivedItem,
    settings: Settings,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archived.item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (archived.item.clientName.isNotBlank()) {
                    Text(
                        text = archived.item.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (archived.item.note.isNotBlank()) {
                    Text(
                        text = archived.item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val location = listOf(archived.shelfName, archived.sectionName)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                Text(
                    text = if (location.isNotBlank()) {
                        stringResource(
                            R.string.archived_on_from,
                            archived.archivedAt.toLongDisplayFormat(settings),
                            location
                        )
                    } else {
                        stringResource(
                            R.string.archived_on,
                            archived.archivedAt.toLongDisplayFormat(settings)
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.archive_delete_permanently),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
