package com.example.storage_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    showPreview: Boolean = false,
    previewWidth: Float = value,
    previewHeight: Float = value
) {
    var textFieldValue by remember { mutableStateOf(value.toInt().toString()) }

    // Update text field when slider changes
    LaunchedEffect(value) {
        textFieldValue = value.toInt().toString()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$valueText (${valueRange.start.toInt()}-${valueRange.endInclusive.toInt()})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                newValue.toIntOrNull()?.let { intValue ->
                    if (intValue >= valueRange.start && intValue <= valueRange.endInclusive) {
                        onValueChange(intValue.toFloat())
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            isError = textFieldValue.toIntOrNull()?.let { 
                it < valueRange.start || it > valueRange.endInclusive 
            } ?: true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
        
        if (showPreview) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(previewWidth.dp)
                    .height(previewHeight.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            )
        }
    }
} 