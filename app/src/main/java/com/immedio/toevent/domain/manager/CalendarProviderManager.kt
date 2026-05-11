package com.immedio.toevent.domain.manager

import com.immedio.toevent.data.auth.CredentialStorage
import com.immedio.toevent.data.auth.GoogleAuthService
import com.immedio.toevent.data.auth.OutlookAuthService
import com.immedio.toevent.data.cache.EventCacheService
import com.immedio.toevent.data.calendar.CalendarProvider
import com.immedio.toevent.data.calendar.GoogleCalendarProvider
import com.immedio.toevent.data.calendar.LocalCalendarProvider
import com.immedio.toevent.data.calendar.OutlookCalendarProvider
import com.immedio.toevent.domain.model.CalendarAccount
import com.immedio.toevent.domain.model.CalendarInfo
import com.immedio.toevent.domain.model.CalendarProviderType
import com.immedio.toevent.domain.model.Event
import com.immedio.toevent.domain.model.OAuthCredentials
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarProviderManager @Inject constructor(
    private val localProvider: LocalCalendarProvider,
    private val googleAuthService: GoogleAuthService,
    private val outlookAuthService: OutlookAuthService,
    private val credentialStorage: CredentialStorage,
    private val cacheService: EventCacheService,
) {
    private val oauthProviders = mutableMapOf<String, CalendarProvider>()

    fun getActiveProviders(): List<CalendarProvider> {
        val providers = mutableListOf<CalendarProvider>(localProvider)

        for (accountId in credentialStorage.listAccountIds()) {
            val creds = credentialStorage.retrieve(accountId) ?: continue
            val provider = oauthProviders[accountId] ?: run {
                val created = createProvider(creds) ?: continue
                oauthProviders[accountId] = created
                created
            }
            providers.add(provider)
        }

        return providers
    }

    private fun createProvider(credentials: OAuthCredentials): CalendarProvider? {
        val account = CalendarAccount(
            id = credentials.accountId,
            providerType = credentials.providerType,
            email = credentials.accountId,
            displayName = credentials.accountId,
        )
        return when (credentials.providerType) {
            CalendarProviderType.GOOGLE -> GoogleCalendarProvider(account, googleAuthService)
            CalendarProviderType.OUTLOOK -> OutlookCalendarProvider(account, outlookAuthService)
            CalendarProviderType.LOCAL -> null
        }
    }

    suspend fun fetchAllEvents(
        from: Long,
        to: Long,
        enabledCalendarIds: Set<String>?,
    ): List<Event> = coroutineScope {
        val providers = getActiveProviders()

        val results = providers.map { provider ->
            async {
                try {
                    val events = provider.fetchEvents(from, to, enabledCalendarIds)
                    cacheService.cacheEvents(provider.account.id, events)
                    events
                } catch (e: Exception) {
                    cacheService.getCachedEvents(provider.account.id) ?: emptyList()
                }
            }
        }.awaitAll()

        val allEvents = results.flatten()

        allEvents
            .distinctBy { "${it.source}_${it.id}" }
            .sortedBy { it.startDate }
    }

    suspend fun fetchAllCalendars(): List<CalendarInfo> = coroutineScope {
        val providers = getActiveProviders()
        providers.map { provider ->
            async {
                try {
                    provider.fetchCalendars()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    val expiredAccounts: List<CalendarAccount>
        get() = credentialStorage.listAccountIds().mapNotNull { accountId ->
            val creds = credentialStorage.retrieve(accountId)
            if (creds != null && creds.isExpired && creds.refreshToken == null) {
                CalendarAccount(
                    id = creds.accountId,
                    providerType = creds.providerType,
                    email = creds.accountId,
                    displayName = creds.accountId,
                )
            } else null
        }

    suspend fun addAccount(credentials: OAuthCredentials) {
        credentialStorage.store(credentials)
        val provider = createProvider(credentials) ?: return
        oauthProviders[credentials.accountId] = provider
    }

    suspend fun removeAccount(accountId: String) {
        oauthProviders[accountId]?.signOut()
        oauthProviders.remove(accountId)
        credentialStorage.delete(accountId)
        cacheService.clearCache(accountId)
    }
}
