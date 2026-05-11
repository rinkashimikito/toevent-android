package com.immedio.toevent.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.immedio.toevent.MainActivity
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.util.DateFormatters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class NextEventWidget : GlanceAppWidget() {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val nextEvent = readNextEventFromCache(context)

        provideContent {
            GlanceTheme {
                if (nextEvent != null) {
                    EventContent(nextEvent)
                } else {
                    EmptyContent()
                }
            }
        }
    }

    @Composable
    private fun EventContent(event: Event) {
        val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
        val countdownText = if (remaining <= 0) "Now" else DateFormatters.formatHybridCountdown(remaining)

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                text = event.title,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                maxLines = 1,
            )
            Text(
                text = countdownText,
                style = TextStyle(fontSize = 12.sp),
            )
        }
    }

    @Composable
    private fun EmptyContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ToEvent",
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
            )
            Text(
                text = "No upcoming events",
                style = TextStyle(fontSize = 12.sp),
            )
        }
    }

    private fun readNextEventFromCache(context: Context): Event? {
        val cacheDir = File(context.cacheDir, "events")
        if (!cacheDir.exists()) return null

        val allEvents = mutableListOf<Event>()
        cacheDir.listFiles()?.forEach { file ->
            try {
                val cached = json.decodeFromString<CachedEventsForWidget>(file.readText())
                allEvents.addAll(cached.events)
            } catch (_: Exception) { }
        }

        val now = System.currentTimeMillis()
        return allEvents
            .filter { !it.isAllDay && it.endDate > now }
            .minByOrNull { it.startDate }
    }
}

@Serializable
private data class CachedEventsForWidget(
    val events: List<Event>,
    val timestamp: Long,
)
