package com.immedio.toevent.ui.events

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Events") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_INSERT)
                        .setData(CalendarContract.Events.CONTENT_URI)
                    context.startActivity(intent)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Event") },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
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
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    nextEvent?.let { event ->
                        item(key = "countdown_banner") {
                            HeroCountdownCard(
                                eventTitle = if (privacyMode) "Busy" else event.title,
                                countdownText = countdownText,
                                urgencyColor = urgencyLevel.color(isDark),
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
                        items(dayEvents, key = { it.id }) { event ->
                            EventCard(
                                event = event,
                                isConflicting = event.id in conflictingIds,
                                privacyMode = privacyMode,
                                onClick = { onNavigateToDetail(event.id) },
                                onLongClick = {
                                    if (viewModel.isEventActive(event)) {
                                        viewModel.dismissFromDisplay(event)
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
private fun HeroCountdownCard(
    eventTitle: String,
    countdownText: String,
    urgencyColor: Color,
    modifier: Modifier = Modifier,
) {
    val gradientStart = urgencyColor.copy(alpha = 0.6f)
    val gradientEnd = urgencyColor.copy(alpha = 0.2f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(gradientStart, gradientEnd),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
        ) {
            Column {
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                    ),
                    color = urgencyColor,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = eventTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No events this week",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pull to refresh or create a new event",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DayHeader(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EventCard(
    event: Event,
    isConflicting: Boolean,
    privacyMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val calendarColor = Color(event.calendarColor)
    val borderColor = if (isConflicting) {
        Color(0xFFFF9800).copy(alpha = 0.7f)
    } else {
        Color.Transparent
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isConflicting) {
            androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        ) {
            // Calendar color left strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(calendarColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
            ) {
                Text(
                    text = if (privacyMode) "Busy" else event.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimeRange(event),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!privacyMode) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.calendarTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            if (event.meetingUrl != null && !privacyMode) {
                SuggestionChip(
                    onClick = onClick,
                    label = { Text("Join", style = MaterialTheme.typography.labelSmall) },
                    icon = {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
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
