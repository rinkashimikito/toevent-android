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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.immedio.toevent.domain.model.ActiveSurface

private val iOSBlue = Color(0xFF007AFF)
private val iOSGray = Color(0xFF8E8E93)
private val iOSGreen = Color(0xFF34C759)
private val iOSInactiveDot = Color(0xFFE5E5EA)
private val iOSBlueTint = Color(0xFFE5F0FF)

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
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

            // Bottom: dots + buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IOSPageIndicator(currentPage = page, pageCount = PAGE_COUNT)

                Spacer(Modifier.height(32.dp))

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
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iOSBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        buttonText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (page in 1..2) {
                    val skipGranted = (page == 1 && calendarGranted) || (page == 2 && notificationGranted)
                    if (!skipGranted) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { page++ }) {
                            Text(
                                "Skip for Now",
                                color = iOSGray,
                                fontSize = 15.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSPageIndicator(currentPage: Int, pageCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) iOSBlue else iOSInactiveDot,
                    ),
            )
        }
    }
}

// -- Pages --

@Composable
private fun WelcomePage() {
    IOSOnboardingPageLayout(
        icon = Icons.Default.CalendarMonth,
        title = "ToEvent",
        description = "Your next event, always visible.\nNever miss a meeting again.",
    )
}

@Composable
private fun CalendarPermissionPage(granted: Boolean, onRequest: () -> Unit) {
    IOSOnboardingPageLayout(
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
    IOSOnboardingPageLayout(
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
            modifier = Modifier.size(100.dp),
            tint = iOSBlue,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Choose Display",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Where should your countdown appear?",
            fontSize = 17.sp,
            color = iOSGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Spacer(Modifier.height(32.dp))

        ActiveSurface.entries.forEach { surface ->
            val isSelected = selected == surface
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

            val borderColor = if (isSelected) iOSBlue else Color(0xFFE5E5EA)
            val bgColor = if (isSelected) iOSBlueTint else Color.White

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelect(surface) },
                shape = RoundedCornerShape(10.dp),
                color = bgColor,
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
                        tint = if (isSelected) iOSBlue else iOSGray,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = iOSGray,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iOSBlue,
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
    IOSOnboardingPageLayout(
        icon = Icons.Default.CheckCircle,
        title = "All Set",
        description = "All calendars will be shown by default.\nYou can customize this in Settings.",
    )
}

// -- Shared layout --

@Composable
private fun IOSOnboardingPageLayout(
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
            modifier = Modifier.size(100.dp),
            tint = iOSBlue,
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            fontSize = 17.sp,
            color = iOSGray,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        if (statusIcon != null && statusText != null) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = iOSGreen,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    fontSize = 15.sp,
                    color = iOSGreen,
                )
            }
        }

        if (secondaryAction != null && secondaryLabel != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = secondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = iOSBlueTint,
                    contentColor = iOSBlue,
                ),
            ) {
                Text(
                    secondaryLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
