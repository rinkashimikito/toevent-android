package com.immedio.toevent.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DateFormattersTest {

    @Test fun `hybrid countdown - now when negative`() {
        assertEquals("Now", DateFormatters.formatHybridCountdown(-5.0))
    }

    @Test fun `hybrid countdown - now when zero`() {
        assertEquals("Now", DateFormatters.formatHybridCountdown(0.0))
    }

    @Test fun `hybrid countdown - seconds when under 60`() {
        assertEquals("45s", DateFormatters.formatHybridCountdown(45.0))
    }

    @Test fun `hybrid countdown - minutes and seconds when under 300`() {
        assertEquals("4m 30s", DateFormatters.formatHybridCountdown(270.0))
    }

    @Test fun `hybrid countdown - minutes only when under 3600`() {
        assertEquals("15m", DateFormatters.formatHybridCountdown(900.0))
    }

    @Test fun `hybrid countdown - hours only`() {
        assertEquals("2h", DateFormatters.formatHybridCountdown(7200.0))
    }

    @Test fun `hybrid countdown - hours and minutes`() {
        assertEquals("2h 30m", DateFormatters.formatHybridCountdown(9000.0))
    }

    @Test fun `hybrid countdown - days hours minutes`() {
        assertEquals("1d 2h 30m", DateFormatters.formatHybridCountdown(95400.0))
    }

    @Test fun `hybrid countdown - days only`() {
        assertEquals("1d", DateFormatters.formatHybridCountdown(86400.0))
    }

    @Test fun `natural language - now when negative`() {
        assertEquals("now", DateFormatters.formatNaturalLanguage(-1.0))
    }

    @Test fun `natural language - now under 60s`() {
        assertEquals("now", DateFormatters.formatNaturalLanguage(30.0))
    }

    @Test fun `natural language - very soon`() {
        assertEquals("very soon", DateFormatters.formatNaturalLanguage(200.0))
    }

    @Test fun `natural language - soon`() {
        assertEquals("soon", DateFormatters.formatNaturalLanguage(600.0))
    }

    @Test fun `natural language - shortly`() {
        assertEquals("shortly", DateFormatters.formatNaturalLanguage(1200.0))
    }

    @Test fun `natural language - under an hour`() {
        assertEquals("in under an hour", DateFormatters.formatNaturalLanguage(2400.0))
    }

    @Test fun `natural language - about an hour`() {
        assertEquals("in about an hour", DateFormatters.formatNaturalLanguage(5400.0))
    }

    @Test fun `natural language - hours`() {
        assertEquals("in 3 hours", DateFormatters.formatNaturalLanguage(10800.0))
    }

    @Test fun `shouldShowSeconds - true under 300`() {
        assertTrue(DateFormatters.shouldShowSeconds(200.0))
    }

    @Test fun `shouldShowSeconds - false over 300`() {
        assertFalse(DateFormatters.shouldShowSeconds(400.0))
    }

    @Test fun `shouldShowSeconds - false when zero`() {
        assertFalse(DateFormatters.shouldShowSeconds(0.0))
    }

    @Test fun `shouldShowSeconds - false when negative`() {
        assertFalse(DateFormatters.shouldShowSeconds(-10.0))
    }

    @Test fun `relative time - now`() {
        assertEquals("now", DateFormatters.formatRelativeTime(-5.0))
    }

    @Test fun `relative time - in 1m for under 60s`() {
        assertEquals("in 1m", DateFormatters.formatRelativeTime(45.0))
    }

    @Test fun `relative time - minutes`() {
        assertEquals("in 15m", DateFormatters.formatRelativeTime(900.0))
    }

    @Test fun `relative time - hours`() {
        assertEquals("in 2h", DateFormatters.formatRelativeTime(7200.0))
    }
}
