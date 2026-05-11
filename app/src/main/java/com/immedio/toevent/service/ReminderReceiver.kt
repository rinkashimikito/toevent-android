package com.immedio.toevent.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.immedio.toevent.EVENT_REMINDER") return

        val eventId = intent.getStringExtra("event_id") ?: return
        val eventTitle = intent.getStringExtra("event_title") ?: "Event"
        val eventStart = intent.getLongExtra("event_start", 0)
        val meetingUrl = intent.getStringExtra("event_meeting_url")
        val minutesBefore = intent.getIntExtra("minutes_before", 5)

        val event = Event(
            id = eventId,
            title = eventTitle,
            startDate = eventStart,
            endDate = eventStart + 3_600_000,
            isAllDay = false,
            calendarColor = 0,
            calendarId = "",
            calendarTitle = "",
            source = CalendarProviderType.LOCAL,
            meetingUrl = meetingUrl,
        )

        val notificationService = NotificationService(context)
        notificationService.createChannels()
        val notification = notificationService.buildReminderNotification(event, minutesBefore, false)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(eventId.hashCode(), notification.build())
    }
}
