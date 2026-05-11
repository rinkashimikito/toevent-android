package com.immedio.toevent.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.immedio.toevent.BuildConfig
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyThresholds
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val activeSurface by settingsViewModel.activeSurface.collectAsState()
    val backgroundMode by settingsViewModel.backgroundMode.collectAsState()
    val timeDisplayFormat by settingsViewModel.timeDisplayFormat.collectAsState()
    val useNaturalLanguage by settingsViewModel.useNaturalLanguage.collectAsState()
    val privacyMode by settingsViewModel.privacyMode.collectAsState()
    val hideAllDayEvents by settingsViewModel.hideAllDayEvents.collectAsState()
    val titleMaxLength by settingsViewModel.titleMaxLength.collectAsState()
    val urgencyThresholds by settingsViewModel.urgencyThresholds.collectAsState()
    val fetchInterval by settingsViewModel.fetchInterval.collectAsState()
    val maxEvents by settingsViewModel.maxEvents.collectAsState()
    val lookahead by settingsViewModel.lookahead.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val reminderMinutes by settingsViewModel.reminderMinutes.collectAsState()
    val notificationSound by settingsViewModel.notificationSound.collectAsState()
    val travelTimeEnabled by settingsViewModel.travelTimeEnabled.collectAsState()

    val calendars by calendarViewModel.calendars.collectAsState()
    val enabledCalendarIds by calendarViewModel.enabledCalendarIds.collectAsState()

    LaunchedEffect(Unit) { calendarViewModel.refreshCalendars() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // -- General --
            item { SectionHeader("General") }

            item {
                SwitchRow(
                    title = "Notification",
                    subtitle = "Show events in notification shade",
                    checked = activeSurface.notificationEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) settingsViewModel.enableNotification()
                        else settingsViewModel.tryDisableNotification()
                    },
                )
            }

            item {
                SwitchRow(
                    title = "Widget",
                    subtitle = "Show events on home screen",
                    checked = activeSurface.widgetEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) settingsViewModel.enableWidget()
                        else settingsViewModel.tryDisableWidget()
                    },
                )
            }

            item {
                DropdownRow(
                    title = "Background Mode",
                    selectedLabel = backgroundMode.label,
                    options = BackgroundMode.entries.map { it.label },
                    onSelect = { index ->
                        settingsViewModel.setBackgroundMode(BackgroundMode.entries[index])
                    },
                )
            }

            item {
                SwitchRow(
                    title = "Privacy Mode",
                    subtitle = "Hide event details on lock screen",
                    checked = privacyMode,
                    onCheckedChange = { settingsViewModel.setPrivacyMode(it) },
                )
            }

            // -- Display --
            item { SectionHeader("Display") }

            item {
                RadioGroupSection(
                    title = "Time Display Format",
                    options = TimeDisplayFormat.entries.map { it.label },
                    selectedIndex = TimeDisplayFormat.entries.indexOf(timeDisplayFormat),
                    onSelect = { index ->
                        settingsViewModel.setTimeDisplayFormat(TimeDisplayFormat.entries[index])
                    },
                )
            }

            if (timeDisplayFormat == TimeDisplayFormat.COUNTDOWN || timeDisplayFormat == TimeDisplayFormat.BOTH) {
                item {
                    SwitchRow(
                        title = "Natural Language",
                        subtitle = "Show \"in a few minutes\" instead of exact time",
                        checked = useNaturalLanguage,
                        onCheckedChange = { settingsViewModel.setUseNaturalLanguage(it) },
                    )
                }
            }

            item {
                SliderRow(
                    title = "Max Title Length",
                    value = titleMaxLength.toFloat(),
                    valueRange = 0f..50f,
                    steps = 49,
                    valueLabel = if (titleMaxLength == 0) "Unlimited" else "$titleMaxLength chars",
                    onValueChange = { settingsViewModel.setTitleMaxLength(it.roundToInt()) },
                )
            }

            item {
                SwitchRow(
                    title = "Hide All-Day Events",
                    subtitle = "Don't show events without a specific time",
                    checked = hideAllDayEvents,
                    onCheckedChange = { settingsViewModel.setHideAllDayEvents(it) },
                )
            }

            // -- Notifications --
            item { SectionHeader("Notifications") }

            item {
                SwitchRow(
                    title = "Enable Notifications",
                    subtitle = "Get reminders before events start",
                    checked = notificationsEnabled,
                    onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) },
                )
            }

            if (notificationsEnabled) {
                item {
                    val reminderOptions = listOf(1, 2, 3, 5, 10, 15, 30)
                    DropdownRow(
                        title = "Reminder",
                        selectedLabel = "$reminderMinutes min before",
                        options = reminderOptions.map { "$it min" },
                        onSelect = { index ->
                            settingsViewModel.setReminderMinutes(reminderOptions[index])
                        },
                    )
                }

                item {
                    val soundOptions = listOf("default", "subtle", "urgent", "none")
                    val soundLabels = listOf("Default", "Subtle", "Urgent", "None")
                    DropdownRow(
                        title = "Notification Sound",
                        selectedLabel = soundLabels[soundOptions.indexOf(notificationSound).coerceAtLeast(0)],
                        options = soundLabels,
                        onSelect = { index ->
                            settingsViewModel.setNotificationSound(soundOptions[index])
                        },
                    )
                }
            }

            // -- Calendars --
            item { SectionHeader("Calendars") }

            val grouped = calendars.groupBy { it.providerType }
            val enabledIds = enabledCalendarIds ?: calendars.map { it.id }.toSet()

            CalendarProviderType.entries.forEach { providerType ->
                val cals = grouped[providerType] ?: return@forEach

                item {
                    Text(
                        text = providerType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }

                items(cals, key = { it.id }) { cal ->
                    CalendarRow(
                        calendar = cal,
                        enabled = cal.id in enabledIds,
                        onToggle = { calendarViewModel.toggleCalendar(cal.id, it) },
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onAddAccount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Account")
                }
            }

            // -- Advanced --
            item { SectionHeader("Advanced") }

            item {
                SliderRow(
                    title = "Imminent Threshold",
                    value = (urgencyThresholds.imminent / 60).toFloat(),
                    valueRange = 1f..15f,
                    steps = 13,
                    valueLabel = "${(urgencyThresholds.imminent / 60).roundToInt()} min",
                    onValueChange = { minutes ->
                        settingsViewModel.setUrgencyThresholds(
                            urgencyThresholds.copy(imminent = minutes.toDouble() * 60),
                        )
                    },
                )
            }

            item {
                SliderRow(
                    title = "Soon Threshold",
                    value = (urgencyThresholds.soon / 60).toFloat(),
                    valueRange = 5f..30f,
                    steps = 24,
                    valueLabel = "${(urgencyThresholds.soon / 60).roundToInt()} min",
                    onValueChange = { minutes ->
                        settingsViewModel.setUrgencyThresholds(
                            urgencyThresholds.copy(soon = minutes.toDouble() * 60),
                        )
                    },
                )
            }

            item {
                SliderRow(
                    title = "Approaching Threshold",
                    value = (urgencyThresholds.approaching / 60).toFloat(),
                    valueRange = 10f..60f,
                    steps = 49,
                    valueLabel = "${(urgencyThresholds.approaching / 60).roundToInt()} min",
                    onValueChange = { minutes ->
                        settingsViewModel.setUrgencyThresholds(
                            urgencyThresholds.copy(approaching = minutes.toDouble() * 60),
                        )
                    },
                )
            }

            item {
                val fetchOptions = listOf(5.0, 10.0, 15.0, 30.0, 60.0)
                val fetchLabels = listOf("5 min", "10 min", "15 min", "30 min", "60 min")
                DropdownRow(
                    title = "Fetch Interval",
                    selectedLabel = fetchLabels[fetchOptions.indexOf(fetchInterval / 60).coerceAtLeast(0)],
                    options = fetchLabels,
                    onSelect = { index ->
                        settingsViewModel.setFetchInterval(fetchOptions[index] * 60)
                    },
                )
            }

            item {
                val maxEventsOptions = listOf(5, 10, 15, 20, 25)
                DropdownRow(
                    title = "Max Events",
                    selectedLabel = "$maxEvents",
                    options = maxEventsOptions.map { "$it" },
                    onSelect = { index ->
                        settingsViewModel.setMaxEvents(maxEventsOptions[index])
                    },
                )
            }

            item {
                val lookaheadOptions = listOf(12.0 * 3600, 24.0 * 3600, 48.0 * 3600, 7.0 * 86400)
                val lookaheadLabels = listOf("12 hours", "24 hours", "48 hours", "7 days")
                DropdownRow(
                    title = "Lookahead",
                    selectedLabel = lookaheadLabels[lookaheadOptions.indexOf(lookahead).coerceAtLeast(0)],
                    options = lookaheadLabels,
                    onSelect = { index ->
                        settingsViewModel.setLookahead(lookaheadOptions[index])
                    },
                )
            }

            item {
                SwitchRow(
                    title = "Travel Time",
                    subtitle = "Account for travel time in event reminders",
                    checked = travelTimeEnabled,
                    onCheckedChange = { settingsViewModel.setTravelTimeEnabled(it) },
                )
            }

            // -- About --
            item { SectionHeader("About") }

            item {
                SettingsRow(
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                )
            }

            item {
                SettingsRow(title = "Check for Updates")
            }

            item {
                SettingsRow(title = "Licenses")
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DropdownRow(
    title: String,
    selectedLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioGroupSection(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        options.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                )
                Spacer(Modifier.width(8.dp))
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

@Composable
private fun CalendarRow(
    calendar: CalendarInfo,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(calendar.color)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = calendar.title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = calendar.source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
