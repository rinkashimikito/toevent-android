package com.immedio.toevent.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferenceKeys {
    val HAS_COMPLETED_INTRO = booleanPreferencesKey("has_completed_intro")
    val ENABLED_CALENDAR_IDS = stringSetPreferencesKey("enabled_calendar_ids")
    val LOOKAHEAD = doublePreferencesKey("lookahead")
    val CALENDAR_PRIORITY = stringPreferencesKey("calendar_priority") // JSON array
    val TIME_DISPLAY_FORMAT = stringPreferencesKey("time_display_format")
    val USE_NATURAL_LANGUAGE = booleanPreferencesKey("use_natural_language")
    val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
    val HIDE_ALL_DAY_EVENTS = booleanPreferencesKey("hide_all_day_events")
    val TITLE_MAX_LENGTH = intPreferencesKey("title_max_length")
    val URGENCY_IMMINENT = doublePreferencesKey("urgency_imminent")
    val URGENCY_SOON = doublePreferencesKey("urgency_soon")
    val URGENCY_APPROACHING = doublePreferencesKey("urgency_approaching")
    val FETCH_INTERVAL = doublePreferencesKey("fetch_interval")
    val MAX_EVENTS = intPreferencesKey("max_events")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
    val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
    val ACTIVE_SURFACE = stringPreferencesKey("active_surface")
    val BACKGROUND_MODE = stringPreferencesKey("background_mode")
    val TRAVEL_TIME_ENABLED = booleanPreferencesKey("travel_time_enabled")
    val FOCUS_FILTER_CALENDARS = stringSetPreferencesKey("focus_filter_calendars")
    val FLOATING_CHIP_ENABLED = booleanPreferencesKey("floating_chip_enabled")
    val THEME_MODE = stringPreferencesKey("theme_mode") // SYSTEM, LIGHT, DARK
}
