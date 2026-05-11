package com.immedio.toevent.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.UrgencyLevel
import com.immedio.toevent.domain.model.UrgencyThresholds
import com.immedio.toevent.util.DateFormatters
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CountdownService : Service() {

    @Inject lateinit var notificationService: NotificationService

    private val handler = Handler(Looper.getMainLooper())
    private var currentEvent: Event? = null
    private var urgencyThresholds = UrgencyThresholds.DEFAULT
    private var privacyMode = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateCountdown()
            handler.postDelayed(this, currentInterval)
        }
    }

    private val currentInterval: Long
        get() {
            val event = currentEvent ?: return 60_000L
            val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
            return if (remaining in 0.0..300.0) 1_000L else 60_000L
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationService.createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
                val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Event"
                val startDate = intent.getLongExtra(EXTRA_EVENT_START, 0)
                val endDate = intent.getLongExtra(EXTRA_EVENT_END, 0)
                val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)
                val isAllDay = intent.getBooleanExtra(EXTRA_IS_ALL_DAY, false)
                privacyMode = intent.getBooleanExtra(EXTRA_PRIVACY_MODE, false)
                val imminentThreshold = intent.getDoubleExtra(EXTRA_URGENCY_IMMINENT, 300.0)
                val soonThreshold = intent.getDoubleExtra(EXTRA_URGENCY_SOON, 900.0)
                val approachingThreshold = intent.getDoubleExtra(EXTRA_URGENCY_APPROACHING, 1800.0)

                urgencyThresholds = UrgencyThresholds(imminentThreshold, soonThreshold, approachingThreshold)
                currentEvent = Event(
                    id = eventId, title = eventTitle,
                    startDate = startDate, endDate = endDate,
                    isAllDay = isAllDay, calendarColor = 0,
                    calendarId = "", calendarTitle = "",
                    meetingUrl = meetingUrl,
                )

                startForegroundWithNotification()
                handler.removeCallbacks(tickRunnable)
                handler.post(tickRunnable)
            }
            ACTION_UPDATE_EVENT -> {
                val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
                val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: "Event"
                val startDate = intent.getLongExtra(EXTRA_EVENT_START, 0)
                val endDate = intent.getLongExtra(EXTRA_EVENT_END, 0)
                val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)
                val isAllDay = intent.getBooleanExtra(EXTRA_IS_ALL_DAY, false)
                privacyMode = intent.getBooleanExtra(EXTRA_PRIVACY_MODE, false)

                currentEvent = Event(
                    id = eventId, title = eventTitle,
                    startDate = startDate, endDate = endDate,
                    isAllDay = isAllDay, calendarColor = 0,
                    calendarId = "", calendarTitle = "",
                    meetingUrl = meetingUrl,
                )
                updateCountdown()
            }
            ACTION_STOP -> {
                handler.removeCallbacks(tickRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val event = currentEvent ?: return
        val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
        val countdownText = DateFormatters.formatHybridCountdown(remaining)
        val urgency = UrgencyLevel.from(remaining, urgencyThresholds)

        val notification = notificationService.buildCountdownNotification(
            event, countdownText, urgency, privacyMode,
        )

        startForeground(NotificationChannels.COUNTDOWN_NOTIFICATION_ID, notification.build())
    }

    private fun updateCountdown() {
        val event = currentEvent ?: return

        if (event.endDate <= System.currentTimeMillis()) {
            handler.removeCallbacks(tickRunnable)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
        val countdownText = if (remaining <= 0) {
            "Now"
        } else {
            DateFormatters.formatHybridCountdown(remaining)
        }
        val urgency = UrgencyLevel.from(remaining, urgencyThresholds)

        val notification = notificationService.buildCountdownNotification(
            event, countdownText, urgency, privacyMode,
        )

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NotificationChannels.COUNTDOWN_NOTIFICATION_ID, notification.build())
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.immedio.toevent.START_COUNTDOWN"
        const val ACTION_UPDATE_EVENT = "com.immedio.toevent.UPDATE_COUNTDOWN_EVENT"
        const val ACTION_STOP = "com.immedio.toevent.STOP_COUNTDOWN"

        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_EVENT_TITLE = "event_title"
        const val EXTRA_EVENT_START = "event_start"
        const val EXTRA_EVENT_END = "event_end"
        const val EXTRA_MEETING_URL = "meeting_url"
        const val EXTRA_IS_ALL_DAY = "is_all_day"
        const val EXTRA_PRIVACY_MODE = "privacy_mode"
        const val EXTRA_URGENCY_IMMINENT = "urgency_imminent"
        const val EXTRA_URGENCY_SOON = "urgency_soon"
        const val EXTRA_URGENCY_APPROACHING = "urgency_approaching"

        fun startIntent(context: Context, event: Event, privacyMode: Boolean, thresholds: UrgencyThresholds): Intent {
            return Intent(context, CountdownService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_EVENT_ID, event.id)
                putExtra(EXTRA_EVENT_TITLE, event.title)
                putExtra(EXTRA_EVENT_START, event.startDate)
                putExtra(EXTRA_EVENT_END, event.endDate)
                putExtra(EXTRA_MEETING_URL, event.meetingUrl)
                putExtra(EXTRA_IS_ALL_DAY, event.isAllDay)
                putExtra(EXTRA_PRIVACY_MODE, privacyMode)
                putExtra(EXTRA_URGENCY_IMMINENT, thresholds.imminent)
                putExtra(EXTRA_URGENCY_SOON, thresholds.soon)
                putExtra(EXTRA_URGENCY_APPROACHING, thresholds.approaching)
            }
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, CountdownService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
