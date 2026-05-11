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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.immedio.toevent.BuildConfig
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.TimeDisplayFormat
import kotlin.math.roundToInt

// iOS design constants
private val iOSBackground = Color(0xFFF2F2F7)
private val iOSSurface = Color.White
private val iOSSeparator = Color(0xFFC6C6C8)
private val iOSBlue = Color(0xFF007AFF)
private val iOSGreen = Color(0xFF34C759)
private val iOSGray = Color(0xFF8E8E93)
private val iOSUncheckedTrack = Color(0xFFE5E5EA)

private val sectionCornerRadius = 10.dp
private val rowMinHeight = 44.dp
private val rowHorizontalPadding = 16.dp
private val sectionSpacing = 35.dp
private val separatorThickness = 0.5.dp
private val separatorStartPadding = 16.dp

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iOSBlue,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = iOSBackground,
                ),
            )
        },
        containerColor = iOSBackground,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // -- Appearance --
            item { IOSSectionHeader("APPEARANCE") }
            item {
                IOSSectionCard {
                    IOSNavigationRow(
                        title = "Theme",
                        value = themeMode.label,
                        options = com.immedio.toevent.domain.model.ThemeMode.entries.map { it.label },
                        onSelect = { index ->
                            settingsViewModel.setThemeMode(com.immedio.toevent.domain.model.ThemeMode.entries[index])
                        },
                        showSeparator = false,
                    )
                }
            }

            // -- Display --
            item { IOSSectionHeader("DISPLAY") }
            item {
                IOSSectionCard {
                    IOSToggleRow(
                        title = "Notification",
                        checked = activeSurface.notificationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) settingsViewModel.enableNotification()
                            else settingsViewModel.tryDisableNotification()
                        },
                    )
                    IOSSeparator()
                    IOSToggleRow(
                        title = "Widget",
                        checked = activeSurface.widgetEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) settingsViewModel.enableWidget()
                            else settingsViewModel.tryDisableWidget()
                        },
                    )
                    IOSSeparator()
                    IOSToggleRow(
                        title = "Floating Chip",
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

            item { Spacer(Modifier.height(sectionSpacing)) }
            item {
                IOSSectionCard {
                    IOSNavigationRow(
                        title = "Time Format",
                        value = timeDisplayFormat.label,
                        options = TimeDisplayFormat.entries.map { it.label },
                        onSelect = { index ->
                            settingsViewModel.setTimeDisplayFormat(TimeDisplayFormat.entries[index])
                        },
                        showSeparator = false,
                    )
                }
            }

            if (timeDisplayFormat == TimeDisplayFormat.COUNTDOWN || timeDisplayFormat == TimeDisplayFormat.BOTH) {
                item { Spacer(Modifier.height(sectionSpacing)) }
                item {
                    IOSSectionCard {
                        IOSToggleRow(
                            title = "Natural Language",
                            checked = useNaturalLanguage,
                            onCheckedChange = { settingsViewModel.setUseNaturalLanguage(it) },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(sectionSpacing)) }
            item {
                IOSSectionCard {
                    IOSToggleRow(
                        title = "Privacy Mode",
                        checked = privacyMode,
                        onCheckedChange = { settingsViewModel.setPrivacyMode(it) },
                    )
                    IOSSeparator()
                    IOSToggleRow(
                        title = "Hide All-Day Events",
                        checked = hideAllDayEvents,
                        onCheckedChange = { settingsViewModel.setHideAllDayEvents(it) },
                    )
                }
            }

            // -- Notifications --
            item { IOSSectionHeader("NOTIFICATIONS") }
            item {
                IOSSectionCard {
                    IOSToggleRow(
                        title = "Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) },
                    )
                    if (notificationsEnabled) {
                        IOSSeparator()
                        val reminderOptions = listOf(1, 2, 3, 5, 10, 15, 30)
                        IOSNavigationRow(
                            title = "Reminder",
                            value = "$reminderMinutes min before",
                            options = reminderOptions.map { "$it min" },
                            onSelect = { index ->
                                settingsViewModel.setReminderMinutes(reminderOptions[index])
                            },
                        )
                        IOSSeparator()
                        val soundOptions = listOf("default", "subtle", "urgent", "none")
                        val soundLabels = listOf("Default", "Subtle", "Urgent", "None")
                        IOSNavigationRow(
                            title = "Sound",
                            value = soundLabels[soundOptions.indexOf(notificationSound).coerceAtLeast(0)],
                            options = soundLabels,
                            onSelect = { index ->
                                settingsViewModel.setNotificationSound(soundOptions[index])
                            },
                            showSeparator = false,
                        )
                    }
                }
            }

            // -- Calendars --
            item { IOSSectionHeader("CALENDARS") }

            val grouped = calendars.groupBy { it.providerType }
            val enabledIds = enabledCalendarIds ?: calendars.map { it.id }.toSet()

            CalendarProviderType.entries.forEach { providerType ->
                val cals = grouped[providerType] ?: return@forEach

                item(key = "header_${providerType.name}") {
                    Text(
                        text = providerType.displayName.uppercase(),
                        fontSize = 13.sp,
                        color = iOSGray,
                        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 8.dp),
                    )
                }

                item(key = "card_${providerType.name}") {
                    IOSSectionCard {
                        cals.forEachIndexed { index, cal ->
                            IOSCalendarRow(
                                calendar = cal,
                                enabled = cal.id in enabledIds,
                                onToggle = { calendarViewModel.toggleCalendar(cal.id, it) },
                            )
                            if (index < cals.size - 1) {
                                IOSSeparator()
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(sectionCornerRadius))
                        .background(iOSSurface)
                        .clickable { onAddAccount() }
                        .defaultMinSize(minHeight = rowMinHeight)
                        .padding(horizontal = rowHorizontalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Add Account",
                        color = iOSBlue,
                        fontSize = 17.sp,
                    )
                }
            }

            // -- Sync --
            item { IOSSectionHeader("SYNC") }
            item {
                IOSSectionCard {
                    IOSNavigationRow(
                        title = "Background Mode",
                        value = backgroundMode.label,
                        options = BackgroundMode.entries.map { it.label },
                        onSelect = { index ->
                            settingsViewModel.setBackgroundMode(BackgroundMode.entries[index])
                        },
                    )
                    IOSSeparator()
                    val lookaheadOptions = listOf(12.0 * 3600, 24.0 * 3600, 48.0 * 3600, 7.0 * 86400)
                    val lookaheadLabels = listOf("12 hours", "24 hours", "48 hours", "7 days")
                    IOSNavigationRow(
                        title = "Lookahead",
                        value = lookaheadLabels[lookaheadOptions.indexOf(lookahead).coerceAtLeast(0)],
                        options = lookaheadLabels,
                        onSelect = { index ->
                            settingsViewModel.setLookahead(lookaheadOptions[index])
                        },
                        showSeparator = false,
                    )
                }
            }

            // -- Advanced --
            item { IOSSectionHeader("ADVANCED") }
            item {
                IOSSectionCard {
                    IOSSliderRow(
                        title = "Imminent",
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
                    IOSSeparator()
                    IOSSliderRow(
                        title = "Soon",
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
                    IOSSeparator()
                    IOSSliderRow(
                        title = "Approaching",
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

            item { Spacer(Modifier.height(sectionSpacing)) }
            item {
                IOSSectionCard {
                    val maxEventsOptions = listOf(5, 10, 15, 20, 25)
                    IOSNavigationRow(
                        title = "Max Events",
                        value = "$maxEvents",
                        options = maxEventsOptions.map { "$it" },
                        onSelect = { index ->
                            settingsViewModel.setMaxEvents(maxEventsOptions[index])
                        },
                    )
                    IOSSeparator()
                    IOSSliderRow(
                        title = "Max Title Length",
                        value = titleMaxLength.toFloat(),
                        valueRange = 0f..50f,
                        steps = 49,
                        valueLabel = if (titleMaxLength == 0) "Unlimited" else "$titleMaxLength chars",
                        onValueChange = { settingsViewModel.setTitleMaxLength(it.roundToInt()) },
                    )
                    IOSSeparator()
                    IOSToggleRow(
                        title = "Travel Time",
                        checked = travelTimeEnabled,
                        onCheckedChange = { settingsViewModel.setTravelTimeEnabled(it) },
                    )
                }
            }

            // -- About --
            item { IOSSectionHeader("ABOUT") }
            item {
                IOSSectionCard {
                    IOSInfoRow(title = "Version", value = BuildConfig.VERSION_NAME)
                    IOSSeparator()
                    IOSInfoRow(title = "Check for Updates")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// -- iOS-style primitives --

@Composable
private fun IOSSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = iOSGray,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 24.dp),
    )
}

@Composable
private fun IOSSectionCard(
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(sectionCornerRadius),
        color = iOSSurface,
    ) {
        Column { content() }
    }
}

@Composable
private fun IOSSeparator() {
    HorizontalDivider(
        thickness = separatorThickness,
        color = iOSSeparator,
        modifier = Modifier.padding(start = separatorStartPadding),
    )
}

@Composable
private fun IOSToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = rowMinHeight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = iOSUncheckedTrack,
                uncheckedBorderColor = iOSUncheckedTrack,
            ),
        )
    }
}

@Composable
private fun IOSInfoRow(
    title: String,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = rowMinHeight)
            .padding(horizontal = rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                fontSize = 17.sp,
                color = iOSGray,
            )
        }
    }
}

