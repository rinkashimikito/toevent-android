package com.immedio.toevent.ui.events

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.manager.CalendarProviderManager
import com.immedio.toevent.domain.model.ActiveSurface
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.TimeDisplayFormat
import com.immedio.toevent.domain.model.UrgencyLevel
import com.immedio.toevent.domain.model.UrgencyThresholds
import com.immedio.toevent.service.CalendarContentObserver
import com.immedio.toevent.service.ReminderScheduler
import com.immedio.toevent.service.SyncScheduler
import com.immedio.toevent.util.DateFormatters
import com.immedio.toevent.util.conflictingEventIds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val providerManager: CalendarProviderManager,
    private val preferences: UserPreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val syncScheduler: SyncScheduler,
    private val calendarContentObserver: CalendarContentObserver,
) : ViewModel() {

    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    private val _dismissedEventId = MutableStateFlow<String?>(null)
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val privacyMode: StateFlow<Boolean> = preferences.privacyMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeSurface: StateFlow<ActiveSurface> = preferences.activeSurface
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActiveSurface.NOTIFICATION)

    val urgencyThresholds: StateFlow<UrgencyThresholds> = preferences.urgencyThresholds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UrgencyThresholds.DEFAULT)

    val timeDisplayFormat: StateFlow<TimeDisplayFormat> = preferences.timeDisplayFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeDisplayFormat.COUNTDOWN)

    val useNaturalLanguage: StateFlow<Boolean> = preferences.useNaturalLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hideAllDayEvents: StateFlow<Boolean> = preferences.hideAllDayEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val titleMaxLength: StateFlow<Int> = preferences.titleMaxLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    private val focusFilterCalendars: StateFlow<Set<String>> = preferences.focusFilterCalendars
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val maxEvents: StateFlow<Int> = preferences.maxEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    val events: StateFlow<List<Event>> = combine(
        _allEvents, hideAllDayEvents, focusFilterCalendars, maxEvents,
    ) { allEvents, hideAllDay, focusCalendars, max ->
        var filtered = allEvents
        if (hideAllDay) {
            filtered = filtered.filter { !it.isAllDay }
        }
        if (focusCalendars.isNotEmpty()) {
            filtered = filtered.filter { it.calendarId in focusCalendars }
        }
        filtered.take(max)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextEvent: StateFlow<Event?> = combine(
        _allEvents, _dismissedEventId,
    ) { allEvents, dismissedId ->
        allEvents.firstOrNull { event ->
            !event.isAllDay && event.id != dismissedId
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val countdownText: StateFlow<String> = combine(
        nextEvent, _currentTimeMillis, timeDisplayFormat, useNaturalLanguage,
    ) { event, now, format, natural ->
        if (event == null) return@combine ""
        val remaining = (event.startDate - now) / 1000.0
        formatCountdown(remaining, format, natural, event.startDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val urgencyLevel: StateFlow<UrgencyLevel> = combine(
        nextEvent, _currentTimeMillis, urgencyThresholds,
    ) { event, now, thresholds ->
        if (event == null) return@combine UrgencyLevel.NORMAL
        val remaining = (event.startDate - now) / 1000.0
        UrgencyLevel.from(remaining, thresholds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UrgencyLevel.NORMAL)

    val conflictingEventIds: StateFlow<Set<String>> = _allEvents.combine(_currentTimeMillis) { events, _ ->
        events.conflictingEventIds()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var countdownJob: Job? = null

    init {
        startCountdownTimer()
        viewModelScope.launch {
            preferences.backgroundMode.collect { mode ->
                syncScheduler.schedulePeriodicSync(mode)
            }
        }
        calendarContentObserver.register()
        refreshEvents()
    }

    override fun onCleared() {
        super.onCleared()
        calendarContentObserver.unregister()
    }

    @VisibleForTesting
    internal fun stopCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun startCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()

                val dismissedId = _dismissedEventId.value
                if (dismissedId != null) {
                    val dismissed = _allEvents.value.firstOrNull { it.id == dismissedId }
                    if (dismissed != null && dismissed.endDate <= System.currentTimeMillis()) {
                        _dismissedEventId.value = null
                    }
                }

                val event = nextEvent.value
                val interval = if (event != null) {
                    val remaining = (event.startDate - System.currentTimeMillis()) / 1000.0
                    if (remaining in 0.0..300.0) 1000L else 60_000L
                } else {
                    60_000L
                }

                delay(interval)
            }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val enabledIds = preferences.enabledCalendarIds.first()
                val lookahead = preferences.lookahead.first()
                val now = System.currentTimeMillis()
                val to = now + (lookahead * 1000).toLong()

                val fetchedEvents = providerManager.fetchAllEvents(now, to, enabledIds)
                android.util.Log.d("MainViewModel", "Fetched ${fetchedEvents.size} events, from=$now to=$to enabledIds=$enabledIds")
                _allEvents.value = fetchedEvents

                val notificationsEnabled = preferences.notificationsEnabled.first()
                if (notificationsEnabled) {
                    val reminderMinutes = preferences.reminderMinutes.first()
                    reminderScheduler.scheduleReminders(fetchedEvents, reminderMinutes)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun dismissFromDisplay(event: Event) {
        val hasOther = _allEvents.value.any { !it.isAllDay && it.id != event.id }
        if (!hasOther) return
        _dismissedEventId.value = event.id
    }

    fun isEventActive(event: Event): Boolean {
        val now = System.currentTimeMillis()
        return event.startDate <= now && now < event.endDate
    }

    fun canDismissActiveEvent(): Boolean {
        val current = nextEvent.value ?: return false
        return _allEvents.value.any { !it.isAllDay && it.id != current.id }
    }

    private fun formatCountdown(
        secondsRemaining: Double,
        format: TimeDisplayFormat,
        naturalLanguage: Boolean,
        startDateMs: Long,
    ): String = when (format) {
        TimeDisplayFormat.COUNTDOWN -> {
            if (naturalLanguage) DateFormatters.formatNaturalLanguage(secondsRemaining)
            else DateFormatters.formatHybridCountdown(secondsRemaining)
        }
        TimeDisplayFormat.ABSOLUTE -> DateFormatters.formatAbsoluteTime(startDateMs)
        TimeDisplayFormat.BOTH -> {
            val countdown = if (naturalLanguage) DateFormatters.formatNaturalLanguage(secondsRemaining)
            else DateFormatters.formatHybridCountdown(secondsRemaining)
            val absolute = DateFormatters.formatAbsoluteTime(startDateMs)
            "$countdown ($absolute)"
        }
        TimeDisplayFormat.NATURAL_LANGUAGE -> DateFormatters.formatNaturalLanguage(secondsRemaining)
    }
}
