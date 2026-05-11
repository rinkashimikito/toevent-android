package com.immedio.toevent.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelTimeService @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, TravelTimeResult>()
    private val bufferMinutes = 5

    data class TravelTimeResult(
        val durationMinutes: Int,
        val leaveByText: String,
    )

    suspend fun getTravelTime(
        destination: String,
        eventStartMs: Long,
        enabled: Boolean,
    ): TravelTimeResult? {
        if (!enabled) return null
        val apiKey = ""
        if (apiKey.isBlank()) return null

        cache[destination]?.let { return it }

        return try {
            val origin = "current+location"
            val url = buildString {
                append("https://maps.googleapis.com/maps/api/directions/json")
                append("?origin=${URLEncoder.encode(origin, "UTF-8")}")
                append("&destination=${URLEncoder.encode(destination, "UTF-8")}")
                append("&mode=driving")
                append("&key=$apiKey")
            }

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonObj = json.parseToJsonElement(response).jsonObject
                val routes = jsonObj["routes"]?.jsonArray
                val leg = routes?.firstOrNull()?.jsonObject?.get("legs")?.jsonArray?.firstOrNull()?.jsonObject
                val durationSec = leg?.get("duration")?.jsonObject?.get("value")?.jsonPrimitive?.content?.toLongOrNull()

                if (durationSec != null) {
                    val totalMinutes = (durationSec / 60).toInt() + bufferMinutes
                    val leaveByMs = eventStartMs - totalMinutes * 60 * 1000L
                    val leaveByText = "Leave by ${DateFormatters.formatAbsoluteTime(leaveByMs)} ($totalMinutes min drive)"

                    val result = TravelTimeResult(totalMinutes, leaveByText)
                    cache[destination] = result
                    result
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun clearCache() {
        cache.clear()
    }
}
