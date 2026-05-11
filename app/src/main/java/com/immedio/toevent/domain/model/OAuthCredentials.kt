package com.immedio.toevent.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OAuthCredentials(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long,  // epoch millis
    val accountId: String,
    val providerType: CalendarProviderType,
) {
    val isExpired: Boolean get() = expiresAt <= System.currentTimeMillis()
    val needsRefresh: Boolean get() = expiresAt <= System.currentTimeMillis() + 5 * 60 * 1000
}
