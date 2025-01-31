import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.storage_manager.R
import com.example.storage_manager.model.Item
import com.example.storage_manager.model.Settings
import com.example.storage_manager.services.toDisplayFormat
import com.example.storage_manager.viewmodel.SettingsViewModel
import com.example.storage_manager.viewmodel.StorageTrackerViewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailsScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    shelfId: String,
    sectionId: String,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val section by viewModel.getSectionById(shelfId, sectionId).collectAsState()
    
    var isAddItemDialogVisible by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemClientName by remember { mutableStateOf("") }
    var newItemNote by remember { mutableStateOf("") }
    var newItemHasAlarm by remember { mutableStateOf(false) }
    var newItemEntryDate by remember { mutableStateOf(Date()) }
    var newItemReturnDate by remember { 
        mutableStateOf(
            Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.section_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isAddItemDialogVisible = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                section?.let {
                    items(it.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = item.clientName.ifEmpty { stringResource(R.string.unknown_client) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = stringResource(R.string.entry_date),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.entryDate?.toDisplayFormat(settings) ?: "N/A",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = stringResource(R.string.return_date),
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.returnDate?.toDisplayFormat(settings) ?: "N/A",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.removeItemFromSection(shelfId, sectionId, item.id)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cancel)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isAddItemDialogVisible) {
                AddItemDialog(
                    onDismiss = { isAddItemDialogVisible = false },
                    onAddItem = {
                        val newItem = Item(
                            name = newItemName,
                            clientName = newItemClientName,
                            entryDate = newItemEntryDate,
                            returnDate = newItemReturnDate,
                            hasAlarm = newItemHasAlarm,
                            note = newItemNote
                        )
                        viewModel.addItemToSection(shelfId, sectionId, newItem)
                        isAddItemDialogVisible = false
                    },
                    name = newItemName,
                    clientName = newItemClientName,
                    note = newItemNote,
                    hasAlarm = newItemHasAlarm,
                    entryDate = newItemEntryDate,
                    returnDate = newItemReturnDate,
                    onNameChange = { newItemName = it },
                    onClientNameChange = { newItemClientName = it },
                    onNoteChange = { newItemNote = it },
                    onHasAlarmChange = { newItemHasAlarm = it },
                    onEntryDateChange = { newItemEntryDate = it },
                    onReturnDateChange = { newItemReturnDate = it },
                    settings = settings
                )
            }
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    onDeleteClick: () -> Unit,
    settings: Settings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${stringResource(R.string.item_name)}: ${item.name}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${stringResource(R.string.client_name)}: ${item.clientName.ifEmpty { stringResource(R.string.unknown_client) }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.note.isNotEmpty()) {
                        Text(
                            text = "${stringResource(R.string.note)}: ${item.note}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.entry_date)}: ${item.entryDate?.toDisplayFormat(settings) ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${stringResource(R.string.return_date)}: ${item.returnDate?.toDisplayFormat(settings) ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }
            }
        }
    }
}