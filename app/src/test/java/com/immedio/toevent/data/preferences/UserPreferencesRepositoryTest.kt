package com.immedio.toevent.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyThresholds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UserPreferencesRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: UserPreferencesRepository

    @BeforeEach
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = TestScope(UnconfinedTestDispatcher()),
            produceFile = { File(tempDir, "test_prefs.preferences_pb") },
        )
        repo = UserPreferencesRepository(dataStore)
    }

    @Test
    fun `default active surface is NOTIFICATION`() = runTest {
        assertEquals(ActiveSurface.NOTIFICATION, repo.activeSurface.first())
    }

    @Test
    fun `persists active surface`() = runTest {
        repo.setActiveSurface(ActiveSurface.BOTH)
        assertEquals(ActiveSurface.BOTH, repo.activeSurface.first())
    }

    @Test
    fun `default background mode is BALANCED`() = runTest {
        assertEquals(BackgroundMode.BALANCED, repo.backgroundMode.first())
    }

    @Test
    fun `persists background mode`() = runTest {
        repo.setBackgroundMode(BackgroundMode.AGGRESSIVE)
        assertEquals(BackgroundMode.AGGRESSIVE, repo.backgroundMode.first())
    }

    @Test
    fun `default urgency thresholds`() = runTest {
        val thresholds = repo.urgencyThresholds.first()
        assertEquals(300.0, thresholds.imminent)
        assertEquals(900.0, thresholds.soon)
        assertEquals(1800.0, thresholds.approaching)
    }

    @Test
    fun `persists urgency thresholds`() = runTest {
        val custom = UrgencyThresholds(60.0, 180.0, 600.0)
        repo.setUrgencyThresholds(custom)
        val result = repo.urgencyThresholds.first()
        assertEquals(60.0, result.imminent)
        assertEquals(180.0, result.soon)
        assertEquals(600.0, result.approaching)
    }

    @Test
    fun `default time display format is COUNTDOWN`() = runTest {
        assertEquals(TimeDisplayFormat.COUNTDOWN, repo.timeDisplayFormat.first())
    }

    @Test
    fun `persists time display format`() = runTest {
        repo.setTimeDisplayFormat(TimeDisplayFormat.BOTH)
        assertEquals(TimeDisplayFormat.BOTH, repo.timeDisplayFormat.first())
    }

    @Test
    fun `default lookahead is 86400`() = runTest {
        assertEquals(86400.0, repo.lookahead.first())
    }

    @Test
    fun `default hasCompletedIntro is false`() = runTest {
        assertFalse(repo.hasCompletedIntro.first())
    }

    @Test
    fun `default privacy mode is false`() = runTest {
        assertFalse(repo.privacyMode.first())
    }

    @Test
    fun `persists privacy mode`() = runTest {
        repo.setPrivacyMode(true)
        assertTrue(repo.privacyMode.first())
    }

    @Test
    fun `default enabled calendar ids is null`() = runTest {
        assertNull(repo.enabledCalendarIds.first())
    }

    @Test
    fun `persists enabled calendar ids`() = runTest {
        repo.setEnabledCalendarIds(setOf("cal1", "cal2"))
        assertEquals(setOf("cal1", "cal2"), repo.enabledCalendarIds.first())
    }

    @Test
    fun `clears enabled calendar ids`() = runTest {
        repo.setEnabledCalendarIds(setOf("cal1"))
        repo.setEnabledCalendarIds(null)
        assertNull(repo.enabledCalendarIds.first())
    }

    @Test
    fun `default reminder minutes is 5`() = runTest {
        assertEquals(5, repo.reminderMinutes.first())
    }

    @Test
    fun `default max events is 10`() = runTest {
        assertEquals(10, repo.maxEvents.first())
    }

    @Test
    fun `default travel time enabled is false`() = runTest {
        assertFalse(repo.travelTimeEnabled.first())
    }
}
