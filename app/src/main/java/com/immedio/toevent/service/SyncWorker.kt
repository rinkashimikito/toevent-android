package com.immedio.toevent.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.manager.CalendarProviderManager
import com.immedio.toevent.widget.NextEventWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val providerManager: CalendarProviderManager,
    private val preferences: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val enabledIds = preferences.enabledCalendarIds.first()
            val lookahead = preferences.lookahead.first()
            val now = System.currentTimeMillis()
            val to = now + (lookahead * 1000).toLong()

            val events = providerManager.fetchAllEvents(now, to, enabledIds)

            // Schedule reminders if enabled
            val notificationsEnabled = preferences.notificationsEnabled.first()
            if (notificationsEnabled) {
                val reminderMinutes = preferences.reminderMinutes.first()
                reminderScheduler.scheduleReminders(events, reminderMinutes)
            }

            // Update widget
            try {
                val widget = NextEventWidget()
                val manager = GlanceAppWidgetManager(applicationContext)
                manager.getGlanceIds(NextEventWidget::class.java).forEach { glanceId ->
                    widget.update(applicationContext, glanceId)
                }
            } catch (_: Exception) { }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
