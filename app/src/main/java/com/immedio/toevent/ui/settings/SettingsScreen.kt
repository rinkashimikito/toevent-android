package com.immedio.toevent.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immedio.toevent.BuildConfig
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.TimeDisplayFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val activeSurface by settingsViewModel.activeSurface.collectAsStateWithLifecycle()
    val backgroundMode by settingsViewModel.backgroundMode.collectAsStateWithLifecycle()
    val timeDisplayFormat by settingsViewModel.timeDisplayFormat.collectAsStateWithLifecycle()
    val useNaturalLanguage by settingsViewModel.useNaturalLanguage.collectAsStateWithLifecycle()
    val privacyMode by settingsViewModel.privacyMode.collectAsStateWithLifecycle()
    val hideAllDayEvents by settingsViewModel.hideAllDayEvents.collectAsStateWithLifecycle()
    val titleMaxLength by settingsViewModel.titleMaxLength.collectAsStateWithLifecycle()
    val urgencyThresholds by settingsViewModel.urgencyThresholds.collectAsStateWithLifecycle()
    val maxEvents by settingsViewModel.maxEvents.collectAsStateWithLifecycle()
    val lookahead by settingsViewModel.lookahead.collectAsStateWithLifecycle()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val reminderMinutes by settingsViewModel.reminderMinutes.collectAsStateWithLifecycle()
    val notificationSound by settingsViewModel.notificationSound.collectAsStateWithLifecycle()
    val travelTimeEnabled by settingsViewModel.travelTimeEnabled.collectAsStateWithLifecycle()
    val floatingChipEnabled by settingsViewModel.floatingChipEnabled.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    val calendars by calendarViewModel.calendars.collectAsStateWithLifecycle()
    val enabledCalendarIds by calendarViewModel.enabledCalendarIds.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(Unit) { calendarViewModel.refreshCalendars() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Appearance ──
            item { SectionHeader("Appearance") }

            item {
                SectionCard {
                    DropdownRow(
                        title = "Theme",
                        options = com.immedio.toevent.domain.model.ThemeMode.entries.map { it.label },
                        selectedLabel = themeMode.label,
                        onSelect = { index ->
                            settingsViewModel.setThemeMode(com.immedio.toevent.domain.model.ThemeMode.entries[index])
                        },
                    )
                }
            }

            // ── Display ──
            item { SectionHeader("Display") }

            item {
                SectionCard {
                    SwitchRow(
                        title = "Notification",
                        subtitle = "Show events in notification shade",
                        checked = activeSurface.notificationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) settingsViewModel.enableNotification()
                            else settingsViewModel.tryDisableNotification()
                        },
                    )
                    SwitchRow(
                        title = "Widget",
                        subtitle = "Show events on home screen",
                        checked = activeSurface.widgetEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) settingsViewModel.enableWidget()
                            else settingsViewModel.tryDisableWidget()
                        },
                    )
                    SwitchRow(
                        title = "Floating countdown chip",
                        subtitle = "Overlay countdown on other apps",
                        checked = floatingChipEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                )
                                context.startActivity(intent)
                            } else {
                                settingsViewModel.setFloatingChipEnabled(enabled)
                            }
                        },
                    )
                }
            }

            item {
                SectionCard {
                    RadioGroupRow(
                        title = "Time format",
                        options = TimeDisplayFormat.entries.map { it.label },
                        selectedIndex = TimeDisplayFormat.entries.indexOf(timeDisplayFormat),
                        onSelect = { index ->
                            settingsViewModel.setTimeDisplayFormat(TimeDisplayFormat.entries[index])
                        },
                    )
                }
            }

            if (timeDisplayFormat == TimeDisplayFormat.COUNTDOWN || timeDisplayFormat == TimeDisplayFormat.BOTH) {
                item {
                    SectionCard {
                        SwitchRow(
                            title = "Natural language",
                            subtitle = "Show \"in a few minutes\" instead of exact time",
                            checked = useNaturalLanguage,
                            onCheckedChange = { settingsViewModel.setUseNaturalLanguage(it) },
                        )
                    }
                }
            }

            item {
                SectionCard {
                    SwitchRow(
                        title = "Privacy mode",
                        subtitle = "Hide event details on lock screen",
                        checked = privacyMode,
                        onCheckedChange = { settingsViewModel.setPrivacyMode(it) },
                    )
                    SwitchRow(
                        title = "Hide all-day events",
                        subtitle = "Don't show events without a specific time",
                        checked = hideAllDayEvents,
                        onCheckedChange = { settingsViewModel.setHideAllDayEvents(it) },
                    )
                }
            }

            // ── Notifications ──
            item { SectionHeader("Notifications") }

            item {
                SectionCard {
                    SwitchRow(
                        title = "Enable notifications",
                        subtitle = "Get reminders before events start",
                        checked = notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) },
                    )

                    if (notificationsEnabled) {
                        val reminderOptions = listOf(1, 2, 3, 5, 10, 15, 30)
                        DropdownRow(
                            title = "Reminder time",
                            selectedLabel = "$reminderMinutes min before",
                            options = reminderOptions.map { "$it min" },
                            onSelect = { index ->
                                settingsViewModel.setReminderMinutes(reminderOptions[index])
                            },
                        )
                        val soundOptions = listOf("default", "subtle", "urgent", "none")
                        val soundLabels = listOf("Default", "Subtle", "Urgent", "None")
                        DropdownRow(
                            title = "Sound",
                            selectedLabel = soundLabels[soundOptions.indexOf(notificationSound).coerceAtLeast(0)],
                            options = soundLabels,
                            onSelect = { index ->
                                settingsViewModel.setNotificationSound(soundOptions[index])
                            },
                        )
                    }
                }
            }

            // ── Calendars ──
            item { SectionHeader("Calendars") }

            val grouped = calendars.groupBy { it.providerType }
            val enabledIds = enabledCalendarIds ?: calendars.map { it.id }.toSet()

            CalendarProviderType.entries.forEach { providerType ->
                val cals = grouped[providerType] ?: return@forEach

                item(key = "header_${providerType.name}") {
                    Text(
                        text = providerType.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                    )
                }

                item(key = "card_${providerType.name}") {
                    SectionCard {
                        cals.forEach { cal ->
                            CalendarRow(
                                calendar = cal,
                                enabled = cal.id in enabledIds,
                                onToggle = { calendarViewModel.toggleCalendar(cal.id, it) },
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onAddAccount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Account")
                }
            }

            // ── Sync ──
            item { SectionHeader("Sync") }

            item {
                SectionCard {
                    RadioGroupRow(
                        title = "Background mode",
                        options = BackgroundMode.entries.map { it.label },
                        selectedIndex = BackgroundMode.entries.indexOf(backgroundMode),
                        onSelect = { index ->
                            settingsViewModel.setBackgroundMode(BackgroundMode.entries[index])
                        },
                    )
                }
            }

            item {
                SectionCard {
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
            }

            // ── Advanced ──
            item { SectionHeader("Advanced") }

            item {
                SectionCard {
                    SliderRow(
                        title = "Imminent threshold",
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
                    SliderRow(
                        title = "Soon threshold",
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
                    SliderRow(
                        title = "Approaching threshold",
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
            }

            item {
                SectionCard {
                    val maxEventsOptions = listOf(5, 10, 15, 20, 25)
                    DropdownRow(
                        title = "Max events",
                        selectedLabel = "$maxEvents",
                        options = maxEventsOptions.map { "$it" },
                        onSelect = { index ->
                            settingsViewModel.setMaxEvents(maxEventsOptions[index])
                        },
                    )
                    SliderRow(
                        title = "Max title length",
                        value = titleMaxLength.toFloat(),
                        valueRange = 0f..50f,
                        steps = 49,
                        valueLabel = if (titleMaxLength == 0) "Unlimited" else "$titleMaxLength chars",
                        onValueChange = { settingsViewModel.setTitleMaxLength(it.roundToInt()) },
                    )
                    SwitchRow(
                        title = "Travel time",
                        subtitle = "Account for travel time in event reminders",
                        checked = travelTimeEnabled,
                        onCheckedChange = { settingsViewModel.setTravelTimeEnabled(it) },
                    )
                }
            }

            // ── About ──
            item { SectionHeader("About") }

            item {
                SectionCard {
                    InfoRow(title = "Version", value = BuildConfig.VERSION_NAME)
                    InfoRow(title = "Check for updates")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Primitives ──

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column { content() }
    }
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
            .height(56.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownRow(
    title: String,
    selectedLabel: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(index)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun RadioGroupRow(
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
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        options.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onSelect(index) },
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
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
    val tint by animateColorAsState(
        targetValue = if (enabled) Color(calendar.color) else Color(calendar.color).copy(alpha = 0.4f),
        label = "calColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(tint),
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
