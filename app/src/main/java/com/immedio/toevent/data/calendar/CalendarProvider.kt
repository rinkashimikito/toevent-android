package com.immedio.toevent.data.calendar

import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event

interface CalendarProvider {
    val providerType: CalendarProviderType
    val account: CalendarAccount
    val isAuthenticated: Boolean

    suspend fun fetchEvents(from: Long, to: Long, calendarIds: Set<String>? = null): List<Event>
    suspend fun fetchCalendars(): List<CalendarInfo>
    suspend fun signOut()
}
