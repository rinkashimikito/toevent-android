package com.immedio.toevent.ui.events

import app.cash.turbine.test
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.manager.CalendarProviderManager
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyLevel
import com.immedio.toevent.domain.model.UrgencyThresholds
import com.immedio.toevent.service.ReminderScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var providerManager: CalendarProviderManager
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var reminderScheduler: ReminderScheduler

    private fun testEvent(id: String, startOffset: Long = 600_000, allDay: Boolean = false): Event {
        val now = System.currentTimeMillis()
        return Event(
            id = id,
            title = "Event $id",
            startDate = now + startOffset,
            endDate = now + startOffset + 3600_000,
            isAllDay = allDay,
            calendarColor = 0,
            calendarId = "cal1",
            calendarTitle = "Work",
            source = CalendarProviderType.LOCAL,
        )
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        providerManager = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        reminderScheduler = mockk(relaxed = true)

        every { preferences.enabledCalendarIds } returns flowOf(null)
        every { preferences.lookahead } returns flowOf(86400.0)
        every { preferences.timeDisplayFormat } returns flowOf(TimeDisplayFormat.COUNTDOWN)
        every { preferences.useNaturalLanguage } returns flowOf(false)
        every { preferences.privacyMode } returns flowOf(false)
        every { preferences.hideAllDayEvents } returns flowOf(true)
        every { preferences.titleMaxLength } returns flowOf(20)
        every { preferences.urgencyThresholds } returns flowOf(UrgencyThresholds.DEFAULT)
        every { preferences.maxEvents } returns flowOf(10)
        every { preferences.activeSurface } returns flowOf(ActiveSurface.NOTIFICATION)
        every { preferences.focusFilterCalendars } returns flowOf(emptySet())
        every { preferences.notificationsEnabled } returns flowOf(false)
        every { preferences.reminderMinutes } returns flowOf(5)
        every { preferences.backgroundMode } returns flowOf(BackgroundMode.BALANCED)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nextEvent is first non-all-day event`() = runTest {
        val vm = MainViewModel(providerManager, preferences, reminderScheduler)
        val allDay = testEvent("ad", allDay = true)
        val timed = testEvent("t1")
        coEvery { providerManager.fetchAllEvents(any(), any(), any()) } returns listOf(allDay, timed)

        vm.refreshEvents()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.nextEvent.test {
            val event = awaitItem()
            assertEquals("t1", event?.id)
        }
    }

    @Test
    fun `nextEvent is null when no events`() = runTest {
        val vm = MainViewModel(providerManager, preferences, reminderScheduler)
        coEvery { providerManager.fetchAllEvents(any(), any(), any()) } returns emptyList()

        vm.refreshEvents()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.nextEvent.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `urgency level computed from next event`() = runTest {
        val vm = MainViewModel(providerManager, preferences, reminderScheduler)
        val event = testEvent("1", startOffset = 60_000)
        coEvery { providerManager.fetchAllEvents(any(), any(), any()) } returns listOf(event)

        vm.refreshEvents()
        testDispatcher.scheduler.advanceUntilIdle()

        vm.urgencyLevel.test {
            val level = awaitItem()
            assertEquals(UrgencyLevel.IMMINENT, level)
        }
    }
}
