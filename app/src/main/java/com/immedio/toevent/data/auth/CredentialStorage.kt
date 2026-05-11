package com.immedio.toevent.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.immedio.toevent.domain.model.OAuthCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "oauth_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun store(credentials: OAuthCredentials) {
        val serialized = json.encodeToString(credentials)
        prefs.edit().putString(credentials.accountId, serialized).apply()
    }

    fun retrieve(accountId: String): OAuthCredentials? {
        val serialized = prefs.getString(accountId, null) ?: return null
        return runCatching { json.decodeFromString<OAuthCredentials>(serialized) }.getOrNull()
    }

    fun delete(accountId: String) {
        prefs.edit().remove(accountId).apply()
    }

    fun listAccountIds(): Set<String> {
        return prefs.all.keys
    }

    fun hasCredentials(accountId: String): Boolean {
        return prefs.contains(accountId)
    }
}
