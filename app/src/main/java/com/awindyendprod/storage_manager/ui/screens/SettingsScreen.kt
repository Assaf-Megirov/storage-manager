package com.awindyendprod.storage_manager.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.*
import com.awindyendprod.storage_manager.ui.components.SettingsSlider
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val exportFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportData(it) } }

    val importFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importData(it) } }

    var showExportMenu by remember { mutableStateOf(false) }

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
                },
                colors = smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
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
            // Section Date Type
            Column {
                Text(
                    text = stringResource(R.string.section_date_display),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionDateType.values().forEach { type ->
                        val selected = settings.sectionDateType == type
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateSectionDateType(type) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                containerColor         = MaterialTheme.colorScheme.surface,
                                labelColor             = MaterialTheme.colorScheme.onSurface
                            ),
                            label = {
                                Text(
                                    text = when(type) {
                                        SectionDateType.ENTRY_DATE -> stringResource(R.string.entry_date)
                                        SectionDateType.RETURN_DATE -> stringResource(R.string.return_date)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Date Display Format
            Column {
                Text(
                    text = stringResource(R.string.date_display_format),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateDisplayFormat.values().forEach { format ->
                        val selected = settings.dateDisplayFormat == format
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateDateDisplayFormat(format) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                containerColor         = MaterialTheme.colorScheme.surface,
                                labelColor             = MaterialTheme.colorScheme.onSurface
                            ),
                            label = {
                                Text(
                                    text = when(format) {
                                        DateDisplayFormat.NUMERIC     -> stringResource(R.string.numeric_date)
                                        DateDisplayFormat.DAY_OF_WEEK -> stringResource(R.string.day_of_week)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Default Return Date
            Column {
                Text(
                    text = stringResource(R.string.default_return_date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor           = MaterialTheme.colorScheme.primary,
                        focusedBorderColor    = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
            }

            // Language Selection
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
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
                        enabled = settings.language != language,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor       = MaterialTheme.colorScheme.primary,
                            contentColor         = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    ) {
                        Text(
                            text = when (language) {
                                AppLanguage.SYSTEM  -> stringResource(R.string.system_language)
                                AppLanguage.ENGLISH -> stringResource(R.string.english_language)
                                AppLanguage.HEBREW  -> stringResource(R.string.hebrew_language)
                                AppLanguage.RUSSIAN -> stringResource(R.string.russian_language)
                            }
                        )
                    }
                }
            }
            // Theme Selection

            Column {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Theme.values().forEach { theme ->
                        val selected = settings.theme == theme
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateTheme(theme) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                containerColor         = MaterialTheme.colorScheme.surface,
                                labelColor             = MaterialTheme.colorScheme.onSurface
                            ),
                            label = {
                                Text(
                                    text = when (theme) {
                                        Theme.SYSTEM -> stringResource(R.string.theme_system)
                                        Theme.LIGHT  -> stringResource(R.string.theme_light)
                                        Theme.DARK   -> stringResource(R.string.theme_dark)
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Font Size
            Column {
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontSize.values().forEach { size ->
                        val selected = settings.fontSize == size
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.updateFontSize(size) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimary,
                                containerColor         = MaterialTheme.colorScheme.surface,
                                labelColor             = MaterialTheme.colorScheme.onSurface
                            ),
                            label = {
                                Text(stringResource(
                                    when (size) {
                                        FontSize.SMALL  -> R.string.small
                                        FontSize.MEDIUM -> R.string.medium
                                        FontSize.LARGE  -> R.string.large
                                    }
                                ))
                            }
                        )
                    }
                }
            }

            // Section Size Sliders
            SettingsSlider(
                title      = stringResource(R.string.section_height),
                value      = settings.sectionHeight.toFloat(),
                onValueChange = { viewModel.updateSectionHeight(it.toInt()) },
                valueRange = 100f..300f,
                valueText  = stringResource(R.string.height_in_dp)
            )
            SettingsSlider(
                title      = stringResource(R.string.section_width),
                value      = settings.sectionWidth.toFloat(),
                onValueChange = { viewModel.updateSectionWidth(it.toInt()) },
                valueRange = 100f..300f,
                valueText  = stringResource(R.string.width_in_dp)
            )

            // Shelf Preview
            Text(
                text = stringResource(R.string.shelf_preview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
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

            // Data Management
            Text(
                text = stringResource(R.string.data_management),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
            Box {
                Button(
                    onClick = { showExportMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.export_data))
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.save_to_storage)) },
                        onClick = {
                            showExportMenu = false
                            exportFilePicker.launch("storage_manager_backup.json")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        onClick = {
                            showExportMenu = false
                            viewModel.shareData()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                }
            }

            Button(
                onClick = { importFilePicker.launch(arrayOf("application/json")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector    = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier       = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.import_data))
            }
        }
    }
}
