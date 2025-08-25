package com.awindyendprod.storage_manager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awindyendprod.storage_manager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var profileName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_profile)) },
        text = {
            Column {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { 
                        profileName = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.profile_name)) },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.profile_name_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (profileName.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(profileName.trim())
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
