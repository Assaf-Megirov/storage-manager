package com.awindyendprod.storage_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import com.awindyendprod.storage_manager.services.toDisplayFormat
import com.awindyendprod.storage_manager.services.toShortDisplayFormat
import com.awindyendprod.storage_manager.services.toLongDisplayFormat
import com.awindyendprod.storage_manager.services.PhoneNumberService
import com.awindyendprod.storage_manager.ui.components.PhoneActionMenu
import com.awindyendprod.storage_manager.R
import androidx.compose.ui.res.stringResource
import java.util.*

data class DueItemInfo(
    val item: Item,
    val shelfId: String,
    val sectionId: String,
    val shelfName: String,
    val sectionNumber: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllDueScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onItemClick: (String, String) -> Unit,
    dateIso: String? = null
) {
    val settings by settingsViewModel.settings.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    
    // State for date selection
    var selectedDate by remember(dateIso) {
        mutableStateOf(
            dateIso?.let {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).parse(it)
                } catch (e: Exception) { null }
            } ?: Date()
        )
    }
    var showDateDropdown by remember { mutableStateOf(false) }
    
    // Generate date options (today ±7 days)
    val dateOptions = remember {
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<Date>()
        for (i in -7..7) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, i)
            dates.add(calendar.time.clone() as Date)
        }
        dates
    }
    
    // Filter items by selected date
    val dueItems = remember(shelves, selectedDate) {
        val targetCalendar = Calendar.getInstance().apply { time = selectedDate }
        val items = mutableListOf<DueItemInfo>()
        
        shelves.forEach { shelf ->
            shelf.sections.forEachIndexed { sectionIndex, section ->
                section.items.forEach { item ->
                    item.returnDate?.let { returnDate ->
                        val itemCalendar = Calendar.getInstance().apply { time = returnDate }
                        if (targetCalendar.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR) &&
                            targetCalendar.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)) {
                            items.add(DueItemInfo(item, shelf.id, section.id, shelf.name, sectionIndex + 1))
                        }
                    }
                }
            }
        }
        items
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.items_due),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    val isPastDay = remember(selectedDate) {
                        val today = Calendar.getInstance()
                        val sel = Calendar.getInstance().apply { time = selectedDate }
                        sel.get(Calendar.YEAR) < today.get(Calendar.YEAR) ||
                        (sel.get(Calendar.YEAR) == today.get(Calendar.YEAR) && sel.get(Calendar.DAY_OF_YEAR) < today.get(Calendar.DAY_OF_YEAR))
                    }
                    if (isPastDay) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Date picker dropdown
                    Box {
                        Row(
                            modifier = Modifier
                                .clickable { showDateDropdown = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedDate.toShortDisplayFormat(settings),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.select_date),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showDateDropdown,
                            onDismissRequest = { showDateDropdown = false }
                        ) {
                            dateOptions.forEach { date ->
                                DropdownMenuItem(
                                    text = {
                                        val calendar = Calendar.getInstance().apply { time = date }
                                        val today = Calendar.getInstance()
                                        val label = when {
                                            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> stringResource(R.string.today)
                                            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) - 1 -> stringResource(R.string.yesterday)
                                            calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 -> stringResource(R.string.tomorrow)
                                            else -> date.toLongDisplayFormat(settings)
                                        }
                                        Text(label)
                                    },
                                    onClick = {
                                        selectedDate = date
                                        showDateDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (dueItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            R.string.no_items_due_on,
                            selectedDate.toLongDisplayFormat(settings)
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dueItems) { dueItemInfo ->
                        DueItemCard(
                            dueItemInfo = dueItemInfo,
                            onItemClick = onItemClick,
                            onDeleteItem = { viewModel.removeItemFromSection(dueItemInfo.shelfId, dueItemInfo.sectionId, dueItemInfo.item.id) },
                            settings = settings
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueItemCard(
    dueItemInfo: DueItemInfo,
    onItemClick: (String, String) -> Unit,
    onDeleteItem: () -> Unit,
    settings: com.awindyendprod.storage_manager.model.Settings
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_item)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteItem()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(dueItemInfo.shelfId, dueItemInfo.sectionId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dueItemInfo.item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (dueItemInfo.item.clientName.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = dueItemInfo.item.clientName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        val phone = remember(dueItemInfo.item) {
                            PhoneNumberService.detectPhoneNumber(dueItemInfo.item.clientName, dueItemInfo.item.note)
                        }
                        if (phone != null) {
                            PhoneActionMenu(phone)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.shelf_section_format_simple,
                            stringResource(R.string.shelf_number_format, dueItemInfo.shelfName),
                            stringResource(R.string.section_number_format, dueItemInfo.sectionNumber)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = stringResource(
                            R.string.entry_prefix,
                            dueItemInfo.item.entryDate?.toDisplayFormat(settings) ?: stringResource(R.string.not_available)
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (dueItemInfo.item.hasAlarm) {
                    Text(
                        text = stringResource(R.string.alarm_set),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}