package com.immedio.toevent.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.immedio.toevent.domain.model.ActiveSurface

@Composable
fun OnboardingScreen(
    onComplete: (ActiveSurface) -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var selectedSurface by remember { mutableStateOf(ActiveSurface.NOTIFICATION) }
    var calendarGranted by remember { mutableStateOf(false) }
    var notificationGranted by remember { mutableStateOf(false) }

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        calendarGranted = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationGranted = granted
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (page) {
                0 -> WelcomePage(onNext = { page = 1 })
                1 -> CalendarPermissionPage(
                    granted = calendarGranted,
                    onRequest = {
                        calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onNext = { page = 2 },
                )
                2 -> NotificationPermissionPage(
                    granted = notificationGranted,
                    onRequest = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            notificationGranted = true
                        }
                    },
                    onNext = { page = 3 },
                )
                3 -> SurfaceSelectionPage(
                    selected = selectedSurface,
                    onSelect = { selectedSurface = it },
                    onNext = { page = 4 },
                )
                4 -> CalendarSelectionPage(
                    onComplete = { onComplete(selectedSurface) },
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    Icon(
        imageVector = Icons.Default.CalendarMonth,
        contentDescription = null,
        modifier = Modifier.size(96.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "ToEvent",
        style = MaterialTheme.typography.headlineLarge,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Your next event, always visible. Never miss a meeting again.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(48.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Get Started")
    }
}

@Composable
private fun CalendarPermissionPage(
    granted: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = "Calendar Access",
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "ToEvent reads your calendar to show upcoming events. " +
            "No data leaves your device.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    if (granted) {
        Text(
            text = "Permission granted",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    } else {
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Grant Calendar Access")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNext) {
            Text("Skip")
        }
    }
}

@Composable
private fun NotificationPermissionPage(
    granted: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = "Notifications",
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Notifications show your next event countdown persistently. " +
            "You can also use a widget as a fallback.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    if (granted) {
        Text(
            text = "Permission granted",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    } else {
        Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
            Text("Allow Notifications")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onNext) {
            Text("Skip (use widget instead)")
        }
    }
}

@Composable
private fun SurfaceSelectionPage(
    selected: ActiveSurface,
    onSelect: (ActiveSurface) -> Unit,
    onNext: () -> Unit,
) {
    Text(
        text = "Choose Display Surface",
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Where should your countdown appear?",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    ActiveSurface.entries.forEach { surface ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected == surface,
                onClick = { onSelect(surface) },
            )
            Text(
                text = when (surface) {
                    ActiveSurface.NOTIFICATION -> "Notification"
                    ActiveSurface.WIDGET -> "Widget"
                    ActiveSurface.BOTH -> "Both"
                },
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Continue")
    }
}

@Composable
private fun CalendarSelectionPage(
    onComplete: () -> Unit,
) {
    Text(
        text = "Choose Your Calendars",
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "All calendars will be shown. You can customize this in Settings.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
        Text("Finish Setup")
    }
}
