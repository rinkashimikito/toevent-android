package com.immedio.toevent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.immedio.toevent.data.preferences.UserPreferencesRepository
import com.immedio.toevent.domain.manager.CalendarProviderManager
import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.OAuthCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val providerManager: CalendarProviderManager,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val calendars: StateFlow<List<CalendarInfo>> = _calendars.asStateFlow()

    val enabledCalendarIds: StateFlow<Set<String>?> = preferences.enabledCalendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val expiredAccounts: List<CalendarAccount>
        get() = providerManager.expiredAccounts

    fun refreshCalendars() {
        viewModelScope.launch {
            _calendars.value = providerManager.fetchAllCalendars()
        }
    }

    fun toggleCalendar(calendarId: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = preferences.enabledCalendarIds.stateIn(viewModelScope, SharingStarted.Eagerly, null).value
            val ids = current?.toMutableSet() ?: _calendars.value.map { it.id }.toMutableSet()
            if (enabled) ids.add(calendarId) else ids.remove(calendarId)
            preferences.setEnabledCalendarIds(ids)
        }
    }

    fun addAccount(credentials: OAuthCredentials) {
        viewModelScope.launch {
            providerManager.addAccount(credentials)
            refreshCalendars()
        }
    }

    fun removeAccount(accountId: String) {
        viewModelScope.launch {
            providerManager.removeAccount(accountId)
            refreshCalendars()
        }
    }
}
