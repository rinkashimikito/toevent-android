package com.immedio.toevent.ui.events

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
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
    val activeSurface by viewModel.activeSurface.collectAsStateWithLifecycle()
    val thresholds by viewModel.urgencyThresholds.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshEvents()
    }

    LaunchedEffect(nextEvent, activeSurface, privacyMode, thresholds) {
        if (activeSurface.notificationEnabled && nextEvent != null) {
            val intent = com.immedio.toevent.service.CountdownService.startIntent(
                context, nextEvent!!, privacyMode, thresholds,
            )
            context.startForegroundService(intent)
        } else if (!activeSurface.notificationEnabled) {
            context.stopService(com.immedio.toevent.service.CountdownService.stopIntent(context))
        }
    }

    val floatingChipEnabled by viewModel.floatingChipEnabled.collectAsStateWithLifecycle()
    LaunchedEffect(nextEvent, floatingChipEnabled, privacyMode, thresholds) {
        if (floatingChipEnabled && nextEvent != null &&
            android.provider.Settings.canDrawOverlays(context)
        ) {
            context.startService(
                com.immedio.toevent.service.FloatingCountdownService.startIntent(
                    context, nextEvent!!, privacyMode, thresholds,
                ),
            )
        } else if (!floatingChipEnabled) {
            context.stopService(
                com.immedio.toevent.service.FloatingCountdownService.stopIntent(context),
            )
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Event")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshEvents() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (events.isEmpty() && !isRefreshing) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    // iOS large title
                    item(key = "large_title") {
                        Text(
                            text = "Events",
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 8.dp,
                            ),
                        )
                    }

                    // Countdown banner
                    nextEvent?.let { event ->
                        item(key = "countdown_banner") {
                            CountdownBanner(
                                eventTitle = if (privacyMode) "Busy" else event.title,
                                countdownText = countdownText,
                                accentColor = urgencyLevel.color(isDark),
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .animateItem(
                                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    ),
                            )
                        }
                    }

                    val grouped = groupEventsByDay(events)
                    grouped.forEach { (header, dayEvents) ->
                        item(key = "header_$header") {
                            DayHeader(
                                label = header,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                ),
                            )
                        }
                        item(key = "section_$header") {
                            IOSSection(
                                events = dayEvents,
                                conflictingIds = conflictingIds,
                                privacyMode = privacyMode,
                                onClick = { onNavigateToDetail(it.id) },
                                onLongClick = { event ->
                                    if (viewModel.isEventActive(event)) {
                                        viewModel.dismissFromDisplay(event)
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .animateItem(
                                        fadeInSpec = spring(stiffness = Spring.StiffnessLow),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    ),
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
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eventTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Next up",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = countdownText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = accentColor,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No events this week",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pull to refresh",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IOSSection(
    events: List<Event>,
    conflictingIds: Set<String>,
    privacyMode: Boolean,
    onClick: (Event) -> Unit,
    onLongClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            events.forEachIndexed { index, event ->
                EventRow(
                    event = event,
                    isConflicting = event.id in conflictingIds,
                    privacyMode = privacyMode,
                    onClick = { onClick(event) },
                    onLongClick = { onLongClick(event) },
                )
                if (index < events.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
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
    val calendarColor = Color(event.calendarColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Calendar color left border
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(calendarColor),
        )
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (privacyMode) "Busy" else event.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatTimeRange(event),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (event.meetingUrl != null && !privacyMode) {
            Text(
                text = "Join",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(horizontal = 4.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .size(20.dp)
                .padding(end = 0.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
    }
}

private fun formatTimeRange(event: Event): String {
    if (event.isAllDay) return "All day"
    val start = DateFormatters.formatAbsoluteTime(event.startDate)
    val end = DateFormatters.formatAbsoluteTime(event.endDate)
    return "$start \u2013 $end"
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
