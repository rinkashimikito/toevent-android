package com.immedio.toevent.data.auth

import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.OAuthCredentials
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthService @Inject constructor(
    private val credentialStorage: CredentialStorage,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val clientId: String get() = "" // TODO: set from BuildConfig
    private val clientSecret: String get() = "" // TODO: set from BuildConfig

    private val tokenUrl = "https://oauth2.googleapis.com/token"

    fun getCredentials(accountId: String): OAuthCredentials? {
        return credentialStorage.retrieve(accountId)
    }

    suspend fun refreshToken(credentials: OAuthCredentials): OAuthCredentials? {
        val refreshToken = credentials.refreshToken ?: return null

        return try {
            val conn = URL(tokenUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val params = buildString {
                append("grant_type=refresh_token")
                append("&refresh_token=$refreshToken")
                append("&client_id=$clientId")
                append("&client_secret=$clientSecret")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(params) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonObj = json.parseToJsonElement(response).jsonObject
                val accessToken = jsonObj["access_token"]?.jsonPrimitive?.content ?: return null
                val expiresIn = jsonObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600

                val newCredentials = OAuthCredentials(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresAt = System.currentTimeMillis() + expiresIn * 1000,
                    accountId = credentials.accountId,
                    providerType = CalendarProviderType.GOOGLE,
                )
                credentialStorage.store(newCredentials)
                newCredentials
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun signOut(accountId: String) {
        credentialStorage.delete(accountId)
    }
}
