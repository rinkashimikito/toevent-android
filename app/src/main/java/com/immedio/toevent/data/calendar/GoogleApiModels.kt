package com.immedio.toevent.data.calendar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoogleCalendarListResponse(
    val items: List<GoogleCalendarEntry> = emptyList(),
)

@Serializable
data class GoogleCalendarEntry(
    val id: String,
    val summary: String? = null,
    val backgroundColor: String? = null,
    val primary: Boolean? = null,
)

@Serializable
data class GoogleEventListResponse(
    val items: List<GoogleEventEntry> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
data class GoogleEventEntry(
    val id: String,
    val summary: String? = null,
    val start: GoogleDateTime? = null,
    val end: GoogleDateTime? = null,
    val location: String? = null,
    val description: String? = null,
    val htmlLink: String? = null,
    @SerialName("hangoutLink") val hangoutLink: String? = null,
    val conferenceData: GoogleConferenceData? = null,
)

@Serializable
data class GoogleDateTime(
    val dateTime: String? = null,
    val date: String? = null,
)

@Serializable
data class GoogleConferenceData(
    val entryPoints: List<GoogleEntryPoint>? = null,
)

@Serializable
data class GoogleEntryPoint(
    val entryPointType: String? = null,
    val uri: String? = null,
)
