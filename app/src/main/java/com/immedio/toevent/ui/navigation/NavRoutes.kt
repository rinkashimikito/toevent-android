package com.immedio.toevent.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable data object Onboarding : NavRoute
    @Serializable data object EventList : NavRoute
    @Serializable data object Settings : NavRoute
    @Serializable data class EventDetail(val eventId: String) : NavRoute
    @Serializable data object AddAccount : NavRoute
}
