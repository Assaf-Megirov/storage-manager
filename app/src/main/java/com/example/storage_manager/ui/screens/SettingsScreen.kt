package com.example.storage_manager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.storage_manager.R
import com.example.storage_manager.model.SectionDateType
import com.example.storage_manager.model.DateDisplayFormat
import com.example.storage_manager.model.AppLanguage
import com.example.storage_manager.viewmodel.SettingsViewModel
import com.example.storage_manager.model.FontSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section Date Type Setting
            Column {
                Text(
                    text = stringResource(R.string.section_date_display),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.sectionDateType == SectionDateType.ENTRY_DATE,
                        onClick = { viewModel.updateSectionDateType(SectionDateType.ENTRY_DATE) },
                        label = { Text(stringResource(R.string.entry_date)) }
                    )
                    FilterChip(
                        selected = settings.sectionDateType == SectionDateType.RETURN_DATE,
                        onClick = { viewModel.updateSectionDateType(SectionDateType.RETURN_DATE) },
                        label = { Text(stringResource(R.string.return_date)) }
                    )
                }
            }

            // Date Display Format Setting
            Column {
                Text(
                    text = stringResource(R.string.date_display_format),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.dateDisplayFormat == DateDisplayFormat.NUMERIC,
                        onClick = { viewModel.updateDateDisplayFormat(DateDisplayFormat.NUMERIC) },
                        label = { Text(stringResource(R.string.numeric_date)) }
                    )
                    FilterChip(
                        selected = settings.dateDisplayFormat == DateDisplayFormat.DAY_OF_WEEK,
                        onClick = { viewModel.updateDateDisplayFormat(DateDisplayFormat.DAY_OF_WEEK) },
                        label = { Text(stringResource(R.string.day_of_week)) }
                    )
                }
            }

            // Default Return Date Setting
            Column {
                Text(
                    text = stringResource(R.string.default_return_date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = settings.defaultReturnDateDays.toString(),
                    onValueChange = { 
                        it.toIntOrNull()?.let { days ->
                            if (days > 0) viewModel.updateDefaultReturnDateDays(days)
                        }
                    },
                    label = { Text(stringResource(R.string.days_after_entry)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Language Setting
            Column {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.language == AppLanguage.SYSTEM,
                        onClick = { viewModel.updateLanguage(AppLanguage.SYSTEM) },
                        label = { Text(stringResource(R.string.system_language)) }
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.ENGLISH,
                        onClick = { viewModel.updateLanguage(AppLanguage.ENGLISH) },
                        label = { Text(stringResource(R.string.english_language)) }
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.HEBREW,
                        onClick = { viewModel.updateLanguage(AppLanguage.HEBREW) },
                        label = { Text(stringResource(R.string.hebrew_language)) }
                    )
                    FilterChip(
                        selected = settings.language == AppLanguage.RUSSIAN,
                        onClick = { viewModel.updateLanguage(AppLanguage.RUSSIAN) },
                        label = { Text(stringResource(R.string.russian_language)) }
                    )
                }
            }

            // Font Size Setting
            Column {
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontSize.values().forEach { fontSize ->
                        FilterChip(
                            selected = settings.fontSize == fontSize,
                            onClick = { viewModel.updateFontSize(fontSize) },
                            label = { Text(stringResource(
                                when (fontSize) {
                                    FontSize.SMALL -> R.string.small
                                    FontSize.MEDIUM -> R.string.medium
                                    FontSize.LARGE -> R.string.large
                                }
                            )) }
                        )
                    }
                }
            }

            // Section Height Setting
            Column {
                Text(
                    text = stringResource(R.string.section_height),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var heightText by remember { mutableStateOf(settings.sectionHeight.toString()) }

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { newValue -> 
                        // Update the text field immediately
                        heightText = newValue
                        // Try to update the setting if it's valid
                        if (newValue.all { it.isDigit() }) {
                            newValue.toIntOrNull()?.let { height ->
                                if (height in 100..500) {
                                    viewModel.updateSectionHeight(height)
                                }
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.height_in_dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section Width Setting
            Column {
                Text(
                    text = stringResource(R.string.section_width),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var widthText by remember { mutableStateOf(settings.sectionWidth.toString()) }

                OutlinedTextField(
                    value = widthText,
                    onValueChange = { newValue -> 
                        // Update the text field immediately
                        widthText = newValue
                        // Try to update the setting if it's valid
                        if (newValue.all { it.isDigit() }) {
                            newValue.toIntOrNull()?.let { width ->
                                if (width in 100..500) {
                                    viewModel.updateSectionWidth(width)
                                }
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.width_in_dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
} 