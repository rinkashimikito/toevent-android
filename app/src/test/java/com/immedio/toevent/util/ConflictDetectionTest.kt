package com.immedio.toevent.util

import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConflictDetectionTest {

    private fun event(id: String, startMin: Long, endMin: Long, allDay: Boolean = false): Event {
        val baseMs = 1704067200000L // 2024-01-01T00:00:00Z
        return Event(
            id = id, title = "Event $id",
            startDate = baseMs + startMin * 60_000,
            endDate = baseMs + endMin * 60_000,
            isAllDay = allDay, calendarColor = 0,
            calendarId = "cal1", calendarTitle = "Cal",
            source = CalendarProviderType.LOCAL,
        )
    }

    @Test fun `detects overlapping events`() {
        val events = listOf(event("1", 0, 60), event("2", 30, 90))
        assertEquals(setOf("1", "2"), events.conflictingEventIds())
    }

    @Test fun `no conflict for adjacent events`() {
        val events = listOf(event("1", 0, 60), event("2", 60, 120))
        assertTrue(events.conflictingEventIds().isEmpty())
    }

    @Test fun `no conflict for non-overlapping events`() {
        val events = listOf(event("1", 0, 30), event("2", 60, 90))
        assertTrue(events.conflictingEventIds().isEmpty())
    }

    @Test fun `all day events excluded from conflicts`() {
        val events = listOf(event("1", 0, 60), event("2", 0, 1440, allDay = true))
        assertTrue(events.conflictingEventIds().isEmpty())
    }

    @Test fun `multiple conflicts detected`() {
        val events = listOf(event("1", 0, 60), event("2", 30, 90), event("3", 45, 120))
        val ids = events.conflictingEventIds()
        assertEquals(setOf("1", "2", "3"), ids)
    }

    @Test fun `hasConflict returns true for conflicting event`() {
        val e1 = event("1", 0, 60)
        val e2 = event("2", 30, 90)
        val events = listOf(e1, e2)
        assertTrue(events.hasConflict(e1))
    }

    @Test fun `hasConflict returns false for non-conflicting event`() {
        val e1 = event("1", 0, 30)
        val e2 = event("2", 60, 90)
        val events = listOf(e1, e2)
        assertFalse(events.hasConflict(e1))
    }

    @Test fun `empty list has no conflicts`() {
        assertTrue(emptyList<Event>().conflictingEventIds().isEmpty())
    }
}
