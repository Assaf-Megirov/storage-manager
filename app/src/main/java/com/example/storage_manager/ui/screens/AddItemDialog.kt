import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.storage_manager.R
import com.example.storage_manager.model.Settings
import com.example.storage_manager.ui.screens.DatePickerField
import java.util.Date

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAddItem: () -> Unit,
    name: String,
    clientName: String,
    note: String,
    hasAlarm: Boolean,
    entryDate: Date,
    returnDate: Date,
    onNameChange: (String) -> Unit,
    onClientNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onHasAlarmChange: (Boolean) -> Unit,
    onEntryDateChange: (Date) -> Unit,
    onReturnDateChange: (Date) -> Unit,
    alarmDate: Date?,
    onAlarmDateChange: (Date) -> Unit,
    settings: Settings
) {
    // Update alarm date whenever return date changes
    LaunchedEffect(returnDate) {
        if (alarmDate == null || alarmDate == returnDate) {
            onAlarmDateChange(returnDate)
        }
    }

    // When hasAlarm is toggled on, set alarm date to return date if it's not already set
    LaunchedEffect(hasAlarm) {
        if (hasAlarm && alarmDate == null) {
            onAlarmDateChange(returnDate)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Card(
            modifier = Modifier
                .padding(if (isLandscape) 0.dp else 4.dp)
                .fillMaxWidth(),
            shape = if (isLandscape) {
                MaterialTheme.shapes.extraSmall
            } else {
                MaterialTheme.shapes.medium
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isLandscape) 8.dp else 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_item),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = onNameChange,
                                label = { Text(stringResource(R.string.item_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = onClientNameChange,
                                label = { Text(stringResource(R.string.client_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = note,
                                onValueChange = onNoteChange,
                                label = { Text(stringResource(R.string.note)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Right column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DatePickerField(
                                label = stringResource(R.string.entry_date),
                                selectedDate = entryDate,
                                onDateChange = onEntryDateChange,
                                settings = settings
                            )
                            DatePickerField(
                                label = stringResource(R.string.return_date),
                                selectedDate = returnDate,
                                onDateChange = onReturnDateChange,
                                settings = settings
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = hasAlarm,
                                    onCheckedChange = onHasAlarmChange
                                )
                                Text(
                                    text = stringResource(R.string.has_alarm),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            if (hasAlarm) {
                                DatePickerField(
                                    label = stringResource(R.string.alarm_date),
                                    selectedDate = alarmDate ?: returnDate,
                                    onDateChange = onAlarmDateChange,
                                    settings = settings
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = onNameChange,
                            label = { Text(stringResource(R.string.item_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = onClientNameChange,
                            label = { Text(stringResource(R.string.client_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = onNoteChange,
                            label = { Text(stringResource(R.string.note)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DatePickerField(
                            label = stringResource(R.string.entry_date),
                            selectedDate = entryDate,
                            onDateChange = onEntryDateChange,
                            settings = settings
                        )
                        DatePickerField(
                            label = stringResource(R.string.return_date),
                            selectedDate = returnDate,
                            onDateChange = onReturnDateChange,
                            settings = settings
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hasAlarm,
                                onCheckedChange = onHasAlarmChange
                            )
                            Text(
                                text = stringResource(R.string.has_alarm),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (hasAlarm) {
                            DatePickerField(
                                label = stringResource(R.string.alarm_date),
                                selectedDate = alarmDate ?: returnDate,
                                onDateChange = onAlarmDateChange,
                                settings = settings
                            )
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAddItem) {
                        Text(stringResource(R.string.add))
                    }
                }
            }
        }
    }
} 