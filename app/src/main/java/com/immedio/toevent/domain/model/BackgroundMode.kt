package com.immedio.toevent.domain.model

enum class BackgroundMode {
    AGGRESSIVE, BALANCED, CONSERVATIVE;

    val syncIntervalMinutes: Long get() = when (this) {
        AGGRESSIVE -> 5
        BALANCED -> 15
        CONSERVATIVE -> 30
    }

    val label: String get() = when (this) {
        AGGRESSIVE -> "Aggressive (higher battery use)"
        BALANCED -> "Balanced (recommended)"
        CONSERVATIVE -> "Conservative (best battery)"
    }
}
