package com.immedio.toevent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.immedio.toevent.domain.model.BackgroundMode
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        // Read background mode from shared prefs file directly (no Hilt in receivers)
        val prefs = context.getSharedPreferences("datastore_boot_cache", Context.MODE_PRIVATE)
        val modeStr = prefs.getString("background_mode", null)
        val mode = modeStr?.let { runCatching { BackgroundMode.valueOf(it) }.getOrNull() } ?: BackgroundMode.BALANCED

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            mode.syncIntervalMinutes, TimeUnit.MINUTES,
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncScheduler.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }
}
