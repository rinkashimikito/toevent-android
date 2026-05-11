package com.immedio.toevent.data.calendar

import android.graphics.Color
import com.immedio.toevent.data.auth.GoogleAuthService
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

class GoogleCalendarProvider(
    override val account: CalendarAccount,
    private val authService: GoogleAuthService,
) : CalendarProvider {

    override val providerType = CalendarProviderType.GOOGLE

    override val isAuthenticated: Boolean
        get() = authService.getCredentials(account.id) != null

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://www.googleapis.com/calendar/v3"

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private val dateFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun fetchCalendars(): List<CalendarInfo> {
        val token = getValidToken() ?: return emptyList()
        val response = apiGet("$baseUrl/users/me/calendarList", token) ?: return emptyList()
        val parsed = json.decodeFromString<GoogleCalendarListResponse>(response)

        return parsed.items.map { entry ->
            CalendarInfo(
                id = entry.id,
                title = entry.summary ?: entry.id,
                color = parseColor(entry.backgroundColor),
                source = account.email,
                providerType = CalendarProviderType.GOOGLE,
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

    private fun fetchEventsForCalendar(
        calendarId: String,
        from: Long,
        to: Long,
        token: String,
    ): List<Event> {
        val events = mutableListOf<Event>()
        var pageToken: String? = null

        do {
            val url = buildString {
                append("$baseUrl/calendars/${URLEncoder.encode(calendarId, "UTF-8")}/events")
                append("?timeMin=${URLEncoder.encode(isoFormat.format(from), "UTF-8")}")
                append("&timeMax=${URLEncoder.encode(isoFormat.format(to), "UTF-8")}")
                append("&singleEvents=true")
                append("&orderBy=startTime")
                append("&maxResults=250")
                if (pageToken != null) append("&pageToken=$pageToken")
            }

            val response = apiGet(url, token) ?: break
            val parsed = json.decodeFromString<GoogleEventListResponse>(response)

            for (entry in parsed.items) {
                events.add(mapToEvent(entry, calendarId))
            }

            pageToken = parsed.nextPageToken
        } while (pageToken != null)

        return events
    }

    @Suppress("MagicNumber")
    private fun mapToEvent(entry: GoogleEventEntry, calendarId: String): Event {
        val isAllDay = entry.start?.date != null
        val startMs = if (isAllDay) {
            dateFormat.parse(entry.start?.date ?: "")?.time ?: 0L
        } else {
            parseIsoDate(entry.start?.dateTime) ?: 0L
        }
        val endMs = if (isAllDay) {
            dateFormat.parse(entry.end?.date ?: "")?.time ?: (startMs + 86_400_000)
        } else {
            parseIsoDate(entry.end?.dateTime) ?: (startMs + 3_600_000)
        }

        val conferenceUrl = entry.conferenceData?.entryPoints
            ?.firstOrNull { it.entryPointType == "video" }?.uri
        val meetingUrl = conferenceUrl
            ?: entry.hangoutLink
            ?: MeetingUrlParser.findMeetingUrl(
                url = entry.htmlLink,
                location = entry.location,
                notes = entry.description,
            )

        return Event(
            id = "${account.id}_${entry.id}",
            title = entry.summary ?: "Untitled Event",
            startDate = startMs,
            endDate = endMs,
            isAllDay = isAllDay,
            calendarColor = GOOGLE_BLUE,
            calendarId = calendarId,
            calendarTitle = calendarId,
            source = CalendarProviderType.GOOGLE,
            accountId = account.id,
            location = entry.location,
            meetingUrl = meetingUrl,
            notes = entry.description,
            url = entry.htmlLink,
        )
    }

    private fun parseIsoDate(iso: String?): Long? {
        if (iso == null) return null
        return try {
            val normalized = iso.replace("Z", "+00:00")
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(normalized)?.time
        } catch (_: Exception) {
            try {
                isoFormat.parse(iso)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseColor(hex: String?): Int {
        if (hex == null) return GOOGLE_BLUE
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            GOOGLE_BLUE
        }
    }

    override suspend fun signOut() {
        authService.signOut(account.id)
    }

    companion object {
        @Suppress("MagicNumber")
        private val GOOGLE_BLUE = 0xFF4285F4.toInt()
    }
}
