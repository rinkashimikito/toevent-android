package com.immedio.toevent.domain.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    val label: String get() = when (this) {
        SYSTEM -> "System default"
        LIGHT -> "Light"
        DARK -> "Dark"
    }
}
