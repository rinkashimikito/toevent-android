package com.immedio.toevent.ui.events

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.util.DateFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { viewModel.refreshEvents() }

    val events by viewModel.events.collectAsStateWithLifecycle()
    val conflictingIds by viewModel.conflictingEventIds.collectAsStateWithLifecycle()
    val privacyMode by viewModel.privacyMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val event = events.find { it.id == eventId }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            text = "Events",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 17.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (event == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Event not found",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Large title
                Text(
                    text = if (privacyMode) "Busy" else event.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Conflict warning
                if (event.id in conflictingIds) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This event conflicts with another event",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Metadata section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        // Time row
                        MetadataRow(
                            icon = Icons.Default.AccessTime,
                            label = formatEventTime(event),
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        // Calendar row
                        MetadataRow(
                            icon = Icons.Default.CalendarMonth,
                            label = event.calendarTitle,
                            leadingDot = Color(event.calendarColor),
                        )
                        // Location row
                        if (!event.location.isNullOrBlank() && !privacyMode) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline,
                            )
                            MetadataRow(
                                icon = Icons.Default.LocationOn,
                                label = event.location,
                                onClick = {
                                    val geoUri = Uri.parse(
                                        "geo:0,0?q=${Uri.encode(event.location)}",
                                    )
                                    context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Join Meeting button
                if (event.meetingUrl != null && !privacyMode) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.meetingUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(
                                text = "Join Meeting",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Open in Calendar button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Button(
                        onClick = {
                            val eventIdLong = event.id.toLongOrNull()
                            val intent = if (eventIdLong != null) {
                                Intent(Intent.ACTION_VIEW).setData(
                                    ContentUris.withAppendedId(
                                        CalendarContract.Events.CONTENT_URI,
                                        eventIdLong,
                                    ),
                                )
                            } else {
                                Intent(Intent.ACTION_VIEW)
                                    .setData(CalendarContract.Events.CONTENT_URI)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            text = "Open in Calendar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }

                // Notes section
                if (!event.notes.isNullOrBlank() && !privacyMode) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "NOTES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 32.dp, bottom = 6.dp),
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = event.notes,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    leadingDot: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (leadingDot != null) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(leadingDot),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun formatEventTime(event: Event): String {
    if (event.isAllDay) return "All day"
    val start = DateFormatters.formatAbsoluteTime(event.startDate)
    val end = DateFormatters.formatAbsoluteTime(event.endDate)
    return "$start \u2013 $end"
}
