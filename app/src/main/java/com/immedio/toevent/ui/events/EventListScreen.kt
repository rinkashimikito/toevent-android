package com.immedio.toevent.ui.events

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.util.DateFormatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventListScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val nextEvent by viewModel.nextEvent.collectAsStateWithLifecycle()
    val countdownText by viewModel.countdownText.collectAsStateWithLifecycle()
    val urgencyLevel by viewModel.urgencyLevel.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val conflictingIds by viewModel.conflictingEventIds.collectAsStateWithLifecycle()
    val privacyMode by viewModel.privacyMode.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ToEvent") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                    context.startActivity(intent)
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create event")
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshEvents() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (events.isEmpty() && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No upcoming events",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pull to refresh or tap + to create one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Countdown banner
                    nextEvent?.let { event ->
                        item(key = "countdown_banner") {
                            CountdownBanner(
                                eventTitle = if (privacyMode) "Busy" else event.title,
                                countdownText = countdownText,
                                urgencyColor = urgencyLevel.color(isDark),
                            )
                        }
                    }

                    val grouped = groupEventsByDay(events)
                    grouped.forEach { (header, dayEvents) ->
                        stickyHeader(key = "header_$header") {
                            DayHeader(header)
                        }
                        items(dayEvents, key = { it.id }) { event ->
                            EventRow(
                                event = event,
                                isConflicting = event.id in conflictingIds,
                                privacyMode = privacyMode,
                                onClick = { onNavigateToDetail(event.id) },
                                onLongClick = {
                                    if (viewModel.isEventActive(event)) {
                                        viewModel.dismissFromDisplay(event)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownBanner(
    eventTitle: String,
    countdownText: String,
    urgencyColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(urgencyColor.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = eventTitle,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = countdownText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = urgencyColor,
        )
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventRow(
    event: Event,
    isConflicting: Boolean,
    privacyMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(event.calendarColor)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (privacyMode) "Busy" else event.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatTimeRange(event),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isConflicting) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Conflict",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (event.meetingUrl != null) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Meeting link",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatTimeRange(event: Event): String {
    if (event.isAllDay) return "All day"
    val start = DateFormatters.formatAbsoluteTime(event.startDate)
    val end = DateFormatters.formatAbsoluteTime(event.endDate)
    return "$start - $end"
}

private fun groupEventsByDay(events: List<Event>): List<Pair<String, List<Event>>> {
    val cal = Calendar.getInstance()
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

    return events.groupBy { event ->
        cal.timeInMillis = event.startDate
        when {
            isSameDay(cal, today) -> "Today"
            isSameDay(cal, tomorrow) -> "Tomorrow"
            else -> dateFormat.format(Date(event.startDate))
        }
    }.toList()
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
