package com.immedio.toevent.data.cache

import android.content.Context
import com.immedio.toevent.domain.model.Event
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class CachedEvents(
    val events: List<Event>,
    val timestamp: Long,
)

@Singleton
class EventCacheService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheDir: File get() = File(context.cacheDir, "events").also { it.mkdirs() }

    private val staleThresholdMs = 24 * 60 * 60 * 1000L

    fun cacheEvents(accountId: String, events: List<Event>) {
        val file = File(cacheDir, "$accountId.json")
        val cached = CachedEvents(events, System.currentTimeMillis())
        file.writeText(json.encodeToString(cached))
    }

    fun getCachedEvents(accountId: String): List<Event>? {
        val file = File(cacheDir, "$accountId.json")
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<CachedEvents>(file.readText()).events
        }.getOrNull()
    }

    fun isStale(accountId: String): Boolean {
        val file = File(cacheDir, "$accountId.json")
        if (!file.exists()) return true
        return runCatching {
            val cached = json.decodeFromString<CachedEvents>(file.readText())
            System.currentTimeMillis() - cached.timestamp > staleThresholdMs
        }.getOrDefault(true)
    }

    fun clearCache(accountId: String) {
        File(cacheDir, "$accountId.json").delete()
    }

    fun clearAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
