package com.immedio.toevent.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UrgencyLevelTest {

    private val defaults = UrgencyThresholds.DEFAULT

    @Test
    fun `now when seconds remaining is negative`() {
        assertEquals(UrgencyLevel.NOW, UrgencyLevel.from(-10.0, defaults))
    }

    @Test
    fun `now when seconds remaining is zero`() {
        assertEquals(UrgencyLevel.NOW, UrgencyLevel.from(0.0, defaults))
    }

    @Test
    fun `imminent when under 300s`() {
        assertEquals(UrgencyLevel.IMMINENT, UrgencyLevel.from(299.0, defaults))
    }

    @Test
    fun `imminent at 1 second`() {
        assertEquals(UrgencyLevel.IMMINENT, UrgencyLevel.from(1.0, defaults))
    }

    @Test
    fun `soon at exactly 300s boundary`() {
        assertEquals(UrgencyLevel.SOON, UrgencyLevel.from(300.0, defaults))
    }

    @Test
    fun `soon when between 300s and 900s`() {
        assertEquals(UrgencyLevel.SOON, UrgencyLevel.from(600.0, defaults))
    }

    @Test
    fun `approaching at exactly 900s boundary`() {
        assertEquals(UrgencyLevel.APPROACHING, UrgencyLevel.from(900.0, defaults))
    }

    @Test
    fun `approaching when between 900s and 1800s`() {
        assertEquals(UrgencyLevel.APPROACHING, UrgencyLevel.from(1200.0, defaults))
    }

    @Test
    fun `normal at exactly 1800s boundary`() {
        assertEquals(UrgencyLevel.NORMAL, UrgencyLevel.from(1800.0, defaults))
    }

    @Test
    fun `normal when over 1800s`() {
        assertEquals(UrgencyLevel.NORMAL, UrgencyLevel.from(3600.0, defaults))
    }

    @Test
    fun `custom thresholds`() {
        val custom = UrgencyThresholds(imminent = 60.0, soon = 180.0, approaching = 600.0)
        assertEquals(UrgencyLevel.IMMINENT, UrgencyLevel.from(30.0, custom))
        assertEquals(UrgencyLevel.SOON, UrgencyLevel.from(120.0, custom))
        assertEquals(UrgencyLevel.APPROACHING, UrgencyLevel.from(300.0, custom))
        assertEquals(UrgencyLevel.NORMAL, UrgencyLevel.from(700.0, custom))
    }

    @Test
    fun `thresholds validation - valid`() {
        assertTrue(UrgencyThresholds(60.0, 180.0, 600.0).isValid)
    }

    @Test
    fun `thresholds validation - invalid reversed order`() {
        assertFalse(UrgencyThresholds(600.0, 180.0, 60.0).isValid)
    }

    @Test
    fun `thresholds validation - invalid equal values`() {
        assertFalse(UrgencyThresholds(300.0, 300.0, 300.0).isValid)
    }

    @Test
    fun `default thresholds are valid`() {
        assertTrue(UrgencyThresholds.DEFAULT.isValid)
    }
}
