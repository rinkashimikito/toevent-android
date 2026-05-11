package com.immedio.toevent.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyThresholds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val hasCompletedIntro: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.HAS_COMPLETED_INTRO] ?: false }
    suspend fun setHasCompletedIntro(value: Boolean) { dataStore.edit { it[PreferenceKeys.HAS_COMPLETED_INTRO] = value } }

    val enabledCalendarIds: Flow<Set<String>?> = dataStore.data.map { it[PreferenceKeys.ENABLED_CALENDAR_IDS] }
    suspend fun setEnabledCalendarIds(ids: Set<String>?) {
        dataStore.edit {
            if (ids != null) it[PreferenceKeys.ENABLED_CALENDAR_IDS] = ids
            else it.remove(PreferenceKeys.ENABLED_CALENDAR_IDS)
        }
    }

    val lookahead: Flow<Double> = dataStore.data.map { it[PreferenceKeys.LOOKAHEAD] ?: 604800.0 } // 7 days default
    suspend fun setLookahead(value: Double) { dataStore.edit { it[PreferenceKeys.LOOKAHEAD] = value } }

    val timeDisplayFormat: Flow<TimeDisplayFormat> = dataStore.data.map {
        val raw = it[PreferenceKeys.TIME_DISPLAY_FORMAT]
        raw?.let { r -> runCatching { TimeDisplayFormat.valueOf(r) }.getOrNull() } ?: TimeDisplayFormat.COUNTDOWN
    }
    suspend fun setTimeDisplayFormat(value: TimeDisplayFormat) { dataStore.edit { it[PreferenceKeys.TIME_DISPLAY_FORMAT] = value.name } }

    val useNaturalLanguage: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.USE_NATURAL_LANGUAGE] ?: false }
    suspend fun setUseNaturalLanguage(value: Boolean) { dataStore.edit { it[PreferenceKeys.USE_NATURAL_LANGUAGE] = value } }

    val privacyMode: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.PRIVACY_MODE] ?: false }
    suspend fun setPrivacyMode(value: Boolean) { dataStore.edit { it[PreferenceKeys.PRIVACY_MODE] = value } }

    val hideAllDayEvents: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.HIDE_ALL_DAY_EVENTS] ?: true }
    suspend fun setHideAllDayEvents(value: Boolean) { dataStore.edit { it[PreferenceKeys.HIDE_ALL_DAY_EVENTS] = value } }

    val titleMaxLength: Flow<Int> = dataStore.data.map { it[PreferenceKeys.TITLE_MAX_LENGTH] ?: 20 }
    suspend fun setTitleMaxLength(value: Int) { dataStore.edit { it[PreferenceKeys.TITLE_MAX_LENGTH] = value } }

    val urgencyThresholds: Flow<UrgencyThresholds> = dataStore.data.map {
        UrgencyThresholds(
            imminent = it[PreferenceKeys.URGENCY_IMMINENT] ?: 300.0,
            soon = it[PreferenceKeys.URGENCY_SOON] ?: 900.0,
            approaching = it[PreferenceKeys.URGENCY_APPROACHING] ?: 1800.0,
        )
    }
    suspend fun setUrgencyThresholds(value: UrgencyThresholds) {
        dataStore.edit {
            it[PreferenceKeys.URGENCY_IMMINENT] = value.imminent
            it[PreferenceKeys.URGENCY_SOON] = value.soon
            it[PreferenceKeys.URGENCY_APPROACHING] = value.approaching
        }
    }

    val fetchInterval: Flow<Double> = dataStore.data.map { it[PreferenceKeys.FETCH_INTERVAL] ?: 900.0 }
    suspend fun setFetchInterval(value: Double) { dataStore.edit { it[PreferenceKeys.FETCH_INTERVAL] = value } }

    val maxEvents: Flow<Int> = dataStore.data.map { it[PreferenceKeys.MAX_EVENTS] ?: 10 }
    suspend fun setMaxEvents(value: Int) { dataStore.edit { it[PreferenceKeys.MAX_EVENTS] = value } }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: false }
    suspend fun setNotificationsEnabled(value: Boolean) { dataStore.edit { it[PreferenceKeys.NOTIFICATIONS_ENABLED] = value } }

    val reminderMinutes: Flow<Int> = dataStore.data.map { it[PreferenceKeys.REMINDER_MINUTES] ?: 5 }
    suspend fun setReminderMinutes(value: Int) { dataStore.edit { it[PreferenceKeys.REMINDER_MINUTES] = value } }

    val notificationSound: Flow<String> = dataStore.data.map { it[PreferenceKeys.NOTIFICATION_SOUND] ?: "default" }
    suspend fun setNotificationSound(value: String) { dataStore.edit { it[PreferenceKeys.NOTIFICATION_SOUND] = value } }

    val activeSurface: Flow<ActiveSurface> = dataStore.data.map {
        val raw = it[PreferenceKeys.ACTIVE_SURFACE]
        raw?.let { r -> runCatching { ActiveSurface.valueOf(r) }.getOrNull() } ?: ActiveSurface.NOTIFICATION
    }
    suspend fun setActiveSurface(value: ActiveSurface) { dataStore.edit { it[PreferenceKeys.ACTIVE_SURFACE] = value.name } }

    val backgroundMode: Flow<BackgroundMode> = dataStore.data.map {
        val raw = it[PreferenceKeys.BACKGROUND_MODE]
        raw?.let { r -> runCatching { BackgroundMode.valueOf(r) }.getOrNull() } ?: BackgroundMode.BALANCED
    }
    suspend fun setBackgroundMode(value: BackgroundMode) { dataStore.edit { it[PreferenceKeys.BACKGROUND_MODE] = value.name } }

    val travelTimeEnabled: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.TRAVEL_TIME_ENABLED] ?: false }
    suspend fun setTravelTimeEnabled(value: Boolean) { dataStore.edit { it[PreferenceKeys.TRAVEL_TIME_ENABLED] = value } }

    val focusFilterCalendars: Flow<Set<String>> = dataStore.data.map { it[PreferenceKeys.FOCUS_FILTER_CALENDARS] ?: emptySet() }
    suspend fun setFocusFilterCalendars(value: Set<String>) { dataStore.edit { it[PreferenceKeys.FOCUS_FILTER_CALENDARS] = value } }

    val floatingChipEnabled: Flow<Boolean> = dataStore.data.map { it[PreferenceKeys.FLOATING_CHIP_ENABLED] ?: false }
    suspend fun setFloatingChipEnabled(value: Boolean) { dataStore.edit { it[PreferenceKeys.FLOATING_CHIP_ENABLED] = value } }
}
