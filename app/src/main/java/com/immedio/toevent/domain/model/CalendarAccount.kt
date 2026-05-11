package com.immedio.toevent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarAccount(
    val id: String,
    val providerType: CalendarProviderType,
    val email: String,
    val displayName: String,
) {
    companion object {
        val LOCAL = CalendarAccount("local", CalendarProviderType.LOCAL, "System", "Local Calendars")
    }
}
