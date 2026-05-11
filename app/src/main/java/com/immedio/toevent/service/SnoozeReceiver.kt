package com.immedio.toevent.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra("event_id") ?: return
        val snoozeMinutes = intent.getIntExtra("snooze_minutes", 5)

        when (intent.action) {
            "com.immedio.toevent.SNOOZE_EVENT" -> {
                val snoozeIntent = Intent("com.immedio.toevent.COUNTDOWN_SNOOZED").apply {
                    setPackage(context.packageName)
                    putExtra("event_id", eventId)
                    putExtra("snooze_minutes", snoozeMinutes)
                }
                context.sendBroadcast(snoozeIntent)
            }
            "com.immedio.toevent.SNOOZE_REMINDER" -> {
                val eventTitle = intent.getStringExtra("event_title") ?: "Event"
                val eventStart = intent.getLongExtra("event_start", 0)
                val meetingUrl = intent.getStringExtra("event_meeting_url")

                val triggerTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

                val reminderIntent = Intent("com.immedio.toevent.EVENT_REMINDER").apply {
                    setPackage(context.packageName)
                    putExtra("event_id", eventId)
                    putExtra("event_title", eventTitle)
                    putExtra("event_start", eventStart)
                    putExtra("event_meeting_url", meetingUrl)
                    putExtra("minutes_before", snoozeMinutes)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    eventId.hashCode() + snoozeMinutes,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent,
                )
            }
        }
    }
}
