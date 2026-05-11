package com.immedio.toevent.ui.settings

import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyThresholds
import io.mockk.coVerify
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var vm: SettingsViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        preferences = mockk(relaxed = true)

        every { preferences.activeSurface } returns flowOf(ActiveSurface.NOTIFICATION)
        every { preferences.backgroundMode } returns flowOf(BackgroundMode.BALANCED)
        every { preferences.timeDisplayFormat } returns flowOf(TimeDisplayFormat.COUNTDOWN)
        every { preferences.useNaturalLanguage } returns flowOf(false)
        every { preferences.privacyMode } returns flowOf(false)
        every { preferences.hideAllDayEvents } returns flowOf(true)
        every { preferences.titleMaxLength } returns flowOf(20)
        every { preferences.urgencyThresholds } returns flowOf(UrgencyThresholds.DEFAULT)
        every { preferences.fetchInterval } returns flowOf(900.0)
        every { preferences.maxEvents } returns flowOf(10)
        every { preferences.lookahead } returns flowOf(86400.0)
        every { preferences.notificationsEnabled } returns flowOf(false)
        every { preferences.reminderMinutes } returns flowOf(5)
        every { preferences.notificationSound } returns flowOf("default")
        every { preferences.travelTimeEnabled } returns flowOf(false)

        vm = SettingsViewModel(preferences)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `tryDisableNotification returns false when notification is only surface`() {
        assertFalse(vm.tryDisableNotification())
    }

    @Test
    fun `tryDisableWidget returns true when notification is active`() {
        assertTrue(vm.tryDisableWidget())
    }

    @Test
    fun `setUrgencyThresholds rejects invalid thresholds`() = runTest {
        val invalid = UrgencyThresholds(600.0, 300.0, 100.0)
        vm.setUrgencyThresholds(invalid)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { preferences.setUrgencyThresholds(any()) }
    }

    @Test
    fun `setUrgencyThresholds accepts valid thresholds`() = runTest {
        val valid = UrgencyThresholds(60.0, 300.0, 900.0)
        vm.setUrgencyThresholds(valid)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { preferences.setUrgencyThresholds(valid) }
    }
}
