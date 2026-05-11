package com.immedio.toevent.data.cache

import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EventCacheServiceTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var service: EventCacheService

    private fun testEvent(id: String): Event = Event(
        id = id, title = "Event $id",
        startDate = 1704067200000L, endDate = 1704070800000L,
        isAllDay = false, calendarColor = 0xFF0000,
        calendarId = "cal1", calendarTitle = "Test Cal",
        source = CalendarProviderType.LOCAL,
    )

    @BeforeEach
    fun setup() {
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.cacheDir } returns tempDir
        service = EventCacheService(context)
    }

    @Test
    fun `caches and retrieves events`() {
        val events = listOf(testEvent("1"), testEvent("2"))
        service.cacheEvents("account1", events)
        val cached = service.getCachedEvents("account1")
        assertNotNull(cached)
        assertEquals(2, cached!!.size)
        assertEquals("1", cached[0].id)
        assertEquals("2", cached[1].id)
    }

    @Test
    fun `returns null when no cache exists`() {
        assertNull(service.getCachedEvents("nonexistent"))
    }

    @Test
    fun `isStale returns true when no cache`() {
        assertTrue(service.isStale("nonexistent"))
    }

    @Test
    fun `isStale returns false for fresh cache`() {
        service.cacheEvents("account1", listOf(testEvent("1")))
        assertTrue(!service.isStale("account1"))
    }

    @Test
    fun `clears cache for account`() {
        service.cacheEvents("account1", listOf(testEvent("1")))
        service.clearCache("account1")
        assertNull(service.getCachedEvents("account1"))
    }

    @Test
    fun `clears all caches`() {
        service.cacheEvents("account1", listOf(testEvent("1")))
        service.cacheEvents("account2", listOf(testEvent("2")))
        service.clearAll()
        assertNull(service.getCachedEvents("account1"))
        assertNull(service.getCachedEvents("account2"))
    }

    @Test
    fun `preserves all event fields through cache round trip`() {
        val event = Event(
            id = "test", title = "Full Event",
            startDate = 1704067200000L, endDate = 1704070800000L,
            isAllDay = false, calendarColor = 0xFF00FF,
            calendarId = "cal1", calendarTitle = "Work",
            source = CalendarProviderType.GOOGLE,
            accountId = "acc1",
            location = "Room 5",
            meetingUrl = "https://meet.google.com/abc-defg-hij",
            notes = "Important meeting",
            url = "https://calendar.google.com/event/123",
        )
        service.cacheEvents("acc1", listOf(event))
        val cached = service.getCachedEvents("acc1")!!.first()
        assertEquals(event, cached)
    }
}
