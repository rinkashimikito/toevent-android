package com.immedio.toevent.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.immedio.toevent.domain.model.ActiveSurface

private const val PAGE_COUNT = 5

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
                .padding(innerPadding),
        ) {
            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (slideInHorizontally { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                },
                label = "onboarding_page",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage()
                    1 -> CalendarPermissionPage(
                        granted = calendarGranted,
                        onRequest = {
                            calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
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
                    )
                    3 -> SurfaceSelectionPage(
                        selected = selectedSurface,
                        onSelect = { selectedSurface = it },
                    )
                    4 -> CalendarSelectionPage()
                }
            }

            // Bottom section: indicators + button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PageIndicator(currentPage = page, pageCount = PAGE_COUNT)

                Spacer(Modifier.height(24.dp))

                val isLastPage = page == PAGE_COUNT - 1
                val buttonText = when (page) {
                    0 -> "Get Started"
                    1 -> if (calendarGranted) "Continue" else "Grant Calendar Access"
                    2 -> if (notificationGranted) "Continue" else "Allow Notifications"
                    3 -> "Continue"
                    else -> "Finish Setup"
                }

                Button(
                    onClick = {
                        if (isLastPage) {
                            onComplete(selectedSurface)
                        } else {
                            page++
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(buttonText, style = MaterialTheme.typography.titleMedium)
                }

                if (page in 1..2) {
                    val skipGranted = (page == 1 && calendarGranted) || (page == 2 && notificationGranted)
                    if (!skipGranted) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { page++ }) {
                            Text("Skip")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(currentPage: Int, pageCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

// ── Pages ──

@Composable
private fun WelcomePage() {
    OnboardingPageLayout(
        icon = Icons.Default.CalendarMonth,
        title = "ToEvent",
        description = "Your next event, always visible.\nNever miss a meeting again.",
    )
}

@Composable
private fun CalendarPermissionPage(granted: Boolean, onRequest: () -> Unit) {
    OnboardingPageLayout(
        icon = Icons.Default.CalendarMonth,
        title = "Calendar Access",
        description = "ToEvent reads your calendar to show upcoming events. No data leaves your device.",
        statusIcon = if (granted) Icons.Default.CheckCircle else null,
        statusText = if (granted) "Permission granted" else null,
        secondaryAction = if (!granted) ({ onRequest() }) else null,
        secondaryLabel = "Grant Access",
    )
}

@Composable
private fun NotificationPermissionPage(granted: Boolean, onRequest: () -> Unit) {
    OnboardingPageLayout(
        icon = Icons.Default.Notifications,
        title = "Notifications",
        description = "Persistent notification shows your next event countdown. You can also use a widget instead.",
        statusIcon = if (granted) Icons.Default.CheckCircle else null,
        statusText = if (granted) "Permission granted" else null,
        secondaryAction = if (!granted) ({ onRequest() }) else null,
        secondaryLabel = "Allow Notifications",
    )
}

@Composable
private fun SurfaceSelectionPage(
    selected: ActiveSurface,
    onSelect: (ActiveSurface) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Dashboard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Choose Display",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Where should your countdown appear?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        ActiveSurface.entries.forEach { surface ->
            val isSelected = selected == surface
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val icon = when (surface) {
                ActiveSurface.NOTIFICATION -> Icons.Default.Notifications
                ActiveSurface.WIDGET -> Icons.Default.Widgets
                ActiveSurface.BOTH -> Icons.Default.Dashboard
            }
            val label = when (surface) {
                ActiveSurface.NOTIFICATION -> "Notification"
                ActiveSurface.WIDGET -> "Widget"
                ActiveSurface.BOTH -> "Both"
            }
            val subtitle = when (surface) {
                ActiveSurface.NOTIFICATION -> "Persistent in notification shade"
                ActiveSurface.WIDGET -> "Home screen widget"
                ActiveSurface.BOTH -> "Notification + widget"
            }

            Surface(
                onClick = { onSelect(surface) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                tonalElevation = if (isSelected) 2.dp else 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor.copy(alpha = 0.7f),
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSelectionPage() {
    OnboardingPageLayout(
        icon = Icons.Default.CheckCircle,
        title = "All Set",
        description = "All calendars will be shown by default.\nYou can customize this in Settings.",
    )
}

// ── Shared layout ──

@Composable
private fun OnboardingPageLayout(
    icon: ImageVector,
    title: String,
    description: String,
    statusIcon: ImageVector? = null,
    statusText: String? = null,
    secondaryAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (statusIcon != null && statusText != null) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (secondaryAction != null && secondaryLabel != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = secondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
            ) {
                Text(secondaryLabel)
            }
        }
    }
}
