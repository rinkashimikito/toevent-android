package com.immedio.toevent.domain.model

data class CalendarInfo(
    val id: String,
    val title: String,
    val color: Int,
    val source: String,
    val providerType: CalendarProviderType = CalendarProviderType.LOCAL,
    val accountId: String? = null,
)
