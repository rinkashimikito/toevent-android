package com.immedio.toevent.domain.model

enum class ActiveSurface {
    NOTIFICATION, WIDGET, BOTH;

    val notificationEnabled: Boolean get() = this == NOTIFICATION || this == BOTH
    val widgetEnabled: Boolean get() = this == WIDGET || this == BOTH
}
