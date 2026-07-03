package com.awindyendprod.storage_manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.awindyendprod.storage_manager.viewmodel.StorageTrackerViewModel
import com.awindyendprod.storage_manager.viewmodel.SettingsViewModel
import com.awindyendprod.storage_manager.R
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: StorageTrackerViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDaySelected: (String) -> Unit
) {
    val settings by settingsViewModel.settings.collectAsState()
    val shelves by viewModel.shelves.collectAsState()

    val locale = remember(settings.language) {
        when (settings.language) {
            com.awindyendprod.storage_manager.model.AppLanguage.SYSTEM -> Locale.getDefault()
            com.awindyendprod.storage_manager.model.AppLanguage.ENGLISH -> Locale("en")
            com.awindyendprod.storage_manager.model.AppLanguage.HEBREW -> Locale("he")
            com.awindyendprod.storage_manager.model.AppLanguage.RUSSIAN -> Locale("ru")
        }
    }

    val monthFormatter = remember(locale) { SimpleDateFormat("LLLL yyyy", locale) }
    val weekdayFormatter = remember(locale) { SimpleDateFormat("EE", locale) }
    val isoFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT) }

    var visibleMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.time) }

    val firstDayOfWeek = remember(locale) {
        // Use locale first day (Calendar.getInstance with locale)
        Calendar.getInstance(locale).firstDayOfWeek
    }

    val daysGrid = remember(visibleMonth, firstDayOfWeek, locale) {
        // Build a list of 42 dates covering the 6x7 grid for the visible month
        val cal = Calendar.getInstance(locale).apply {
            time = visibleMonth
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthIndex = cal.get(Calendar.MONTH)
        val startCal = Calendar.getInstance(locale).apply {
            time = cal.time
            set(Calendar.DAY_OF_MONTH, 1)
            var shift = get(Calendar.DAY_OF_WEEK) - firstDayOfWeek
            if (shift < 0) shift += 7
            add(Calendar.DAY_OF_MONTH, -shift)
        }
        val list = mutableListOf<Date>()
        repeat(42) {
            list.add(startCal.time)
            startCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        Pair(monthIndex, list)
    }

    val countsByIso = remember(shelves, daysGrid) {
        val counts = HashMap<String, Int>()
        if (shelves.isNotEmpty()) {
            // Precompute range for quick contains check
            val first = daysGrid.second.first()
            val last = daysGrid.second.last()
            val firstCal = Calendar.getInstance(locale).apply { time = first; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val lastCal = Calendar.getInstance(locale).apply { time = last; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }

            shelves.forEach { shelf ->
                shelf.sections.forEach { section ->
                    section.items.forEach { item ->
                        item.returnDate?.let { rd ->
                            val c = Calendar.getInstance(locale).apply { time = rd }
                            if (!c.time.before(firstCal.time) && !c.time.after(lastCal.time)) {
                                val key = isoFormatter.format(c.time)
                                counts[key] = (counts[key] ?: 0) + 1
                            }
                        }
                    }
                }
            }
        }
        counts
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = monthFormatter.format(visibleMonth).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    Row {
                        IconButton(onClick = {
                            val cal = Calendar.getInstance(locale).apply { time = visibleMonth; add(Calendar.MONTH, -1) }
                            visibleMonth = cal.time
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                        IconButton(onClick = {
                            val cal = Calendar.getInstance(locale).apply { time = visibleMonth; add(Calendar.MONTH, 1) }
                            visibleMonth = cal.time
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Weekday headers
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val headers = weekdayHeaders(firstDayOfWeek, locale, weekdayFormatter)
                headers.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                columns = GridCells.Fixed(7),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(daysGrid.second) { date ->
                    val cal = Calendar.getInstance(locale).apply { time = date }
                    val isOutOfMonth = cal.get(Calendar.MONTH) != daysGrid.first
                    val todayCal = Calendar.getInstance(locale)
                    val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                    val isPast = cal.before(todayCal) && !isToday
                    val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
                    val key = isoFormatter.format(cal.time)
                    val count = countsByIso[key] ?: 0

                    DayCell(
                        dayNumber = dayNumber,
                        count = count,
                        isToday = isToday,
                        isPast = isPast,
                        isOutOfMonth = isOutOfMonth,
                        onClick = { onDaySelected(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: String,
    count: Int,
    isToday: Boolean,
    isPast: Boolean,
    isOutOfMonth: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        isPast -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .height(56.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Day number centered
            Box(modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = dayNumber,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
                    color = if (isOutOfMonth) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
                )
            }
            // Simple badge at top end when count > 0
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                ) {
                    Text(
                        text = if (count > 99) "99+" else count.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

private fun weekdayHeaders(firstDayOfWeek: Int, locale: Locale, formatter: DateFormat): List<String> {
    val cal = Calendar.getInstance(locale)
    cal.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    val result = mutableListOf<String>()
    repeat(7) {
        result.add(formatter.format(cal.time))
        cal.add(Calendar.DAY_OF_WEEK, 1)
    }
    return result
}



