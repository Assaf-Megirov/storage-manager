package com.awindyendprod.storage_manager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings

@Composable
fun ProfileDropdown(
    profiles: List<ProfileData>,
    currentProfileId: String?,
    onProfileSelected: (String) -> Unit,
    onAddProfile: () -> Unit,
    onProfileRename: (String, String) -> Unit,
    onProfileDelete: (String) -> Unit,
    settings: Settings,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var profileToRename by remember { mutableStateOf<ProfileData?>(null) }
    var newProfileName by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        // Profile selector button
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val currentProfile = profiles.find { it.profile.id == currentProfileId }
            Text(
                text = currentProfile?.profile?.name ?: stringResource(R.string.select_profile),
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.profile_dropdown),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            Column(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                profiles.forEach { profile ->
                    ProfileDropdownItem(
                        profile = profile,
                        isSelected = profile.profile.id == currentProfileId,
                        onProfileSelected = { profileId ->
                            onProfileSelected(profileId)
                            expanded = false
                        },
                        onProfileRename = { profileId, newName ->
                            onProfileRename(profileId, newName)
                        },
                        onProfileDelete = { profileId ->
                            onProfileDelete(profileId)
                        },
                        settings = settings
                    )
                }
                
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddProfile()
                            expanded = false
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_profile),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_profile),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDropdownItem(
    profile: ProfileData,
    isSelected: Boolean,
    onProfileSelected: (String) -> Unit,
    onProfileRename: (String, String) -> Unit,
    onProfileDelete: (String) -> Unit,
    settings: Settings
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onProfileSelected(profile.profile.id) 
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                onClick = { 
                    showRenameDialog = true
                },
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
                    onClick = { onProfileDelete(profile.profile.id) },
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
}

@Composable
fun RenameProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_profile)) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank() && newName != currentName) {
                        onConfirm(newName)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
