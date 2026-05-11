package com.immedio.toevent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val startDate: Long,   // epoch millis
    val endDate: Long,     // epoch millis
    val isAllDay: Boolean,
    val calendarColor: Int, // ARGB color int
    val calendarId: String,
    val calendarTitle: String,
    val source: CalendarProviderType = CalendarProviderType.LOCAL,
    val accountId: String? = null,
    val location: String? = null,
    val meetingUrl: String? = null,
    val notes: String? = null,
    val url: String? = null,
)
