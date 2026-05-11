package com.immedio.toevent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.immedio.toevent.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.immedio.toevent.EVENT_REMINDER") return

        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "Event"
        val eventStart = intent.getLongExtra("event_start", 0)
        val meetingUrl = intent.getStringExtra("event_meeting_url")
        val minutesBefore = intent.getIntExtra("minutes_before", 5)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists
        nm.createNotificationChannel(
            NotificationChannel(
                NotificationChannels.EVENT_ALERTS,
                "Event Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Reminders before events start" }
        )

        val title = "$eventTitle in $minutesBefore min"
        val text = if (eventStart > 0) {
            "Starting at ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(eventStart))}"
        } else "Upcoming event"

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

        // Join meeting action
        if (meetingUrl != null) {
            val joinIntent = PendingIntent.getActivity(
                context, eventId.hashCode() + 100,
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(meetingUrl)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(android.R.drawable.ic_menu_send, "Join", joinIntent)
        }

        // Snooze actions with proper extras for SnoozeReceiver
        val snooze5Intent = PendingIntent.getBroadcast(
            context, eventId.hashCode() + 200,
            Intent("com.immedio.toevent.SNOOZE_REMINDER").apply {
                setPackage(context.packageName)
                putExtra("event_id", eventId)
                putExtra("event_title", eventTitle)
                putExtra("event_start", eventStart)
                putExtra("event_meeting_url", meetingUrl)
                putExtra("snooze_minutes", 5)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5m", snooze5Intent)

        val snooze10Intent = PendingIntent.getBroadcast(
            context, eventId.hashCode() + 300,
            Intent("com.immedio.toevent.SNOOZE_REMINDER").apply {
                setPackage(context.packageName)
                putExtra("event_id", eventId)
                putExtra("event_title", eventTitle)
                putExtra("event_start", eventStart)
                putExtra("event_meeting_url", meetingUrl)
                putExtra("snooze_minutes", 10)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snooze10Intent)

        nm.notify(eventId.hashCode(), builder.build())
    }
}
