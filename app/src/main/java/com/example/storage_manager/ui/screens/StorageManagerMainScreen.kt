package com.example.storage_manager.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.storage_manager.model.Item
import com.example.storage_manager.model.Shelf
import com.example.storage_manager.model.ShelfSection
import com.example.storage_manager.services.toDisplayFormat
import com.example.storage_manager.viewmodel.StorageTrackerViewModel
import com.example.storage_manager.viewmodel.SettingsViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalConfiguration
import com.example.storage_manager.model.Settings
import com.example.storage_manager.model.SectionDateType
import com.example.storage_manager.model.DateDisplayFormat
import com.example.storage_manager.R
import androidx.compose.ui.res.stringResource
import com.example.storage_manager.model.FontSize
import com.example.storage_manager.ui.components.SideBar
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import android.net.Uri
import com.example.storage_manager.ui.components.ImportConfirmationDialog

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
    var newItemName by remember { mutableStateOf("") }
    var newItemClientName by remember { mutableStateOf("") }
    var newItemNote by remember { mutableStateOf("") }
    var newItemHasAlarm by remember { mutableStateOf(false) }
    var newItemEntryDate by remember { mutableStateOf(Date()) }
    var newItemAlarmDate by remember { mutableStateOf<Date?>(null) }
    
    // Get settings first
    val settings by settingsViewModel.settings.collectAsState()
    
    // Then use settings in the remember block for newItemReturnDate
    var newItemReturnDate by remember(settings.defaultReturnDateDays) { 
        mutableStateOf(
            Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
        )
    }

    Row {
        if (isLandscape()) {
            SideBar(
                onSearchClick = onSearchClick,
                onSettingsClick = onSettingsClick,
                isEditMode = isEditMode,
                onEditModeToggle = { isEditMode = !isEditMode }
            )
        }

        Scaffold(
            topBar = if (!isLandscape()) {
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
            Box(modifier = Modifier.padding(padding)) {
                val shelves by viewModel.shelves.collectAsState()
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

                // Show Add Item Dialog
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
            }
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Update dateText whenever selectedDate changes
    LaunchedEffect(selectedDate) {
        dateText = dateFormatter.format(selectedDate)
    }

    // DatePickerDialog
    if (showDatePicker) {
        val currentCalendar = Calendar.getInstance().apply { time = selectedDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.time = selectedDate  // Start with current selected date
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                showDatePicker = false
                showTimePicker = true
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // TimePickerDialog
    if (showTimePicker) {
        val currentCalendar = Calendar.getInstance().apply { time = selectedDate }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                onDateChange(calendar.time)  // Update the parent with new date
                showTimePicker = false
            },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = { },
        label = { Text(label) },
        readOnly = true,
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            ) {
                showDatePicker = true
            }
    )
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
        // Top row for section number
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

        // Calculate available space for items
        val itemHeight = when (settings.fontSize) {
            FontSize.SMALL -> 36.dp
            FontSize.MEDIUM -> 42.dp
            FontSize.LARGE -> 48.dp
        }

        // Subtract top and bottom areas from available space, accounting for edit mode button
        val bottomPadding = if (isEditMode) 56.dp else 10.dp // Increased space when in edit mode
        val availableSpace = (settings.sectionHeight.dp - (4.dp + bottomPadding)) // 24.dp for top header
        val maxVisibleItems = (availableSpace.value / itemHeight.value).toInt()

        // Middle section for items
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

        // Bottom row for add and delete buttons (if in edit mode)
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

@Composable
fun StorageManagerApp(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    importUri: Uri? = null
) {
    val navController = rememberNavController()
    var showImportDialog by remember { mutableStateOf(false) }
    var handledUri by remember { mutableStateOf<Uri?>(null) }
    
    // Show import dialog if URI is provided and hasn't been handled
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
        // Main screen composable
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

        // Section details screen
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
                onBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // Search screen
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
    }
}