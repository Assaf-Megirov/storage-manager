package com.awindyendprod.storage_manager.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.awindyendprod.storage_manager.R

@Composable
fun DueItemsAlertDialog(
    itemCount: Int,
    daysBefore: Int,
    onDismiss: () -> Unit,
    onViewItems: () -> Unit
) {
    val dayText = when (daysBefore) {
        0 -> stringResource(R.string.today)
        1 -> stringResource(R.string.tomorrow)
        else -> stringResource(R.string.in_days, daysBefore)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.high_due_items_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.high_due_items_dialog_message, itemCount, dayText))
        },
        confirmButton = {
            TextButton(onClick = onViewItems) {
                Text(stringResource(R.string.items_due))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        }
    )
}
