package com.awindyendprod.storage_manager.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.*
import com.awindyendprod.storage_manager.ui.components.SettingsSlider
import com.awindyendprod.storage_manager.ui.components.ProfileDropdown
import com.awindyendprod.storage_manager.ui.components.NewProfileDialog
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.viewmodel.ProfileViewModel
import com.awindyendprod.storage_manager.viewmodel.DataTransferResult
import com.awindyendprod.storage_manager.viewmodel.SyncViewModel
import com.awindyendprod.storage_manager.viewmodel.SyncResultUi
import com.awindyendprod.storage_manager.viewmodel.MainDeviceStatus
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

private const val TAPS_TO_REVEAL_DANGER_ZONE = 7

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    profileViewModel: ProfileViewModel,
    syncViewModel: SyncViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val profiles by profileViewModel.profiles.collectAsState()
    val currentProfileId by profileViewModel.currentProfileId.collectAsState()
    val exportFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportData(it, profiles, currentProfileId) } }

    val importFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importData(it) { importedProfiles, importedCurrentProfileId ->
                profileViewModel.importProfiles(importedProfiles, importedCurrentProfileId)
            }
        }
    }

    val context = LocalContext.current
    val dataTransferResult by viewModel.dataTransferResult.collectAsState()
    LaunchedEffect(dataTransferResult) {
        when (dataTransferResult) {
            DataTransferResult.Success -> {
                Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_SHORT).show()
                viewModel.clearDataTransferResult()
            }
            DataTransferResult.Failed -> {
                Toast.makeText(context, context.getString(R.string.import_failed), Toast.LENGTH_SHORT).show()
                viewModel.clearDataTransferResult()
            }
            null -> Unit
        }
    }

    val syncUiState by syncViewModel.uiState.collectAsState()
    val syncResult by syncViewModel.syncResult.collectAsState()
    val showAdoptConfirmation by syncViewModel.showAdoptConfirmation.collectAsState()
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result -> syncViewModel.handleSignInResult(result.data) }

    if (showAdoptConfirmation) {
        AlertDialog(
            onDismissRequest = { syncViewModel.confirmAdopt(false) },
            title = { Text(stringResource(R.string.adopt_main_device_data_title)) },
            text = { Text(stringResource(R.string.adopt_main_device_data_message)) },
            confirmButton = {
                TextButton(onClick = { syncViewModel.confirmAdopt(true) }) {
                    Text(stringResource(R.string.adopt_main_device_data_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { syncViewModel.confirmAdopt(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LaunchedEffect(syncResult) {
        val messageRes = when (syncResult) {
            SyncResultUi.Success -> R.string.sync_success
            SyncResultUi.AuthRequired -> R.string.sync_auth_required
            SyncResultUi.NetworkUnavailable -> R.string.sync_network_unavailable
            SyncResultUi.Failed -> R.string.sync_failed
            SyncResultUi.SignInFailed -> R.string.sign_in_failed
            SyncResultUi.MainClaimRejected -> R.string.main_device_claim_rejected
            null -> null
        }
        messageRes?.let {
            Toast.makeText(context, context.getString(it), Toast.LENGTH_SHORT).show()
            syncViewModel.clearSyncResult()
        }
    }

    var showExportMenu by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }

    var aboutTapCount by remember { mutableStateOf(0) }
    var showDangerZone by remember { mutableStateOf(false) }
    var showCleanSlateDialog by remember { mutableStateOf(false) }
    var cleanSlateConfirmationText by remember { mutableStateOf("") }
    val resetInProgress by syncViewModel.resetInProgress.collectAsState()

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
            
            // Profile Management
            Column {
                Text(
                    text = stringResource(R.string.profiles),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_profiles_button),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = settings.showProfilesButton,
                        onCheckedChange = { viewModel.updateShowProfilesButton(it) }
                    )
                }
                ProfileDropdown(
                    profiles = profiles,
                    currentProfileId = currentProfileId,
                    onProfileSelected = { profileId ->
                        profileViewModel.switchProfile(profileId)
                    },
                    onAddProfile = {
                        showNewProfileDialog = true
                    },
                    onProfileRename = { profileId, newName ->
                        profileViewModel.updateProfileName(profileId, newName)
                    },
                    onProfileDelete = { profileId ->
                        profileViewModel.deleteProfile(profileId)
                    },
                    settings = settings,
                    modifier = Modifier.fillMaxWidth()
                )
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

            // FAB Settings
            Column {
                Text(
                    text = stringResource(R.string.floating_button_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.allow_moving_fab),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = settings.fabDragEnabled,
                        onCheckedChange = { viewModel.updateFabDragEnabled(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { viewModel.resetFabPositions() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reset_fab_positions))
                }
            }

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

            // Daily Notifications
            Column {
                Text(
                    text = stringResource(R.string.daily_notifications),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.daily_notifications),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = settings.dailyNotificationsEnabled,
                        onCheckedChange = { viewModel.updateDailyNotificationsEnabled(it) }
                    )
                }
                
                if (settings.dailyNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = settings.notificationDaysBefore.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { days ->
                                if (days in 0..7) viewModel.updateNotificationDaysBefore(days)
                            }
                        },
                        label = { Text(stringResource(R.string.notification_days_before)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = settings.notificationMaxItems.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let { items ->
                                if (items in 1..100) viewModel.updateNotificationMaxItems(items)
                            }
                        },
                        label = { Text(stringResource(R.string.notification_max_items)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.sync),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.enable_sync),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Switch(
                        checked = syncUiState.syncEnabled,
                        onCheckedChange = { syncViewModel.setSyncEnabled(it) }
                    )
                }

                if (syncUiState.syncEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (syncUiState.signedInAccountEmail == null) {
                        Button(
                            onClick = { signInLauncher.launch(syncViewModel.buildSignInIntent()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.sign_in_with_google))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = syncUiState.signedInAccountEmail.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            TextButton(onClick = { syncViewModel.signOut() }) {
                                Text(stringResource(R.string.sign_out))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = syncUiState.lastSyncedAtMillis?.let {
                                stringResource(
                                    R.string.last_synced_at,
                                    android.text.format.DateFormat.getMediumDateFormat(context).format(java.util.Date(it)),
                                    android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(it))
                                )
                            } ?: stringResource(R.string.never_synced),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { syncViewModel.syncNow(interactive = true) },
                            enabled = !syncUiState.syncInProgress,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (syncUiState.syncInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.sync_now))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.mark_as_main_device),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            Switch(
                                checked = syncUiState.mainDeviceStatus == MainDeviceStatus.THIS_DEVICE,
                                onCheckedChange = { syncViewModel.setMainDevice(it) }
                            )
                        }
                        Text(
                            text = when (syncUiState.mainDeviceStatus) {
                                MainDeviceStatus.THIS_DEVICE -> stringResource(R.string.this_is_main_device)
                                MainDeviceStatus.OTHER_DEVICE -> stringResource(R.string.main_device_is_elsewhere)
                                MainDeviceStatus.UNSET -> stringResource(R.string.no_main_device_set)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            viewModel.shareData(profiles, currentProfileId)
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

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!showDangerZone) {
                            aboutTapCount++
                            val remaining = TAPS_TO_REVEAL_DANGER_ZONE - aboutTapCount
                            when {
                                remaining <= 0 -> {
                                    showDangerZone = true
                                    Toast.makeText(context, context.getString(R.string.danger_zone), Toast.LENGTH_SHORT).show()
                                }
                                remaining <= 3 ->
                                    Toast.makeText(context, "$remaining more tap(s)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .padding(vertical = 16.dp)
            )

            if (showDangerZone) {
                Text(
                    text = stringResource(R.string.danger_zone),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = { showCleanSlateDialog = true },
                    enabled = !resetInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.clean_slate))
                }
            }
        }
    }

    // Profile management dialogs
    if (showNewProfileDialog) {
        NewProfileDialog(
            onDismiss = { showNewProfileDialog = false },
            onConfirm = { profileName ->
                profileViewModel.createProfile(profileName)
            }
        )
    }

    if (showCleanSlateDialog) {
        val confirmationWord = stringResource(R.string.clean_slate_confirmation_word)
        AlertDialog(
            onDismissRequest = {
                showCleanSlateDialog = false
                cleanSlateConfirmationText = ""
            },
            title = { Text(stringResource(R.string.clean_slate_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.clean_slate_dialog_message, confirmationWord))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cleanSlateConfirmationText,
                        onValueChange = { cleanSlateConfirmationText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCleanSlateDialog = false
                        cleanSlateConfirmationText = ""
                        syncViewModel.resetAllData { remoteDeleted ->
                            if (!remoteDeleted) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.clean_slate_remote_delete_failed),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            restartApp(context)
                        }
                    },
                    enabled = cleanSlateConfirmationText == confirmationWord && !resetInProgress
                ) {
                    Text(stringResource(R.string.clean_slate_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCleanSlateDialog = false
                    cleanSlateConfirmationText = ""
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun restartApp(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent?.let { context.startActivity(it) }
    Runtime.getRuntime().exit(0)
}
