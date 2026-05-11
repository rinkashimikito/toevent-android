package com.immedio.toevent.domain.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActiveSurfaceTest {

    @Test
    fun `NOTIFICATION enables notification only`() {
        assertTrue(ActiveSurface.NOTIFICATION.notificationEnabled)
        assertFalse(ActiveSurface.NOTIFICATION.widgetEnabled)
    }

    @Test
    fun `WIDGET enables widget only`() {
        assertFalse(ActiveSurface.WIDGET.notificationEnabled)
        assertTrue(ActiveSurface.WIDGET.widgetEnabled)
    }

    @Test
    fun `BOTH enables both`() {
        assertTrue(ActiveSurface.BOTH.notificationEnabled)
        assertTrue(ActiveSurface.BOTH.widgetEnabled)
    }
}
