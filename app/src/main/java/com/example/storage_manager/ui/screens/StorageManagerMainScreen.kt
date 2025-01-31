package com.example.storage_manager.ui.screens

import AddItemDialog
import SectionDetailsScreen
import android.app.TimePickerDialog
import android.app.DatePickerDialog
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
import com.example.storage_manager.model.Settings
import com.example.storage_manager.model.SectionDateType
import com.example.storage_manager.model.DateDisplayFormat
import com.example.storage_manager.R
import androidx.compose.ui.res.stringResource
import com.example.storage_manager.model.FontSize

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
    
    // Get settings first
    val settings by settingsViewModel.settings.collectAsState()
    
    // Then use settings in the remember block for newItemReturnDate
    var newItemReturnDate by remember(settings.defaultReturnDateDays) { 
        mutableStateOf(
            Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    IconButton(onClick = { isEditMode = !isEditMode }) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_mode)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isEditMode) {
                val shelfText = stringResource(R.string.shelf)
                FloatingActionButton(
                    onClick = { viewModel.addShelf(shelfText) }
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

//@Composable
//fun AddItemDialog(
//    onDismiss: () -> Unit,
//    onAddItem: () -> Unit,
//    name: String,
//    clientName: String,
//    note: String,
//    hasAlarm: Boolean,
//    entryDate: Date,
//    returnDate: Date,
//    onNameChange: (String) -> Unit,
//    onClientNameChange: (String) -> Unit,
//    onNoteChange: (String) -> Unit,
//    onHasAlarmChange: (Boolean) -> Unit,
//    onEntryDateChange: (Date) -> Unit,
//    onReturnDateChange: (Date) -> Unit,
//    settings: Settings
//) {
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text(stringResource(R.string.add_item)) },
//        text = {
//            Column {
//                OutlinedTextField(
//                    value = name,
//                    onValueChange = onNameChange,
//                    label = { Text(stringResource(R.string.item_name)) },
//                    trailingIcon = {
//                        if (name.isNotEmpty()) {
//                            IconButton(onClick = { onNameChange("") }) {
//                                Icon(Icons.Default.Close,
//                                    contentDescription = stringResource(R.string.cancel))
//                            }
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                OutlinedTextField(
//                    value = clientName,
//                    onValueChange = onClientNameChange,
//                    label = { Text(stringResource(R.string.client_name)) },
//                    trailingIcon = {
//                        if (clientName.isNotEmpty()) {
//                            IconButton(onClick = { onClientNameChange("") }) {
//                                Icon(Icons.Default.Close,
//                                    contentDescription = stringResource(R.string.cancel))
//                            }
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                OutlinedTextField(
//                    value = note,
//                    onValueChange = onNoteChange,
//                    label = { Text(stringResource(R.string.note)) },
//                    trailingIcon = {
//                        if (note.isNotEmpty()) {
//                            IconButton(onClick = { onNoteChange("") }) {
//                                Icon(Icons.Default.Close,
//                                    contentDescription = stringResource(R.string.cancel))
//                            }
//                        }
//                    },
//                    modifier = Modifier.fillMaxWidth()
//                )
//                Row {
//                    Text(stringResource(R.string.has_alarm))
//                    Checkbox(
//                        checked = hasAlarm,
//                        onCheckedChange = onHasAlarmChange
//                    )
//                }
//                DatePickerField(
//                    label = stringResource(R.string.entry_date),
//                    selectedDate = entryDate,
//                    onDateChange = onEntryDateChange
//                )
//                DatePickerField(
//                    label = stringResource(R.string.return_date),
//                    selectedDate = returnDate,
//                    onDateChange = onReturnDateChange
//                )
//            }
//        },
//        confirmButton = {
//            TextButton(onClick = onAddItem) {
//                Text(stringResource(R.string.add))
//            }
//        },
//        dismissButton = {
//            TextButton(onClick = onDismiss) {
//                Text(stringResource(R.string.cancel))
//            }
//        }
//    )
//}

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
    settings: Settings
) {
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
            Column {
                if (isEditMode) {
                    IconButton(onClick = { onAddSection(shelf.id) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Section")
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
                                settings = settings
                            )
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
    settings: Settings
) {
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
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // Calculate available space for items
        val itemHeight = when (settings.fontSize) {
            FontSize.SMALL -> 36.dp
            FontSize.MEDIUM -> 42.dp
            FontSize.LARGE -> 48.dp
        }

        // Subtract top and bottom areas from available space
        val availableSpace = (settings.sectionHeight.dp - 40.dp) // Reduced from 80.dp to 40.dp for more space
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
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = item.clientName.ifEmpty { stringResource(R.string.unknown_client) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = when (settings.sectionDateType) {
                            SectionDateType.ENTRY_DATE -> item.entryDate
                            SectionDateType.RETURN_DATE -> item.returnDate
                        }?.toDisplayFormat(settings) ?: "",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (section.items.size > maxVisibleItems) {
                Text(
                    text = stringResource(R.string.more_items, section.items.size - maxVisibleItems),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Bottom row for add button (if in edit mode)
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onAddItem,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                }
            }
        }
    }
}

@Composable
fun StorageManagerApp(viewModel: StorageTrackerViewModel, settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()

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

//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SectionDetailsScreen(
//    viewModel: StorageTrackerViewModel,
//    settingsViewModel: SettingsViewModel,
//    shelfId: String,
//    sectionId: String,
//    onBack: () -> Unit
//) {
//    // Get settings first
//    val settings by settingsViewModel.settings.collectAsState()
//
//    // Then the rest of your state variables
//    val section by viewModel.getSectionById(shelfId, sectionId).collectAsState()
//    var isAddItemDialogVisible by remember { mutableStateOf(false) }
//    var newItemName by remember { mutableStateOf("") }
//    var newItemClientName by remember { mutableStateOf("") }
//    var newItemNote by remember { mutableStateOf("") }
//    var newItemHasAlarm by remember { mutableStateOf(false) }
//    var newItemEntryDate by remember { mutableStateOf(Date()) }
//    var newItemReturnDate by remember(settings.defaultReturnDateDays) {
//        mutableStateOf(
//            Date(System.currentTimeMillis() + (settings.defaultReturnDateDays * 24 * 60 * 60 * 1000L))
//        )
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text(text = "Section Details") },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = {
//                // Open Add Item Dialog
//                isAddItemDialogVisible = true
//            }) {
//                Icon(Icons.Default.Add, contentDescription = "Add Item")
//            }
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//        ) {
//            // Title for the section items list
//            Text(
//                text = "Items in Section",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(16.dp)
//            )
//
//            // Display the list of items using LazyColumn
//            LazyColumn(
//                modifier = Modifier.fillMaxSize()
//            ) {
//                section?.let {
//                    items(it.items) { item ->
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 16.dp, vertical = 8.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            // Main Content (Name and Client Name)
//                            Column(
//                                modifier = Modifier.weight(1f) // Takes up remaining space
//                            ) {
//                                // Item Name
//                                Text(
//                                    text = item.name,
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    maxLines = 1,
//                                    overflow = TextOverflow.Ellipsis
//                                )
//
//                                // Client Name
//                                Text(
//                                    text = item.clientName ?: "Unknown Client",
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//
//                            // Dates and Delete Button in a Row
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between dates and delete button
//                            ) {
//                                // Dates Container
//                                Column(
//                                    horizontalAlignment = Alignment.End
//                                ) {
//                                    // Return Date
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically,
//                                        modifier = Modifier.padding(bottom = 4.dp) // Space between return and entry dates
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.ExitToApp,
//                                            contentDescription = "Return Date",
//                                            modifier = Modifier.size(16.dp),
//                                            tint = MaterialTheme.colorScheme.primary
//                                        )
//                                        Spacer(modifier = Modifier.width(4.dp))
//                                        Text(
//                                            text = item.entryDate?.toDisplayFormat() ?: "N/A",
//                                            style = MaterialTheme.typography.bodySmall
//                                        )
//                                    }
//
//                                    // Entry Date
//                                    Row(
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Icon(
//                                            imageVector = Icons.Default.ArrowBack,
//                                            contentDescription = "Entry Date",
//                                            modifier = Modifier.size(16.dp),
//                                            tint = MaterialTheme.colorScheme.primary
//                                        )
//                                        Spacer(modifier = Modifier.width(4.dp))
//                                        Text(
//                                            text = item.returnDate?.toDisplayFormat() ?: "N/A",
//                                            style = MaterialTheme.typography.bodySmall
//                                        )
//                                    }
//                                }
//
//                                // Delete Button
//                                IconButton(
//                                    onClick = {
//                                        viewModel.removeItemFromSection(shelfId, sectionId, item.id)
//                                    }
//                                ) {
//                                    Icon(Icons.Default.Delete, contentDescription = "Remove Item")
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            if (isAddItemDialogVisible) {
//                AddItemDialog(
//                    onDismiss = { isAddItemDialogVisible = false },
//                    onAddItem = {
//                        val newItem = Item(
//                            name = newItemName,
//                            clientName = newItemClientName,
//                            entryDate = newItemEntryDate,
//                            returnDate = newItemReturnDate,
//                            hasAlarm = newItemHasAlarm,
//                            note = newItemNote
//                        )
//                        viewModel.addItemToSection(shelfId, sectionId, newItem)
//                        isAddItemDialogVisible = false // Close dialog after adding the item
//                    },
//                    name = newItemName,
//                    clientName = newItemClientName,
//                    note = newItemNote,
//                    hasAlarm = newItemHasAlarm,
//                    entryDate = newItemEntryDate,
//                    returnDate = newItemReturnDate,
//                    onNameChange = { newItemName = it },
//                    onClientNameChange = { newItemClientName = it },
//                    onNoteChange = { newItemNote = it },
//                    onHasAlarmChange = { newItemHasAlarm = it },
//                    onEntryDateChange = { newItemEntryDate = it },
//                    onReturnDateChange = { newItemReturnDate = it },
//                    settings = settings
//                )
//            }
//        }
//    }
//}

