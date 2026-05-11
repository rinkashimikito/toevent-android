package com.immedio.toevent.domain.model

import androidx.compose.ui.graphics.Color

data class UrgencyThresholds(
    val imminent: Double = 300.0,
    val soon: Double = 900.0,
    val approaching: Double = 1800.0,
) {
    val isValid: Boolean get() = imminent < soon && soon < approaching

    companion object {
        val DEFAULT = UrgencyThresholds()
    }
}

enum class UrgencyLevel {
    NORMAL, APPROACHING, SOON, IMMINENT, NOW;

    fun color(isDark: Boolean): Color = when (this) {
        NORMAL -> if (isDark) Color(0xFFE6E1E5) else Color(0xFF1C1B1F)
        APPROACHING -> if (isDark) Color(0xFFFFD54F) else Color(0xFFFFC107)
        SOON -> if (isDark) Color(0xFFFFB74D) else Color(0xFFFF9800)
        IMMINENT, NOW -> if (isDark) Color(0xFFEF5350) else Color(0xFFF44336)
    }

    companion object {
        fun from(secondsRemaining: Double, thresholds: UrgencyThresholds = UrgencyThresholds.DEFAULT): UrgencyLevel = when {
            secondsRemaining <= 0 -> NOW
            secondsRemaining < thresholds.imminent -> IMMINENT
            secondsRemaining < thresholds.soon -> SOON
            secondsRemaining < thresholds.approaching -> APPROACHING
            else -> NORMAL
        }
    }
}
