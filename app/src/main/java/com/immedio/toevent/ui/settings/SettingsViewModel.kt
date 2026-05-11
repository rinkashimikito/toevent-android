package com.immedio.toevent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.BackgroundMode
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyThresholds
import com.immedio.toevent.service.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val activeSurface: StateFlow<ActiveSurface> = preferences.activeSurface
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveSurface.NOTIFICATION)

    val backgroundMode: StateFlow<BackgroundMode> = preferences.backgroundMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundMode.BALANCED)

    val timeDisplayFormat: StateFlow<TimeDisplayFormat> = preferences.timeDisplayFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeDisplayFormat.COUNTDOWN)

    val useNaturalLanguage: StateFlow<Boolean> = preferences.useNaturalLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val privacyMode: StateFlow<Boolean> = preferences.privacyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hideAllDayEvents: StateFlow<Boolean> = preferences.hideAllDayEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val titleMaxLength: StateFlow<Int> = preferences.titleMaxLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val urgencyThresholds: StateFlow<UrgencyThresholds> = preferences.urgencyThresholds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UrgencyThresholds.DEFAULT)

    val fetchInterval: StateFlow<Double> = preferences.fetchInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 900.0)

    val maxEvents: StateFlow<Int> = preferences.maxEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val lookahead: StateFlow<Double> = preferences.lookahead
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 86400.0)

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderMinutes: StateFlow<Int> = preferences.reminderMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 5)

    val notificationSound: StateFlow<String> = preferences.notificationSound
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")

    val travelTimeEnabled: StateFlow<Boolean> = preferences.travelTimeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Set active surface with at-least-one enforcement.
     * Returns false if the change would leave no surface active.
     */
    fun setActiveSurface(value: ActiveSurface): Boolean {
        viewModelScope.launch { preferences.setActiveSurface(value) }
        return true
    }

    /**
     * Try to disable notification surface. Returns false if widget is also off.
     */
    fun tryDisableNotification(): Boolean {
        val current = activeSurface.value
        return when (current) {
            ActiveSurface.BOTH -> {
                viewModelScope.launch { preferences.setActiveSurface(ActiveSurface.WIDGET) }
                true
            }
            ActiveSurface.NOTIFICATION -> false
            ActiveSurface.WIDGET -> true
        }
    }

    /**
     * Try to disable widget surface. Returns false if notification is also off.
     */
    fun tryDisableWidget(): Boolean {
        val current = activeSurface.value
        return when (current) {
            ActiveSurface.BOTH -> {
                viewModelScope.launch { preferences.setActiveSurface(ActiveSurface.NOTIFICATION) }
                true
            }
            ActiveSurface.WIDGET -> false
            ActiveSurface.NOTIFICATION -> true
        }
    }

    fun enableNotification() {
        val current = activeSurface.value
        val new = if (current == ActiveSurface.WIDGET) ActiveSurface.BOTH else ActiveSurface.NOTIFICATION
        viewModelScope.launch { preferences.setActiveSurface(new) }
    }

    fun enableWidget() {
        val current = activeSurface.value
        val new = if (current == ActiveSurface.NOTIFICATION) ActiveSurface.BOTH else ActiveSurface.WIDGET
        viewModelScope.launch { preferences.setActiveSurface(new) }
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        viewModelScope.launch {
            preferences.setBackgroundMode(mode)
            syncScheduler.schedulePeriodicSync(mode)
        }
    }

    fun setTimeDisplayFormat(format: TimeDisplayFormat) {
        viewModelScope.launch { preferences.setTimeDisplayFormat(format) }
    }

    fun setUseNaturalLanguage(value: Boolean) {
        viewModelScope.launch { preferences.setUseNaturalLanguage(value) }
    }

    fun setPrivacyMode(value: Boolean) {
        viewModelScope.launch { preferences.setPrivacyMode(value) }
    }

    fun setHideAllDayEvents(value: Boolean) {
        viewModelScope.launch { preferences.setHideAllDayEvents(value) }
    }

    fun setTitleMaxLength(value: Int) {
        viewModelScope.launch { preferences.setTitleMaxLength(value) }
    }

    fun setUrgencyThresholds(thresholds: UrgencyThresholds) {
        if (!thresholds.isValid) return
        viewModelScope.launch { preferences.setUrgencyThresholds(thresholds) }
    }

    fun setFetchInterval(value: Double) {
        viewModelScope.launch { preferences.setFetchInterval(value) }
    }

    fun setMaxEvents(value: Int) {
        viewModelScope.launch { preferences.setMaxEvents(value) }
    }

    fun setLookahead(value: Double) {
        viewModelScope.launch { preferences.setLookahead(value) }
    }

    fun setNotificationsEnabled(value: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(value) }
    }

    fun setReminderMinutes(value: Int) {
        viewModelScope.launch { preferences.setReminderMinutes(value) }
    }

    fun setNotificationSound(value: String) {
        viewModelScope.launch { preferences.setNotificationSound(value) }
    }

    fun setTravelTimeEnabled(value: Boolean) {
        viewModelScope.launch { preferences.setTravelTimeEnabled(value) }
    }
}
