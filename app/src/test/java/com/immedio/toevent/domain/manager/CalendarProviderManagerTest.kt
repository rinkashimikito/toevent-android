package com.immedio.toevent.domain.manager

import com.immedio.toevent.data.auth.CredentialStorage
import com.immedio.toevent.data.auth.GoogleAuthService
import com.immedio.toevent.data.auth.OutlookAuthService
import com.immedio.toevent.data.cache.EventCacheService
import com.immedio.toevent.data.calendar.LocalCalendarProvider
import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CalendarProviderManagerTest {

    private lateinit var localProvider: LocalCalendarProvider
    private lateinit var googleAuthService: GoogleAuthService
    private lateinit var outlookAuthService: OutlookAuthService
    private lateinit var credentialStorage: CredentialStorage
    private lateinit var cacheService: EventCacheService
    private lateinit var manager: CalendarProviderManager

    private fun testEvent(
        id: String,
        startDate: Long,
        source: CalendarProviderType = CalendarProviderType.LOCAL,
    ): Event = Event(
        id = id,
        title = "Event $id",
        startDate = startDate,
        endDate = startDate + 3_600_000,
        isAllDay = false,
        calendarColor = 0,
        calendarId = "cal1",
        calendarTitle = "Cal",
        source = source,
    )

    @BeforeEach
    fun setup() {
        localProvider = mockk(relaxed = true)
        googleAuthService = mockk(relaxed = true)
        outlookAuthService = mockk(relaxed = true)
        credentialStorage = mockk(relaxed = true)
        cacheService = mockk(relaxed = true)

        every { localProvider.providerType } returns CalendarProviderType.LOCAL
        every { localProvider.account } returns CalendarAccount.LOCAL
        every { credentialStorage.listAccountIds() } returns emptySet()

        manager = CalendarProviderManager(
            localProvider,
            googleAuthService,
            outlookAuthService,
            credentialStorage,
            cacheService,
        )
    }

    @Test
    fun `merges events from local provider`() = runTest {
        val events = listOf(testEvent("1", 1000L), testEvent("2", 2000L))
        coEvery { localProvider.fetchEvents(any(), any(), any()) } returns events

        val result = manager.fetchAllEvents(0, 10000, null)
        assertEquals(2, result.size)
    }

    @Test
    fun `sorts merged events by start time`() = runTest {
        val events = listOf(testEvent("2", 2000L), testEvent("1", 1000L))
        coEvery { localProvider.fetchEvents(any(), any(), any()) } returns events

        val result = manager.fetchAllEvents(0, 10000, null)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
    }

    @Test
    fun `deduplicates by source and id`() = runTest {
        val events = listOf(testEvent("1", 1000L), testEvent("1", 1000L))
        coEvery { localProvider.fetchEvents(any(), any(), any()) } returns events

        val result = manager.fetchAllEvents(0, 10000, null)
        assertEquals(1, result.size)
    }

    @Test
    fun `falls back to cache on provider failure`() = runTest {
        coEvery { localProvider.fetchEvents(any(), any(), any()) } throws RuntimeException("Network error")
        coEvery { cacheService.getCachedEvents("local") } returns listOf(testEvent("cached", 1000L))

        val result = manager.fetchAllEvents(0, 10000, null)
        assertEquals(1, result.size)
        assertEquals("cached", result[0].id)
    }

    @Test
    fun `returns empty when provider fails and no cache`() = runTest {
        coEvery { localProvider.fetchEvents(any(), any(), any()) } throws RuntimeException("Error")
        coEvery { cacheService.getCachedEvents("local") } returns null

        val result = manager.fetchAllEvents(0, 10000, null)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `caches events after successful fetch`() = runTest {
        val events = listOf(testEvent("1", 1000L))
        coEvery { localProvider.fetchEvents(any(), any(), any()) } returns events

        manager.fetchAllEvents(0, 10000, null)

        coVerify { cacheService.cacheEvents("local", events) }
    }
}
