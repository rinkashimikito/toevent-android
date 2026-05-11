package com.immedio.toevent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.immedio.toevent.DISMISS_EVENT") return
        // Send stop command to CountdownService to move to next event
        val stopIntent = Intent(context, CountdownService::class.java).apply {
            action = CountdownService.ACTION_STOP
        }
        context.startService(stopIntent)
    }
}
