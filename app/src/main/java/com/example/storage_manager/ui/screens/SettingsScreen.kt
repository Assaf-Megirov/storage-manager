package com.example.storage_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.example.storage_manager.ui.components.SettingsSlider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowUp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    // Add file pickers
    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

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

            // Language selection
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppLanguage.values().forEach { language ->
                    FilledTonalButton(
                        onClick = { viewModel.updateLanguage(language) },
                        modifier = Modifier.wrapContentWidth(),
                        enabled = settings.language != language
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.SYSTEM -> stringResource(R.string.system_language)
                                AppLanguage.ENGLISH -> stringResource(R.string.english_language)
                                AppLanguage.HEBREW -> stringResource(R.string.hebrew_language)
                                AppLanguage.RUSSIAN -> stringResource(R.string.russian_language)
                            },
                            maxLines = 1
                        )
                    }
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
            SettingsSlider(
                title = stringResource(R.string.section_height),
                value = settings.sectionHeight.toFloat(),
                onValueChange = { newValue -> 
                    viewModel.updateSectionHeight(newValue.toInt())
                },
                valueRange = 100f..300f,
                valueText = stringResource(R.string.height_in_dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section Width Setting
            SettingsSlider(
                title = stringResource(R.string.section_width),
                value = settings.sectionWidth.toFloat(),
                onValueChange = { newValue ->
                    viewModel.updateSectionWidth(newValue.toInt())
                },
                valueRange = 100f..300f,
                valueText = stringResource(R.string.width_in_dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Preview shelf with multiple sections
            Text(
                text = stringResource(R.string.shelf_preview),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .width(settings.sectionWidth.dp)
                            .height(settings.sectionHeight.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                }
            }


            // Data Management Section
            Text(
                text = stringResource(R.string.data_management),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top=8.dp)
            )

            // Export Button
            Button(
                onClick = {
                    exportFilePicker.launch("storage_manager_backup.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.export_data))
            }

            // Import Button
            Button(
                onClick = {
                    importFilePicker.launch(arrayOf("application/json"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.import_data))
            }
        }
    }
} 