package com.example.storage_manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.storage_manager.R
import com.example.storage_manager.model.Item
import com.example.storage_manager.services.toDisplayFormat
import com.example.storage_manager.viewmodel.StorageTrackerViewModel
import com.example.storage_manager.viewmodel.SettingsViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height

// Add this enum at the top level with SortOrder
enum class SearchType {
    ITEM_NAME,
    CLIENT_NAME
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onItemClick: (String, String) -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    var searchQuery by remember { mutableStateOf<String>("") }
    var sortOrder by remember { mutableStateOf<SortOrder>(SortOrder.NAME_ASC) }
    var searchType by remember { mutableStateOf<SearchType>(SearchType.ITEM_NAME) }

    val allItems = remember { mutableStateOf<List<SearchItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.shelves.collect { shelves ->
            val items = mutableListOf<SearchItem>()
            shelves.forEach { shelf ->
                shelf.sections.forEach { section ->
                    section.items.forEach { item ->
                        items.add(
                            SearchItem(
                                item = item,
                                shelfId = shelf.id,
                                sectionId = section.id,
                                shelfName = shelf.name,
                                sectionNumber = shelf.sections.indexOf(section) + 1
                            )
                        )
                    }
                }
            }
            allItems.value = items
        }
    }

    val filteredAndSortedItems = remember(searchQuery, sortOrder, allItems.value, searchType) {
        allItems.value
            .filter { searchItem ->
                when (searchType) {
                    SearchType.ITEM_NAME -> 
                        searchQuery.isEmpty() || searchItem.item.name.contains(searchQuery, ignoreCase = true)
                    SearchType.CLIENT_NAME -> 
                        searchQuery.isEmpty() || searchItem.item.clientName?.contains(searchQuery, ignoreCase = true) == true
                }
            }
            .sortedWith(when (sortOrder) {
                SortOrder.NAME_ASC -> compareBy { it.item.name }
                SortOrder.NAME_DESC -> compareByDescending { it.item.name }
                SortOrder.ENTRY_DATE_ASC -> compareBy { it.item.entryDate }
                SortOrder.ENTRY_DATE_DESC -> compareByDescending { it.item.entryDate }
                SortOrder.RETURN_DATE_ASC -> compareBy { it.item.returnDate }
                SortOrder.RETURN_DATE_DESC -> compareByDescending { it.item.returnDate }
            })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.back), style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                modifier = Modifier.height(48.dp)
            )
        }
    ) { padding ->
        if (isLandscape()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    // Search options (filters, sort options, etc.)
                    SearchOptions(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchType = searchType,
                        onSearchTypeChange = { searchType = it },
                        sortOrder = sortOrder,
                        onSortOrderChange = { sortOrder = it }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(filteredAndSortedItems) { searchItem ->
                        SearchItemCard(
                            searchItem = searchItem,
                            onClick = { onItemClick(searchItem.shelfId, searchItem.sectionId) },
                            settings = settings
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search options and results in a single column for portrait mode
                SearchOptions(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchType = searchType,
                    onSearchTypeChange = { searchType = it },
                    sortOrder = sortOrder,
                    onSortOrderChange = { sortOrder = it }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(filteredAndSortedItems) { searchItem ->
                        SearchItemCard(
                            searchItem = searchItem,
                            onClick = { onItemClick(searchItem.shelfId, searchItem.sectionId) },
                            settings = settings
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchOptions(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchType: SearchType,
    onSearchTypeChange: (SearchType) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = searchType == SearchType.ITEM_NAME,
                onClick = { onSearchTypeChange(SearchType.ITEM_NAME) },
                label = { Text(stringResource(R.string.search_by_item_name)) }
            )
            FilterChip(
                selected = searchType == SearchType.CLIENT_NAME,
                onClick = { onSearchTypeChange(SearchType.CLIENT_NAME) },
                label = { Text(stringResource(R.string.search_by_client_name)) }
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.sort_by),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SortChip(
                        text = stringResource(R.string.name),
                        isAscending = sortOrder == SortOrder.NAME_ASC,
                        isSelected = sortOrder in listOf(SortOrder.NAME_ASC, SortOrder.NAME_DESC),
                        onClick = {
                            onSortOrderChange(
                                if (sortOrder == SortOrder.NAME_ASC)
                                    SortOrder.NAME_DESC else SortOrder.NAME_ASC
                            )
                        }
                    )
                    SortChip(
                        text = stringResource(R.string.entry),
                        isAscending = sortOrder == SortOrder.ENTRY_DATE_ASC,
                        isSelected = sortOrder in listOf(SortOrder.ENTRY_DATE_ASC, SortOrder.ENTRY_DATE_DESC),
                        onClick = {
                            onSortOrderChange(
                                if (sortOrder == SortOrder.ENTRY_DATE_ASC)
                                    SortOrder.ENTRY_DATE_DESC else SortOrder.ENTRY_DATE_ASC
                            )
                        }
                    )
                    SortChip(
                        text = stringResource(R.string.return_sort),
                        isAscending = sortOrder == SortOrder.RETURN_DATE_ASC,
                        isSelected = sortOrder in listOf(SortOrder.RETURN_DATE_ASC, SortOrder.RETURN_DATE_DESC),
                        onClick = {
                            onSortOrderChange(
                                if (sortOrder == SortOrder.RETURN_DATE_ASC)
                                    SortOrder.RETURN_DATE_DESC else SortOrder.RETURN_DATE_ASC
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(
    text: String,
    isAscending: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text)
                if (isSelected) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isAscending) "Ascending" else "Descending",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SearchItemCard(
    searchItem: SearchItem,
    onClick: () -> Unit,
    settings: com.example.storage_manager.model.Settings
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = searchItem.item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = searchItem.item.clientName ?: stringResource(R.string.unknown_client),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(
                        R.string.shelf_section_format,
                        searchItem.shelfName,
                        searchItem.sectionNumber
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = stringResource(R.string.entry_date_label),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = searchItem.item.entryDate?.toDisplayFormat(settings) ?: "N/A",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.return_date_label),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = searchItem.item.returnDate?.toDisplayFormat(settings) ?: "N/A",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Data class to hold search item information
data class SearchItem(
    val item: Item,
    val shelfId: String,
    val sectionId: String,
    val shelfName: String,
    val sectionNumber: Int
)

// Enum for sort orders
enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    ENTRY_DATE_ASC,
    ENTRY_DATE_DESC,
    RETURN_DATE_ASC,
    RETURN_DATE_DESC
}