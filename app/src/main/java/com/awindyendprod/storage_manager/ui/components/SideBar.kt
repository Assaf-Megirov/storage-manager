package com.awindyendprod.storage_manager.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awindyendprod.storage_manager.R

@Composable
fun SideBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onHelpClick: () -> Unit
) {
    Log.d("SideBar", "SideBar is being displayed")
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp, top=24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onEditModeToggle) {
            Icon(
                imageVector = if (isEditMode) Icons.Default.Done else Icons.Default.Edit,
                contentDescription = stringResource(R.string.edit_mode)
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
        IconButton(onClick = onHelpClick) {
            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.help))
        }
    }
}