@Composable
private fun IOSNavigationRow(
    title: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    showSeparator: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = rowMinHeight)
            .clickable { showDialog = true }
            .padding(horizontal = rowHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 17.sp,
            color = iOSGray,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = iOSSeparator,
            modifier = Modifier.size(20.dp),
        )
    }

    if (showDialog) {
        IOSPickerDialog(
            title = title,
            options = options,
            selectedOption = value,
            onSelect = { index ->
                onSelect(index)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun IOSPickerDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = rowMinHeight)
                            .clickable { onSelect(index) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option,
                            fontSize = 17.sp,
                            color = if (option == selectedOption) iOSBlue else Color.Unspecified,
                            modifier = Modifier.weight(1f),
                        )
                        if (option == selectedOption) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (index < options.size - 1) {
                        HorizontalDivider(
                            thickness = separatorThickness,
                            color = iOSSeparator,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = iOSBlue)
            }
        },
        containerColor = iOSSurface,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun IOSSliderRow(
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
            .padding(horizontal = rowHorizontalPadding, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
            )
            Text(
                text = valueLabel,
                fontSize = 15.sp,
                color = iOSGray,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = iOSBlue,
                inactiveTrackColor = iOSUncheckedTrack,
            ),
        )
    }
}

@Composable
private fun IOSCalendarRow(
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
            .defaultMinSize(minHeight = rowMinHeight)
            .clickable { onToggle(!enabled) }
            .padding(horizontal = rowHorizontalPadding),
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
            Text(
                text = calendar.title,
                fontSize = 17.sp,
            )
            Text(
                text = calendar.source,
                fontSize = 13.sp,
                color = iOSGray,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iOSGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = iOSUncheckedTrack,
                uncheckedBorderColor = iOSUncheckedTrack,
            ),
        )
    }
}
