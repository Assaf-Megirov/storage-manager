package com.awindyendprod.storage_manager.ui.screens

import AddItemDialog
import SectionDetailsScreen
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.ShelfSection
import com.awindyendprod.storage_manager.services.toDisplayFormat
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.SectionDateType
import com.awindyendprod.storage_manager.R
import androidx.compose.ui.res.stringResource
import com.awindyendprod.storage_manager.model.FontSize
import com.awindyendprod.storage_manager.ui.components.SideBar
import android.net.Uri
import androidx.compose.foundation.border
import com.awindyendprod.storage_manager.ui.components.ImportConfirmationDialog
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Log.d("OrientationCheck", "isLandscape: $isLandscape")
    return isLandscape
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerMainScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    onSectionClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var isEditMode by remember { mutableStateOf(false) }
    var selectedShelf by remember { mutableStateOf<Shelf?>(null) }
    var selectedShelfId by remember { mutableStateOf<String?>(null) }
    var selectedSectionId by remember { mutableStateOf<String?>(null) }
    var isAddItemDialogVisible by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemClientName by remember { mutableStateOf("") }
    var newItemNote by remember { mutableStateOf("") }
    var newItemHasAlarm by remember { mutableStateOf(false) }
    var newItemEntryDate by remember { mutableStateOf(Date()) }
    var newItemAlarmDate by remember { mutableStateOf<Date?>(null) }
    
    val settings by settingsViewModel.settings.collectAsState()
    val isLandscape = isLandscape()

    var newItemReturnDate by remember(settings.defaultReturnDateDays) { 
        mutableStateOf(
            Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
        )
    }

    Row {
        if (isLandscape) {
            SideBar(
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode },
                onHelpClick = { showHelpDialog = true }
            )
        }

        Scaffold(
            topBar = if (!isLandscape) {
                {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        actions = {
                            IconButton(onClick = onSearchClick) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                            IconButton(onClick = onSettingsClick) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.settings)
                                )
                            }
                            IconButton(onClick = { showHelpDialog = true }) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = stringResource(R.string.help)
                                )
                            }
                            IconButton(onClick = { isEditMode = !isEditMode }) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_mode)
                                )
                            }
                        }
                    )
                }
            } else {
                {}
            },
            floatingActionButton = {
                if (isEditMode) {
                    FloatingActionButton(
                        onClick = { viewModel.addShelf() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_shelf))
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                val shelves by viewModel.shelves.collectAsState()
                
                if (shelves.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_shelves_message),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    EmptyShelvesHint(isLandscape = isLandscape)

                } else {
                    ShelvesScrollableView(
                        shelves = shelves,
                        isEditMode = isEditMode,
                        onShelfSelect = { shelf -> selectedShelf = shelf },
                        onAddSection = { shelfId -> viewModel.addSectionToShelf(shelfId, "New Section") },
                        onAddItem = { shelfId, sectionId ->
                            selectedShelfId = shelfId
                            selectedSectionId = sectionId
                            isAddItemDialogVisible = true
                        },
                        onSectionClick = onSectionClick,
                        onRemoveShelf = { shelfId -> viewModel.removeShelf(shelfId) },
                        onRemoveSection = { shelfId, sectionId -> viewModel.removeSection(shelfId, sectionId) },
                        settings = settings
                    )
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
                            selectedShelfId?.let { shelfId ->
                                selectedSectionId?.let { sectionId ->
                                    viewModel.addItemToSection(shelfId, sectionId, newItem)
                                }
                            }
                            isAddItemDialogVisible = false
                            newItemName = ""
                            newItemClientName = ""
                            newItemNote = ""
                            newItemHasAlarm = false
                            newItemEntryDate = Date()
                            newItemReturnDate = Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
                            newItemAlarmDate = null
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
                        settings = settings
                    )
                }

                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        title = { Text(stringResource(R.string.help)) },
                        text = { Text(stringResource(R.string.help_message)) },
                        confirmButton = {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyShelvesHint(isLandscape: Boolean) {
    val hintAlignment = if (isLandscape) Alignment.TopStart else Alignment.TopEnd
    val hintPadding = 16.dp
    val hintIcon = if (isLandscape) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowUp
    val hintTextRes = R.string.click_edit_to_add

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(hintPadding),
        contentAlignment = hintAlignment
    ) {
        Column(
            horizontalAlignment = if (isLandscape) Alignment.Start else Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = hintIcon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(hintTextRes),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: Date,
    onDateChange: (Date) -> Unit,
    settings: Settings
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val calendar = remember { Calendar.getInstance() }

    var dateText by remember { mutableStateOf(dateFormatter.format(selectedDate)) }
    var showPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        dateText = dateFormatter.format(selectedDate)
    }

    if (showPicker) {
        val currentCalendar = Calendar.getInstance().apply { time = selectedDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.time = selectedDate
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        onDateChange(calendar.time)
                        showPicker = false
                    },
                    currentCalendar.get(Calendar.HOUR_OF_DAY),
                    currentCalendar.get(Calendar.MINUTE),
                    true
                ).show()
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
        showPicker = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ) {
                showPicker = true
            }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun ShelvesScrollableView(
    shelves: List<Shelf>,
    isEditMode: Boolean,
    onShelfSelect: (Shelf) -> Unit,
    onAddSection: (String) -> Unit,
    onAddItem: (String, String) -> Unit,
    onSectionClick: (String, String) -> Unit,
    onRemoveShelf: (String) -> Unit,
    onRemoveSection: (String, String) -> Unit,
    settings: Settings
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        shelves.forEach { shelf ->
            ShelfView(
                shelf = shelf,
                isEditMode = isEditMode,
                onShelfSelect = onShelfSelect,
                onAddSection = onAddSection,
                onAddItem = onAddItem,
                onSectionClick = onSectionClick,
                onRemoveShelf = onRemoveShelf,
                onRemoveSection = onRemoveSection,
                settings = settings
            )
        }
    }
}

