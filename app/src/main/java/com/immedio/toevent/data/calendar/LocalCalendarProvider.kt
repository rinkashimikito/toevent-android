package com.immedio.toevent.data.calendar

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.service.MeetingUrlParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCalendarProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalendarProvider {

    override val providerType = CalendarProviderType.LOCAL
    override val account = CalendarAccount.LOCAL
    override val isAuthenticated = true

    private val contentResolver: ContentResolver get() = context.contentResolver

    override suspend fun fetchCalendars(): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )

        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(0).toString(),
                        title = cursor.getString(1) ?: "Unknown",
                        color = cursor.getInt(2),
                        source = cursor.getString(3) ?: "Unknown",
                        providerType = CalendarProviderType.LOCAL,
                    )
                )
            }
        }

        return calendars
    }

    override suspend fun fetchEvents(from: Long, to: Long, calendarIds: Set<String>?): List<Event> {
        val events = mutableListOf<Event>()
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_COLOR,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.CUSTOM_APP_URI,
        )

        val selection = buildString {
            append("${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?")
            if (!calendarIds.isNullOrEmpty()) {
                append(" AND ${CalendarContract.Events.CALENDAR_ID} IN (${calendarIds.joinToString(",") { "?" }})")
            }
        }

        val selectionArgs = buildList {
            add(from.toString())
            add(to.toString())
            if (!calendarIds.isNullOrEmpty()) addAll(calendarIds)
        }.toTypedArray()

        contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val location = cursor.getStringOrNull(8)
                val notes = cursor.getStringOrNull(9)
                val url = cursor.getStringOrNull(10)

                events.add(
                    Event(
                        id = cursor.getLong(0).toString(),
                        title = cursor.getStringOrNull(1) ?: "Untitled Event",
                        startDate = cursor.getLong(2),
                        endDate = cursor.getLongOrNull(3) ?: (cursor.getLong(2) + 3_600_000),
                        isAllDay = cursor.getInt(4) == 1,
                        calendarColor = cursor.getInt(5),
                        calendarId = cursor.getLong(6).toString(),
                        calendarTitle = cursor.getStringOrNull(7) ?: "Unknown",
                        source = CalendarProviderType.LOCAL,
                        location = location,
                        notes = notes,
                        url = url,
                        meetingUrl = MeetingUrlParser.findMeetingUrl(
                            url = url,
                            location = location,
                            notes = notes,
                        ),
                    )
                )
            }
        }

        return events
    }

    override suspend fun signOut() {
        // Local provider cannot sign out
    }

    private fun Cursor.getStringOrNull(index: Int): String? =
        if (isNull(index)) null else getString(index)

    private fun Cursor.getLongOrNull(index: Int): Long? =
        if (isNull(index)) null else getLong(index)
}
