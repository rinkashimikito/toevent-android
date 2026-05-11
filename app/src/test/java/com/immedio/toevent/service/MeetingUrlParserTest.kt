package com.immedio.toevent.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MeetingUrlParserTest {

    @Test fun `finds zoom url`() {
        val text = "Join at https://us02web.zoom.us/j/1234567890?pwd=abc"
        val result = MeetingUrlParser.findMeetingUrl(text)
        assertNotNull(result)
        assertTrue(result!!.contains("zoom.us"))
    }

    @Test fun `finds google meet url`() {
        val text = "Meet link: https://meet.google.com/abc-defg-hij"
        val result = MeetingUrlParser.findMeetingUrl(text)
        assertNotNull(result)
        assertTrue(result!!.contains("meet.google.com"))
    }

    @Test fun `finds teams url`() {
        val text = "https://teams.microsoft.com/l/meetup-join/something"
        assertNotNull(MeetingUrlParser.findMeetingUrl(text))
    }

    @Test fun `finds webex url`() {
        val text = "https://company.webex.com/meet/user"
        assertNotNull(MeetingUrlParser.findMeetingUrl(text))
    }

    @Test fun `returns null for no meeting url`() {
        assertNull(MeetingUrlParser.findMeetingUrl("Just a normal description"))
    }

    @Test fun `returns null for null input`() {
        assertNull(MeetingUrlParser.findMeetingUrl(null as String?))
    }

    @Test fun `returns null for empty string`() {
        assertNull(MeetingUrlParser.findMeetingUrl(""))
    }

    @Test fun `multi-source searches url first`() {
        val result = MeetingUrlParser.findMeetingUrl(
            url = "https://meet.google.com/abc-defg-hij",
            location = "https://zoom.us/j/123",
            notes = null,
        )
        assertTrue(result!!.contains("meet.google.com"))
    }

    @Test fun `multi-source falls back to location`() {
        val result = MeetingUrlParser.findMeetingUrl(
            url = null,
            location = "Room 5 https://zoom.us/j/123",
            notes = null,
        )
        assertTrue(result!!.contains("zoom.us"))
    }

    @Test fun `multi-source falls back to notes`() {
        val result = MeetingUrlParser.findMeetingUrl(
            url = null,
            location = "Room 5",
            notes = "Join: https://meet.google.com/abc-defg-hij",
        )
        assertTrue(result!!.contains("meet.google.com"))
    }

    @Test fun `isMeetingUrl returns true for zoom`() {
        assertTrue(MeetingUrlParser.isMeetingUrl("https://us02web.zoom.us/j/123"))
    }

    @Test fun `isMeetingUrl returns false for regular url`() {
        assertFalse(MeetingUrlParser.isMeetingUrl("https://google.com"))
    }
}
