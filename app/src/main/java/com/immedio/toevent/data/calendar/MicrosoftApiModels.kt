package com.immedio.toevent.data.calendar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MicrosoftCalendarListResponse(
    val value: List<MicrosoftCalendarEntry> = emptyList(),
)

@Serializable
data class MicrosoftCalendarEntry(
    val id: String,
    val name: String? = null,
    val color: String? = null,
    val hexColor: String? = null,
    val isDefaultCalendar: Boolean? = null,
)

@Serializable
data class MicrosoftEventListResponse(
    val value: List<MicrosoftEventEntry> = emptyList(),
    @SerialName("@odata.nextLink") val nextLink: String? = null,
)

@Serializable
data class MicrosoftEventEntry(
    val id: String,
    val subject: String? = null,
    val start: MicrosoftDateTimeTimeZone? = null,
    val end: MicrosoftDateTimeTimeZone? = null,
    val isAllDay: Boolean? = null,
    val location: MicrosoftLocation? = null,
    val body: MicrosoftBody? = null,
    val webLink: String? = null,
    val onlineMeeting: MicrosoftOnlineMeeting? = null,
    val onlineMeetingUrl: String? = null,
    val isOnlineMeeting: Boolean? = null,
)

@Serializable
data class MicrosoftDateTimeTimeZone(
    val dateTime: String? = null,
    val timeZone: String? = null,
)

@Serializable
data class MicrosoftLocation(
    val displayName: String? = null,
)

@Serializable
data class MicrosoftBody(
    val content: String? = null,
    val contentType: String? = null,
)

@Serializable
data class MicrosoftOnlineMeeting(
    val joinUrl: String? = null,
)
