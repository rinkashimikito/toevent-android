package com.immedio.toevent.service

object MeetingUrlParser {

    private val patterns = listOf(
        "Zoom" to Regex("""https?://([a-z0-9]+\.)?zoom(gov)?\.us/(j|my|w)/[a-zA-Z0-9/?=&\-]+""", RegexOption.IGNORE_CASE),
        "Google Meet" to Regex("""https?://meet\.google\.com/[a-z]+-[a-z]+-[a-z]+""", RegexOption.IGNORE_CASE),
        "Microsoft Teams" to Regex("""https?://teams\.microsoft\.com/l/meetup-join/\S+""", RegexOption.IGNORE_CASE),
        "Webex" to Regex("""https?://([a-z0-9]+\.)?webex\.com/\S+""", RegexOption.IGNORE_CASE),
    )

    fun findMeetingUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        for ((_, regex) in patterns) {
            val match = regex.find(text)
            if (match != null) return match.value
        }
        return null
    }

    fun findMeetingUrl(url: String?, location: String?, notes: String?): String? {
        if (url != null && isMeetingUrl(url)) return url
        findMeetingUrl(location)?.let { return it }
        return findMeetingUrl(notes)
    }

    fun isMeetingUrl(url: String): Boolean {
        return patterns.any { (_, regex) -> regex.containsMatchIn(url) }
    }
}
