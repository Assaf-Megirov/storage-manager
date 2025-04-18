import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.FontSize
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.services.toDisplayFormat
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SectionDetailsScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    shelfId: String,
    sectionId: String,
    onBack: () -> Unit,
    onEditItem: (String, String, String) -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val section by viewModel.getSectionById(shelfId, sectionId).collectAsState()
    
    val detailsDateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val iconSize = when (settings.fontSize) {
        FontSize.SMALL -> 14.dp
        FontSize.MEDIUM -> 16.dp
        FontSize.LARGE -> 18.dp
    }

    val itemSpacing = when (settings.fontSize) {
        FontSize.SMALL -> 2.dp
        FontSize.MEDIUM -> 6.dp
        FontSize.LARGE -> 10.dp
    }

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
    var newItemAlarmDate by remember { mutableStateOf<Date?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<Item?>(null) }
    var selectedShelfId by remember { mutableStateOf(shelfId) }
    var selectedSectionId by remember { mutableStateOf(sectionId) }
    val shelves by viewModel.shelves.collectAsState()

    //selection states
    var selectedItems by remember { mutableStateOf<List<Item>>(emptyList()) }
    var selectionMode by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopAppBar(
                    selectedCount = selectedItems.size,
                    sectionItemsSize = section?.items?.size ?: 0,
                    onCancelClick = {
                        selectionMode = false
                        selectedItems = emptyList()
                    },
                    onSelectAllClick = {
                        if (selectedItems.size == (section?.items?.size ?: false)) {
                            selectedItems = emptyList() //if all selected, deselect all
                        }else{
                            selectedItems = section?.items ?: emptyList()
                        }
                    },
                    onMoveClick = {
                        //TODO: show move dialog
                        showMoveDialog = true
                    },
                    onDeleteClick = {
                        if(selectedItems.isNotEmpty()){
                            showDeleteConfirmation = true
                        }
                    }
                )
            }else{
                TopAppBar(
                    title = { Text(stringResource(R.string.section_details)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
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
            if(!selectionMode){
                Text(
                    text = stringResource(R.string.long_press_hint),
                    style = MaterialTheme.typography.bodySmall, // smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // grayish color
                    modifier = Modifier.align(Alignment.CenterHorizontally) // center horizontally inside a Column/Box
                )
            }

            LazyColumn(

            ) {
                section?.let {
                    items(it.items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = itemSpacing)
                                .combinedClickable (
                                    onClick = {
                                        if (selectionMode) {
                                            selectedItems = selectedItems.toMutableList().apply {
                                                if (contains(item)) remove(item) else add(item)
                                            }
                                        } else {
                                            onEditItem(shelfId, sectionId, item.id)
                                        }
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selectedItems = listOf(item)
                                    }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectionMode){
                                Checkbox(
                                    checked = selectedItems.contains(item),
                                    onCheckedChange = {
                                        selectedItems = selectedItems.toMutableList().apply {
                                            if (contains(item)) remove(item) else add(item)
                                        }
                                    }
                                )
                            }
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
                                            modifier = Modifier.size(iconSize),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.entryDate?.toDisplayFormat(settings) ?: stringResource(R.string.entry_date_na),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = stringResource(R.string.return_date),
                                            modifier = Modifier.size(iconSize),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.returnDate?.toDisplayFormat(settings) ?: stringResource(R.string.return_date_na),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if(!selectionMode){ //if in selection ignore this button
                                        selectedItems = listOf(item)
                                        showMoveDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CompareArrows,
                                    contentDescription = stringResource(R.string.move),
                                    modifier = Modifier.size(iconSize*2)
                                )
                            }
                            IconButton(
                                onClick = {
                                    itemToDelete = item
                                    showDeleteConfirmation = true
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    modifier = Modifier.size(iconSize*2)
                                )
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
                            alarmDate = newItemAlarmDate,
                            note = newItemNote
                        )
                        selectedShelfId.let { shelfId ->
                            selectedSectionId.let { sectionId ->
                                viewModel.addItemToSection(shelfId, sectionId, newItem)
                            }
                        }
                        isAddItemDialogVisible = false
                    },
                    name = newItemName,
                    clientName = newItemClientName,
                    note = newItemNote,
                    hasAlarm = newItemHasAlarm,
                    entryDate = newItemEntryDate,
                    returnDate = newItemReturnDate,
                    alarmDate = newItemAlarmDate,
                    onNameChange = { newItemName = it },
                    onClientNameChange = { newItemClientName = it },
                    onNoteChange = { newItemNote = it },
                    onHasAlarmChange = { newItemHasAlarm = it },
                    onEntryDateChange = { newItemEntryDate = it },
                    onReturnDateChange = { newItemReturnDate = it },
                    onAlarmDateChange = { newItemAlarmDate = it },
                    onSelectedShelfIdChange = { selectedShelfId = it },
                    onSelectedSectionIdChange = { selectedSectionId = it },
                    newSelectedShelfId = selectedShelfId,
                    newSelectedSectionId = selectedSectionId,
                    shelves = shelves,
                    settings = settings
                )
            }

            if (showDeleteConfirmation && itemToDelete != null) { //regular delete dialog
                AlertDialog(
                    onDismissRequest = { 
                        showDeleteConfirmation = false
                        itemToDelete = null
                    },
                    title = { Text(stringResource(R.string.confirm_delete)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.confirm_delete_item))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.item_not_empty_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                itemToDelete?.let { item ->
                                    viewModel.removeItemFromSection(shelfId, sectionId, item.id)
                                }
                                showDeleteConfirmation = false
                                itemToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                showDeleteConfirmation = false
                                itemToDelete = null
                            }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            if (showDeleteConfirmation && selectedItems.isNotEmpty() && itemToDelete == null){ //bulk delete dialog
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(stringResource(R.string.confirm_delete)) },
                    text = {
                        Column {
                            Text(stringResource(R.string.confirm_delete_bulk_items))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.going_to_delete_items, selectedItems.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                selectedItems.forEach { item ->
                                    viewModel.removeItemFromSection(shelfId, sectionId, item.id)
                                }
                                showDeleteConfirmation = false
                                selectedItems = emptyList()
                            },
                            enabled = selectedItems.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirmation = false }
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            if(showMoveDialog){
                MoveDialog(
                    shelves = shelves,
                    selectedCount = selectedItems.size,
                    onDismiss = { showMoveDialog = false },
                    onConfirm = { newShelfId, newSectionId ->
                        selectedItems.forEach{item -> viewModel.updateItem(newShelfId, newSectionId, item.id, item)}
                        showMoveDialog = false
                        selectedItems = emptyList()
                    },
                    initialShelfId = shelfId,
                    initialSectionId = sectionId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopAppBar(
    selectedCount: Int,
    sectionItemsSize: Int,
    onCancelClick: () -> Unit,
    onSelectAllClick: () -> Unit,
    onMoveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = { Text("$selectedCount "+stringResource(R.string.items_selected)) },
        navigationIcon = {
            IconButton(onClick = onCancelClick) {
                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.cancel_selection))
            }
        },
        actions = {//select all
            val actionName = if (selectedCount == sectionItemsSize) stringResource(R.string.deselect_all) else stringResource(R.string.select_all)
            Text(actionName, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                checked = selectedCount == sectionItemsSize,
                onCheckedChange = { onSelectAllClick() }
            )
            IconButton(onClick = onMoveClick, enabled = selectedCount > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.move))
            }
            IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors( //use secondary colors to differentiate from regular top app bar
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Composable
fun MoveDialog (
    shelves: List<Shelf>,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (newShelfId: String, newSectionId: String) -> Unit,
    modifier: Modifier = Modifier,
    initialShelfId: String = shelves.firstOrNull()?.id ?: "",
    initialSectionId: String = shelves.firstOrNull()?.sections?.firstOrNull()?.id ?: ""
){
    var internalSelectedSectionId by remember { mutableStateOf<String?>(initialSectionId) }
    var internalSelectedShelfId by remember { mutableStateOf<String?>(initialShelfId) }
    AlertDialog(
        modifier = modifier,
        title = { Text(stringResource(R.string.move_item)) },
        text = {
            Column {
                Text(stringResource(R.string.move_warning, selectedCount))
                Spacer(modifier = Modifier.height(8.dp))
                ShelfDropdown(
                    shelves = shelves,
                    selectedShelfId = internalSelectedShelfId ?: "",
                    onShelfSelected = { newShelfId ->
                        internalSelectedShelfId = newShelfId
                        internalSelectedSectionId = shelves //when changing shelves we need to change the section to a one that exists in the new shelf
                            .firstOrNull { it.id == newShelfId }
                            ?.sections
                            ?.firstOrNull()
                            ?.id
                            ?: ""
                    }
                )
                SectionDropdown(
                    sections = internalSelectedShelfId?.let { shelfId -> shelves.find { it.id == shelfId }?.sections } ?: emptyList(),
                    selectedSectionId = internalSelectedSectionId ?: "",
                    onSectionSelected = {selectedSectionId -> internalSelectedSectionId = selectedSectionId}
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {onConfirm(internalSelectedShelfId ?: "", internalSelectedSectionId ?: "")},
                enabled = !internalSelectedShelfId.isNullOrBlank() &&
                        !internalSelectedSectionId.isNullOrBlank()
            ) {
                Text(stringResource(R.string.move))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}