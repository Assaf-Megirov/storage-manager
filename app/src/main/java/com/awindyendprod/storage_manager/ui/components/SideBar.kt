package com.awindyendprod.storage_manager.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.ProfileData

@Composable
fun SideBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onHelpClick: () -> Unit,
    onAllDueClick: () -> Unit,
    profiles: List<ProfileData>,
    currentProfileId: String?,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
    onProfileRename: (String, String) -> Unit,
    onProfileDelete: (String) -> Unit,
    settings: com.awindyendprod.storage_manager.model.Settings
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    
    Log.d("SideBar", "SideBar is being displayed")
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (settings.showProfilesButton) {
            IconButton(onClick = { showProfileDialog = true }) {
                Icon(Icons.Default.Person, contentDescription = stringResource(R.string.profiles))
            }
        }
        IconButton(onClick = onEditModeToggle) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_mode)
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
        }
        IconButton(onClick = onAllDueClick) {
            Icon(Icons.Default.Today, contentDescription = stringResource(R.string.items_due))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
        IconButton(onClick = onHelpClick) {
            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.help))
        }
    }
    
    // Profile Selection Dialog
    if (showProfileDialog) {
        ProfileSelectionDialog(
            profiles = profiles,
            currentProfileId = currentProfileId,
            onProfileSelected = { profileId ->
                onProfileSelected(profileId)
                showProfileDialog = false
            },
            onAddProfile = {
                onAddProfile()
                showProfileDialog = false
            },
            onProfileRename = onProfileRename,
            onProfileDelete = onProfileDelete,
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
fun ProfileSelectionDialog(
    profiles: List<ProfileData>,
    currentProfileId: String?,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
    onProfileRename: (String, String) -> Unit,
    onProfileDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profiles)) },
        text = {
            LazyColumn {
                items(profiles) { profile ->
                    ProfileSelectionItem(
                        profile = profile,
                        isSelected = profile.profile.id == currentProfileId,
                        onProfileSelected = onProfileSelected,
                        onProfileRename = onProfileRename,
                        onProfileDelete = onProfileDelete
                    )
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddProfile() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_profile),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.add_profile),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun ProfileSelectionItem(
    profile: ProfileData,
    isSelected: Boolean,
    onProfileSelected: (String) -> Unit,
    onProfileRename: (String, String) -> Unit,
    onProfileDelete: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProfileSelected(profile.profile.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = profile.profile.name,
            modifier = Modifier.weight(1f),
            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ) else MaterialTheme.typography.bodyMedium
        )
        
        Row {
            IconButton(
                onClick = { showRenameDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(R.string.rename_profile),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            if (!profile.profile.isDefault) {
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_profile),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
    
    // Rename Dialog
    if (showRenameDialog) {
        RenameProfileDialog(
            currentName = profile.profile.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                onProfileRename(profile.profile.id, newName)
                showRenameDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.confirm_delete_profile)) },
            text = {
                Column {
                    Text(stringResource(R.string.profile_delete_warning))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onProfileDelete(profile.profile.id)
                        showDeleteDialog = false
                    }
                ) {
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
}