package com.immedio.toevent.data.calendar

import android.graphics.Color
import com.immedio.toevent.data.auth.OutlookAuthService
import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.service.MeetingUrlParser
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class OutlookCalendarProvider(
    override val account: CalendarAccount,
    private val authService: OutlookAuthService,
) : CalendarProvider {

    override val providerType = CalendarProviderType.OUTLOOK

    override val isAuthenticated: Boolean
        get() = authService.getCredentials(account.id) != null

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://graph.microsoft.com/v1.0"

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val msDateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private suspend fun getValidToken(): String? {
        var creds = authService.getCredentials(account.id) ?: return null
        if (creds.needsRefresh) {
            creds = authService.refreshToken(creds) ?: return null
        }
        return creds.accessToken
    }

    private fun apiGet(url: String, token: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun fetchCalendars(): List<CalendarInfo> {
        val token = getValidToken() ?: return emptyList()
        val response = apiGet("$baseUrl/me/calendars", token) ?: return emptyList()
        val parsed = json.decodeFromString<MicrosoftCalendarListResponse>(response)

        return parsed.value.map { entry ->
            CalendarInfo(
                id = entry.id,
                title = entry.name ?: "Unnamed",
                color = parseColor(entry.hexColor),
                source = account.email,
                providerType = CalendarProviderType.OUTLOOK,
                accountId = account.id,
            )
        }
    }

    override suspend fun fetchEvents(from: Long, to: Long, calendarIds: Set<String>?): List<Event> {
        val token = getValidToken() ?: return emptyList()
        val calendars = calendarIds ?: fetchCalendars().map { it.id }.toSet()
        val allEvents = mutableListOf<Event>()

        for (calId in calendars) {
            allEvents.addAll(fetchEventsForCalendar(calId, from, to, token))
        }

        return allEvents
    }

    private fun fetchEventsForCalendar(calendarId: String, from: Long, to: Long, token: String): List<Event> {
        val events = mutableListOf<Event>()

        val startTime = isoFormat.format(from)
        val endTime = isoFormat.format(to)

        val initialUrl = buildString {
            append("$baseUrl/me/calendars/$calendarId/calendarView")
            append("?startDateTime=${URLEncoder.encode(startTime, "UTF-8")}")
            append("&endDateTime=${URLEncoder.encode(endTime, "UTF-8")}")
            append("&\$orderby=start/dateTime")
            append("&\$top=100")
            append("&\$select=id,subject,start,end,isAllDay,location,body,webLink,onlineMeeting,onlineMeetingUrl,isOnlineMeeting")
        }

        var url: String? = initialUrl

        do {
            val response = apiGet(url!!, token) ?: break
            val parsed = json.decodeFromString<MicrosoftEventListResponse>(response)

            for (entry in parsed.value) {
                events.add(mapToEvent(entry, calendarId))
            }

            url = parsed.nextLink
        } while (url != null)

        return events
    }

    private fun mapToEvent(entry: MicrosoftEventEntry, calendarId: String): Event {
        val startMs = parseMsDateTime(entry.start?.dateTime) ?: 0L
        val endMs = parseMsDateTime(entry.end?.dateTime) ?: (startMs + 3600_000)

        val meetingUrl = entry.onlineMeeting?.joinUrl
            ?: entry.onlineMeetingUrl
            ?: MeetingUrlParser.findMeetingUrl(
                url = entry.webLink,
                location = entry.location?.displayName,
                notes = entry.body?.content,
            )

        return Event(
            id = "${account.id}_${entry.id}",
            title = entry.subject ?: "Untitled Event",
            startDate = startMs,
            endDate = endMs,
            isAllDay = entry.isAllDay == true,
            calendarColor = 0xFF0078D4.toInt(),
            calendarId = calendarId,
            calendarTitle = calendarId,
            source = CalendarProviderType.OUTLOOK,
            accountId = account.id,
            location = entry.location?.displayName,
            meetingUrl = meetingUrl,
            notes = entry.body?.content,
            url = entry.webLink,
        )
    }

    private fun parseMsDateTime(dateTime: String?): Long? {
        if (dateTime == null) return null
        return try {
            msDateFormat.parse(dateTime)?.time
        } catch (e: Exception) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(dateTime)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun parseColor(hex: String?): Int {
        if (hex == null) return 0xFF0078D4.toInt()
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (e: Exception) {
            0xFF0078D4.toInt()
        }
    }

    override suspend fun signOut() {
        authService.signOut(account.id)
    }
}
