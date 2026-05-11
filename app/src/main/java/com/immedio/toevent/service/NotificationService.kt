package com.immedio.toevent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.immedio.toevent.MainActivity
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.UrgencyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

object NotificationChannels {
    const val NEXT_EVENT = "next_event"
    const val EVENT_ALERTS = "event_alerts"
    const val SYNC_STATUS = "sync_status"

    const val COUNTDOWN_NOTIFICATION_ID = 1
}

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createChannels() {
        val nextEvent = NotificationChannel(
            NotificationChannels.NEXT_EVENT,
            "Next Event",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing countdown to your next event"
            setShowBadge(false)
        }

        val eventAlerts = NotificationChannel(
            NotificationChannels.EVENT_ALERTS,
            "Event Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders before events start"
        }

        val syncStatus = NotificationChannel(
            NotificationChannels.SYNC_STATUS,
            "Sync Status",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "Calendar sync status and errors"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(listOf(nextEvent, eventAlerts, syncStatus))
    }

    fun buildCountdownNotification(
        event: Event,
        countdownText: String,
        urgencyLevel: UrgencyLevel,
        privacyMode: Boolean,
    ): NotificationCompat.Builder {
        val title = if (privacyMode) "Busy" else event.title

        // Tap notification -> open system calendar at event time
        val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("content://com.android.calendar/time/${event.startDate}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val openIntent = PendingIntent.getActivity(
            context, 0,
            calendarIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.NEXT_EVENT)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(countdownText)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

        if (event.meetingUrl != null) {
            val joinIntent = PendingIntent.getActivity(
                context, event.id.hashCode() + 3000,
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(event.meetingUrl)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                android.R.drawable.ic_menu_send,
                "Join",
                joinIntent,
            )
        }

        val dismissIntent = PendingIntent.getBroadcast(
            context, event.id.hashCode() + 1000,
            Intent("com.immedio.toevent.DISMISS_EVENT").apply {
                setPackage(context.packageName)
                putExtra("event_id", event.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            dismissIntent,
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context, event.id.hashCode() + 2000,
            Intent("com.immedio.toevent.SNOOZE_EVENT").apply {
                setPackage(context.packageName)
                putExtra("event_id", event.id)
                putExtra("snooze_minutes", 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(
            android.R.drawable.ic_menu_recent_history,
            "Snooze 5m",
            snoozeIntent,
        )

        return builder
    }

    fun buildReminderNotification(
        event: Event,
        minutesBefore: Int,
        privacyMode: Boolean,
    ): NotificationCompat.Builder {
        val title = if (privacyMode) "Event starting soon" else "${event.title} in $minutesBefore min"
        val text = if (privacyMode) "You have an upcoming event" else "Starting at ${formatTime(event.startDate)}"

        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, NotificationChannels.EVENT_ALERTS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        if (event.meetingUrl != null) {
            val joinIntent = PendingIntent.getActivity(
                context, event.id.hashCode() + 4000,
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(event.meetingUrl)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(android.R.drawable.ic_menu_send, "Join", joinIntent)
        }

        val snooze5Intent = PendingIntent.getBroadcast(
            context, event.id.hashCode() + 5000,
            Intent("com.immedio.toevent.SNOOZE_REMINDER").apply {
                setPackage(context.packageName)
                putExtra("event_id", event.id)
                putExtra("snooze_minutes", 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5m", snooze5Intent)

        val snooze10Intent = PendingIntent.getBroadcast(
            context, event.id.hashCode() + 6000,
            Intent("com.immedio.toevent.SNOOZE_REMINDER").apply {
                setPackage(context.packageName)
                putExtra("event_id", event.id)
                putExtra("snooze_minutes", 10)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snooze10Intent)

        return builder
    }

    fun cancelAllReminders() {
        notificationManager.activeNotifications
            .filter { it.id != NotificationChannels.COUNTDOWN_NOTIFICATION_ID }
            .forEach { notificationManager.cancel(it.id) }
    }

    fun cancelCountdown() {
        notificationManager.cancel(NotificationChannels.COUNTDOWN_NOTIFICATION_ID)
    }

    private fun formatTime(epochMillis: Long): String {
        return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(epochMillis))
    }
}
