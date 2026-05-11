package com.immedio.toevent.util

import java.text.DateFormat
import java.util.Date

object DateFormatters {

    fun formatHybridCountdown(secondsRemaining: Double): String {
        if (secondsRemaining <= 0) return "Now"
        val interval = secondsRemaining.toLong()
        if (interval < 60) return "${interval}s"
        if (interval < 300) {
            val m = interval / 60
            val s = interval % 60
            return "${m}m %02ds".format(s)
        }
        if (interval < 3600) return "${interval / 60}m"
        if (interval < 86400) {
            val h = interval / 3600
            val m = (interval % 3600) / 60
            return if (m == 0L) "${h}h" else "${h}h ${m}m"
        }
        val d = interval / 86400
        val h = (interval % 86400) / 3600
        val m = (interval % 3600) / 60
        return buildString {
            append("${d}d")
            if (h > 0) append(" ${h}h")
            if (m > 0) append(" ${m}m")
        }
    }

    fun formatRelativeTime(secondsRemaining: Double): String {
        if (secondsRemaining <= 0) return "now"
        if (secondsRemaining < 60) return "in 1m"
        val minutes = (secondsRemaining / 60).toLong()
        if (minutes < 60) return "in ${minutes}m"
        val hours = minutes / 60
        return "in ${hours}h"
    }

    fun formatNaturalLanguage(secondsRemaining: Double): String = when {
        secondsRemaining <= 0 -> "now"
        secondsRemaining < 60 -> "now"
        secondsRemaining < 300 -> "very soon"
        secondsRemaining < 900 -> "soon"
        secondsRemaining < 1800 -> "shortly"
        secondsRemaining < 3600 -> "in under an hour"
        secondsRemaining < 7200 -> "in about an hour"
        else -> "in ${(secondsRemaining / 3600).toLong()} hours"
    }

    fun formatAbsoluteTime(epochMillis: Long): String {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMillis))
    }

    fun shouldShowSeconds(secondsRemaining: Double): Boolean {
        return secondsRemaining > 0 && secondsRemaining < 300
    }
}
