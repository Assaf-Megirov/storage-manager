package com.awindyendprod.storage_manager.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.services.PhoneNumberService

@Composable
fun PhoneActionMenu(phoneNumber: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier) {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(20.dp)) {
            Icon(
                Icons.Default.Call,
                contentDescription = stringResource(R.string.phone_actions),
                modifier = Modifier.size(14.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_on_whatsapp)) },
                onClick = {
                    expanded = false
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(PhoneNumberService.buildWhatsAppUrl(phoneNumber)))
                        )
                    }
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.call)) },
                onClick = {
                    expanded = false
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse(PhoneNumberService.buildTelUri(phoneNumber)))
                        )
                    }
                }
            )
        }
    }
}
