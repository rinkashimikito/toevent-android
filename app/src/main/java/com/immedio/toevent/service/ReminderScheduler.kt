package com.immedio.toevent.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.immedio.toevent.domain.model.Event
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun scheduleReminder(event: Event, minutesBefore: Int) {
        val triggerTime = event.startDate - minutesBefore * 60 * 1000L
        if (triggerTime <= System.currentTimeMillis()) return
        if (!canScheduleExactAlarms()) return

        val intent = Intent("com.immedio.toevent.EVENT_REMINDER").apply {
            setPackage(context.packageName)
            putExtra("event_id", event.id)
            putExtra("event_title", event.title)
            putExtra("event_start", event.startDate)
            putExtra("event_meeting_url", event.meetingUrl)
            putExtra("minutes_before", minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent,
            )
        } catch (_: SecurityException) { }
    }

    fun cancelReminder(eventId: String) {
        val intent = Intent("com.immedio.toevent.EVENT_REMINDER").apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    fun cancelAllReminders() {
        // AlarmManager doesn't have "cancel all" -- scheduled alarms expire naturally
    }

    fun scheduleReminders(events: List<Event>, minutesBefore: Int) {
        for (event in events) {
            if (!event.isAllDay && event.startDate > System.currentTimeMillis()) {
                scheduleReminder(event, minutesBefore)
            }
        }
    }
}
