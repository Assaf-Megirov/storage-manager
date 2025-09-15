package com.awindyendprod.storage_manager.ui.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.ShelfSection
import com.awindyendprod.storage_manager.ui.screens.DatePickerField
import java.util.Date

// Smart TextField with Overlay Chip for Keyboard Switching
@Composable
fun SmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    modifier: Modifier = Modifier,
    startWithNumbers: Boolean = false
) {
    var keyboardType by remember { mutableStateOf(if (startWithNumbers) KeyboardType.Number else KeyboardType.Text) }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            textStyle = textStyle,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (keyboardType == KeyboardType.Number) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    keyboardType = KeyboardType.Text
                    focusManager.clearFocus()
                    focusRequester.requestFocus()
                }
            ),
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
        )

        // Floating chip that appears when focused and on number keyboard
        if (isFocused && keyboardType == KeyboardType.Number) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-12).dp, y = (-8).dp) // Use offset instead of padding
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        keyboardType = KeyboardType.Text
                        focusManager.clearFocus()
                        focusRequester.requestFocus()
                    },
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.switch_to_abc),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddItem: () -> Unit,
    name: String,
    clientName: String,
    note: String,
    hasAlarm: Boolean,
    entryDate: Date,
    returnDate: Date,
    onNameChange: (String) -> Unit,
    onClientNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onHasAlarmChange: (Boolean) -> Unit,
    onEntryDateChange: (Date) -> Unit,
    onReturnDateChange: (Date) -> Unit,
    alarmDate: Date?,
    onAlarmDateChange: (Date) -> Unit,
    newSelectedShelfId: String,
    onSelectedShelfIdChange: (String) -> Unit,
    newSelectedSectionId: String,
    onSelectedSectionIdChange: (String) -> Unit,
    shelves: List<Shelf>,
    settings: Settings
) {
    LaunchedEffect(returnDate) {
        if (alarmDate == null || alarmDate == returnDate) {
            onAlarmDateChange(returnDate)
        }
    }

    LaunchedEffect(hasAlarm) {
        if (hasAlarm && alarmDate == null) {
            onAlarmDateChange(returnDate)
        }
    }

    LaunchedEffect(newSelectedShelfId) {
        // Only set default section if no section is currently selected
        // or if the current section doesn't belong to the selected shelf
        val currentSectionBelongsToShelf = shelves
            .find { it.id == newSelectedShelfId }
            ?.sections
            ?.any { it.id == newSelectedSectionId } == true

        if (!currentSectionBelongsToShelf) {
            val defaultSection = shelves
                .find { it.id == newSelectedShelfId }
                ?.sections
                ?.firstOrNull()
            if (defaultSection != null) {
                onSelectedSectionIdChange(defaultSection.id)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 1f)
                .then(if (isLandscape) {
                    Modifier.fillMaxHeight(0.95f)
                } else {
                    Modifier.wrapContentHeight()
                })
                .padding(
                    horizontal = if (isLandscape) 32.dp else 8.dp,
                    vertical = if (isLandscape) 16.dp else 8.dp
                ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier.verticalScroll(rememberScrollState())
                    })
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_item),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(2f)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(
                            onClick = onAddItem,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.add),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Item name with smart keyboard switching
                            SmartTextField(
                                value = name,
                                onValueChange = onNameChange,
                                label = { Text(stringResource(R.string.item_name), style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.fillMaxWidth(),
                                startWithNumbers = true
                            )

                            // Client name - regular text field (names are usually text)
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = onClientNameChange,
                                label = { Text(stringResource(R.string.client_name), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Note - regular text field
                            OutlinedTextField(
                                value = note,
                                onValueChange = onNoteChange,
                                label = { Text(stringResource(R.string.note), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ShelfDropdown(
                                shelves = shelves,
                                selectedShelfId = newSelectedShelfId,
                                onShelfSelected = onSelectedShelfIdChange,
                                modifier = Modifier.fillMaxWidth()
                            )

                            SectionDropdown(
                                sections = shelves.find { it.id == newSelectedShelfId }?.sections ?: emptyList(),
                                selectedSectionId = newSelectedSectionId,
                                onSectionSelected = onSelectedSectionIdChange,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DatePickerField(
                                label = stringResource(R.string.entry_date),
                                selectedDate = entryDate,
                                onDateChange = onEntryDateChange,
                                settings = settings
                            )
                            DatePickerField(
                                label = stringResource(R.string.return_date),
                                selectedDate = returnDate,
                                onDateChange = onReturnDateChange,
                                settings = settings
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = hasAlarm,
                                    onCheckedChange = onHasAlarmChange
                                )
                                Text(
                                    text = stringResource(R.string.has_alarm),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (hasAlarm) {
                                DatePickerField(
                                    label = stringResource(R.string.alarm_date),
                                    selectedDate = alarmDate ?: returnDate,
                                    onDateChange = onAlarmDateChange,
                                    settings = settings
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Item name with smart keyboard switching
                        SmartTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text(stringResource(R.string.item_name), style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            startWithNumbers = true
                        )

                        // Client name - regular text field
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = onClientNameChange,
                            label = { Text(stringResource(R.string.client_name), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Note - regular text field
                        OutlinedTextField(
                            value = note,
                            onValueChange = onNoteChange,
                            label = { Text(stringResource(R.string.note), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ShelfDropdown(
                            shelves = shelves,
                            selectedShelfId = newSelectedShelfId,
                            onShelfSelected = onSelectedShelfIdChange,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Log.d("AddItemDialog", "newSelectedShelfId: $newSelectedShelfId")
                        Log.d("AddItemDialog", "newSelectedSectionId: $newSelectedSectionId")
                        SectionDropdown(
                            sections = shelves.find { it.id == newSelectedShelfId }?.sections ?: emptyList(),
                            selectedSectionId = newSelectedSectionId,
                            onSectionSelected = onSelectedSectionIdChange,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DatePickerField(
                            label = stringResource(R.string.entry_date),
                            selectedDate = entryDate,
                            onDateChange = onEntryDateChange,
                            settings = settings
                        )
                        DatePickerField(
                            label = stringResource(R.string.return_date),
                            selectedDate = returnDate,
                            onDateChange = onReturnDateChange,
                            settings = settings
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasAlarm,
                                onCheckedChange = onHasAlarmChange
                            )
                            Text(
                                text = stringResource(R.string.has_alarm),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (hasAlarm) {
                            DatePickerField(
                                label = stringResource(R.string.alarm_date),
                                selectedDate = alarmDate ?: returnDate,
                                onDateChange = onAlarmDateChange,
                                settings = settings
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfDropdown(
    shelves: List<Shelf>,
    selectedShelfId: String,
    onShelfSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedShelf = shelves.find { it.id == selectedShelfId } ?: shelves.firstOrNull()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedShelf?.name ?: stringResource(R.string.shelf),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.shelf)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            shelves.forEachIndexed { index, shelf ->
                DropdownMenuItem(
                    text = { Text((index+1).toString()) },
                    onClick = {
                        onShelfSelected(shelf.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDropdown(
    sections: List<ShelfSection>,
    selectedSectionId: String,
    onSectionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSection = sections.find { it.id == selectedSectionId } ?: sections.firstOrNull()
    val selectedSectionIndex = sections.indexOf(selectedSection)+1
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (sections.isNotEmpty()) selectedSectionIndex.toString() else stringResource(R.string.section),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.section)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor(),
            enabled = sections.isNotEmpty()
        )

        if (sections.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sections.forEachIndexed { index, section ->
                    DropdownMenuItem(
                        text = { Text((index+1).toString()) },
                        onClick = {
                            onSectionSelected(section.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}