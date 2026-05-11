package com.immedio.toevent.data.auth

import android.content.Context
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.OAuthCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutlookAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialStorage: CredentialStorage,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val clientId: String get() = "" // Set from BuildConfig
    private val tenantId: String = "common"
    private val tokenUrl = "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token"
    private val scope = "https://graph.microsoft.com/Calendars.Read"

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
                append("&scope=$scope offline_access")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(params) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val jsonObj = json.parseToJsonElement(response).jsonObject
                val accessToken = jsonObj["access_token"]?.jsonPrimitive?.content ?: return null
                val newRefreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
                val expiresIn = jsonObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600

                val newCredentials = OAuthCredentials(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    expiresAt = System.currentTimeMillis() + expiresIn * 1000,
                    accountId = credentials.accountId,
                    providerType = CalendarProviderType.OUTLOOK,
                )
                credentialStorage.store(newCredentials)
                newCredentials
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun signOut(accountId: String) {
        credentialStorage.delete(accountId)
    }
}
