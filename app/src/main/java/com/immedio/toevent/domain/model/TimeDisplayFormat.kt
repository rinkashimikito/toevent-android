package com.immedio.toevent.domain.model

enum class TimeDisplayFormat {
    COUNTDOWN, ABSOLUTE, BOTH, NATURAL_LANGUAGE;

    val label: String get() = when (this) {
        COUNTDOWN -> "Countdown (5m 30s)"
        ABSOLUTE -> "Absolute (2:30 PM)"
        BOTH -> "Both (5m 30s - 2:30 PM)"
        NATURAL_LANGUAGE -> "Natural (soon)"
    }
}
