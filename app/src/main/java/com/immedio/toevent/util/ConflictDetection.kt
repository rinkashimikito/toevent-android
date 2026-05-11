package com.immedio.toevent.util

import com.immedio.toevent.domain.model.Event

fun List<Event>.conflictingEventIds(): Set<String> {
    val timed = filter { !it.isAllDay }
    val ids = mutableSetOf<String>()
    for (i in timed.indices) {
        for (j in i + 1 until timed.size) {
            if (timed[i].startDate < timed[j].endDate && timed[j].startDate < timed[i].endDate) {
                ids.add(timed[i].id)
                ids.add(timed[j].id)
            }
        }
    }
    return ids
}

fun List<Event>.hasConflict(event: Event): Boolean {
    return conflictingEventIds().contains(event.id)
}
