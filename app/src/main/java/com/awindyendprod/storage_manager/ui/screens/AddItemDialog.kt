import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.ui.screens.DatePickerField
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
    LaunchedEffect(returnDate) {
        if (alarmDate == null || alarmDate == returnDate) {
            onAlarmDateChange(returnDate)
        }
    }

    LaunchedEffect(hasAlarm) {
        if (hasAlarm && alarmDate == null) {
            onAlarmDateChange(returnDate)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Card(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.85f else 1f)
                .then(if (isLandscape) {
                    Modifier.fillMaxHeight(0.95f)
                } else {
                    Modifier.wrapContentHeight()
                })
                .padding(
                    horizontal = if (isLandscape) 32.dp else 8.dp,
                    vertical = if (isLandscape) 16.dp else 8.dp
                ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isLandscape) {
                        Modifier.verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    })
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.add_item),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(2f)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = onAddItem,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.add))
                        }
                    }
                }

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = onNameChange,
                                label = { Text(stringResource(R.string.item_name), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = clientName,
                                onValueChange = onClientNameChange,
                                label = { Text(stringResource(R.string.client_name), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = note,
                                onValueChange = onNoteChange,
                                label = { Text(stringResource(R.string.note), style = MaterialTheme.typography.bodyMedium) },
                                textStyle = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            label = { Text(stringResource(R.string.item_name), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = clientName,
                            onValueChange = onClientNameChange,
                            label = { Text(stringResource(R.string.client_name), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = onNoteChange,
                            label = { Text(stringResource(R.string.note), style = MaterialTheme.typography.bodyMedium) },
                            textStyle = MaterialTheme.typography.bodyMedium,
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
            }
        }
    }
} 