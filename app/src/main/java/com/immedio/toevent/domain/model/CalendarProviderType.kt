package com.immedio.toevent.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class CalendarProviderType {
    LOCAL, GOOGLE, OUTLOOK;

    val displayName: String get() = when (this) {
        LOCAL -> "Local Calendar"
        GOOGLE -> "Google Calendar"
        OUTLOOK -> "Microsoft Outlook"
    }
}