@Composable
fun ShelfView(
    shelf: Shelf,
    isEditMode: Boolean,
    onShelfSelect: (Shelf) -> Unit,
    onAddSection: (String) -> Unit,
    onAddItem: (String, String) -> Unit,
    onSectionClick: (String, String) -> Unit,
    onRemoveShelf: (String) -> Unit,
    onRemoveSection: (String, String) -> Unit,
    settings: Settings
) {
    var showDeleteShelfDialog by remember { mutableStateOf(false) }

    if (showDeleteShelfDialog) {
        if(shelf.sections.isNotEmpty()){
            AlertDialog(
                onDismissRequest = { showDeleteShelfDialog = false },
                title = { Text(stringResource(R.string.confirm_delete)) },
                text = {
                    if (shelf.sections.any { it.items.isNotEmpty() }) {
                        Text(stringResource(R.string.shelf_not_empty_warning))
                    } else {
                        Text(stringResource(R.string.confirm_delete_shelf))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onRemoveShelf(shelf.id)
                        showDeleteShelfDialog = false
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteShelfDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }else{
            onRemoveShelf(shelf.id)
            showDeleteShelfDialog = false
        }
    }

    val shelfColor = Color.hsl(
        hue = 66f,
        saturation = 0.33f,
        lightness = 0.78f,
        alpha = 1f,
        colorSpace = ColorSpaces.Srgb
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shelfColor)
        ) {
            Row {
                Text(
                    text = shelf.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(8.dp, 8.dp, 4.dp, 4.dp)
                )
                Column {
                    if (isEditMode) {
                        if (shelf.sections.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .padding(top = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                IconButton(
                                    onClick = { onAddSection(shelf.id) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Section",
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showDeleteShelfDialog = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .align(Alignment.CenterEnd)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Shelf")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { onAddSection(shelf.id) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Section")
                                }
                                IconButton(onClick = { showDeleteShelfDialog = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Shelf")
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        shelf.sections.forEachIndexed { index, section ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(settings.sectionHeight.dp)
                            ) {
                                SectionView(
                                    section = section,
                                    sectionNumber = index,
                                    isEditMode = isEditMode,
                                    onAddItem = { onAddItem(shelf.id, section.id) },
                                    onSectionClick = { onSectionClick(shelf.id, section.id) },
                                    onRemoveSection = { onRemoveSection(shelf.id, section.id) },
                                    settings = settings
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun SectionView(
    section: ShelfSection,
    sectionNumber: Int,
    isEditMode: Boolean,
    onAddItem: () -> Unit,
    onSectionClick: () -> Unit,
    onRemoveSection: () -> Unit,
    settings: Settings
) {
    var showDeleteSectionDialog by remember { mutableStateOf(false) }

    if (showDeleteSectionDialog) {
        if (section.items.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showDeleteSectionDialog = false },
                title = { Text(stringResource(R.string.confirm_delete)) },
                text = {
                    if (section.items.isNotEmpty()) {
                        Text(stringResource(R.string.section_not_empty_warning))
                    } else {
                        Text(stringResource(R.string.confirm_delete_section))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        onRemoveSection()
                        showDeleteSectionDialog = false
                    }) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSectionDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }else{
            onRemoveSection()
            showDeleteSectionDialog = false
        }
    }

    Column(
        modifier = Modifier
            .width(settings.sectionWidth.dp)
            .height(settings.sectionHeight.dp)
            .padding(4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onSectionClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = (sectionNumber + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        val itemHeight = when (settings.fontSize) {
            FontSize.SMALL -> 36.dp
            FontSize.MEDIUM -> 42.dp
            FontSize.LARGE -> 48.dp
        }

        val bottomPadding = if (isEditMode) 56.dp else 10.dp
        val availableSpace = (settings.sectionHeight.dp - (4.dp + bottomPadding))
        val maxVisibleItems = (availableSpace.value / itemHeight.value).toInt()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            section.items.take(maxVisibleItems).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                        Text(
                            text = item.clientName.ifEmpty { stringResource(R.string.unknown_client) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Text(
                        text = when (settings.sectionDateType) {
                            SectionDateType.ENTRY_DATE -> item.entryDate
                            SectionDateType.RETURN_DATE -> item.returnDate
                        }?.toDisplayFormat(settings) ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (section.items.size > maxVisibleItems) {
                Text(
                    text = stringResource(R.string.more_items, section.items.size - maxVisibleItems),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        if (isEditMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { showDeleteSectionDialog = true }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove Section")
                }
                IconButton(
                    onClick = onAddItem
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    shelfId: String,
    sectionId: String,
    itemId: String,
    onBack: () -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val item by remember(shelfId, sectionId, itemId) {
        viewModel.getItem(shelfId, sectionId, itemId)
    }.collectAsState(initial = null)

    var editedName by remember { mutableStateOf("") }
    var editedClientName by remember { mutableStateOf("") }
    var editedNote by remember { mutableStateOf("") }
    var editedHasAlarm by remember { mutableStateOf(false) }
    var editedEntryDate by remember { mutableStateOf(Date()) }
    var editedReturnDate by remember { mutableStateOf(Date()) }
    var editedAlarmDate by remember { mutableStateOf<Date?>(null) }

    LaunchedEffect(item) {
        item?.let {
            editedName = it.name
            editedClientName = it.clientName
            editedNote = it.note
            editedHasAlarm = it.hasAlarm
            editedEntryDate = it.entryDate!!
            editedReturnDate = it.returnDate!!
            editedAlarmDate = it.alarmDate
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_item)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            item?.let {
                                viewModel.updateItem(
                                    shelfId,
                                    sectionId,
                                    itemId,
                                    Item(
                                        id = itemId,
                                        name = editedName,
                                        clientName = editedClientName,
                                        note = editedNote,
                                        hasAlarm = editedHasAlarm,
                                        entryDate = editedEntryDate,
                                        returnDate = editedReturnDate,
                                        alarmDate = editedAlarmDate
                                    )
                                )
                                onBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(R.string.save))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text(stringResource(R.string.item_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editedClientName,
                onValueChange = { editedClientName = it },
                label = { Text(stringResource(R.string.client_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editedNote,
                onValueChange = { editedNote = it },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.has_alarm))
                Switch(
                    checked = editedHasAlarm,
                    onCheckedChange = { editedHasAlarm = it }
                )
            }

            DatePickerField(
                label = stringResource(R.string.entry_date),
                selectedDate = editedEntryDate,
                onDateChange = { editedEntryDate = it },
                settings = settings
            )

            DatePickerField(
                label = stringResource(R.string.return_date),
                selectedDate = editedReturnDate,
                onDateChange = { editedReturnDate = it },
                settings = settings
            )

            if (editedHasAlarm) {
                DatePickerField(
                    label = stringResource(R.string.alarm_date),
                    selectedDate = editedAlarmDate ?: editedReturnDate,
                    onDateChange = { editedAlarmDate = it },
                    settings = settings
                )
            }
        }
    }
}

@Composable
fun StorageManagerApp(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    importUri: Uri? = null
) {
    val navController = rememberNavController()
    var showImportDialog by remember { mutableStateOf(false) }
    var handledUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(importUri) {
        if (importUri != null && importUri != handledUri) {
            showImportDialog = true
            handledUri = importUri
        }
    }

    if (showImportDialog) {
        ImportConfirmationDialog(
            onConfirm = {
                importUri?.let { settingsViewModel.importData(it) }
                showImportDialog = false
            },
            onDismiss = {
                showImportDialog = false
            }
        )
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            StorageManagerMainScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onSectionClick = { shelfId, sectionId ->
                    navController.navigate("section_details/$shelfId/$sectionId")
                },
                onSearchClick = {
                    navController.navigate("search")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "section_details/{shelfId}/{sectionId}",
            arguments = listOf(
                navArgument("shelfId") { type = NavType.StringType },
                navArgument("sectionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            SectionDetailsScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                shelfId = backStackEntry.arguments?.getString("shelfId") ?: "",
                sectionId = backStackEntry.arguments?.getString("sectionId") ?: "",
                onBack = { navController.popBackStack() },
                onEditItem = { shelfId, sectionId, itemId ->
                    navController.navigate("edit_item/$shelfId/$sectionId/$itemId")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onItemClick = { shelfId, sectionId ->
                    navController.navigate("section_details/$shelfId/$sectionId")
                }
            )
        }

        composable(
            route = "edit_item/{shelfId}/{sectionId}/{itemId}",
            arguments = listOf(
                navArgument("shelfId") { type = NavType.StringType },
                navArgument("sectionId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditItemScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                shelfId = backStackEntry.arguments?.getString("shelfId") ?: "",
                sectionId = backStackEntry.arguments?.getString("sectionId") ?: "",
                itemId = backStackEntry.arguments?.getString("itemId") ?: "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}