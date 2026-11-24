package com.example.data.local

import com.example.data.model.GhostModePreferences
import com.example.data.model.GhostPrivacyCategory
import com.example.data.model.InterceptedPrivacyRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Repository pattern abstraction for managing Ghost Mode privacy preferences
 * and recording intercepted telemetry requests across Native Social Engine and OkHttp.
 */
interface GhostModeRepository {
    val privacyPreferences: Flow<GhostModePreferences>
    val interceptedRequests: StateFlow<List<InterceptedPrivacyRequest>>
    val blockedCountsByCategory: StateFlow<Map<GhostPrivacyCategory, Int>>

    fun currentPreferencesSnapshot(): GhostModePreferences
    suspend fun getPreferences(): GhostModePreferences

    suspend fun setStorySeenHidden(hidden: Boolean)
    suspend fun setDmReadReceiptsHidden(hidden: Boolean)
    suspend fun setTypingIndicatorHidden(hidden: Boolean)
    suspend fun setOnlineStatusHidden(hidden: Boolean)
    suspend fun setLogBlockedRequests(enabled: Boolean)
    suspend fun setMasterGhostMode(enabled: Boolean)

    fun recordInterceptedRequest(
        endpoint: String,
        category: GhostPrivacyCategory,
        method: String = "POST",
        transport: String = "NativeEngine",
        reason: String = category.displayName
    )

    fun clearInterceptedLog()
}

class DefaultGhostModeRepository(
    private val prefsRepository: PreferencesRepository
) : GhostModeRepository {

    private val latestSnapshot = AtomicReference(GhostModePreferences())
    private val sequenceGenerator = AtomicLong(0L)

    override val privacyPreferences: Flow<GhostModePreferences> =
        prefsRepository.settingsFlow.map { settings ->
            GhostModePreferences.fromSettings(settings).also { latestSnapshot.set(it) }
        }

    private val _interceptedRequests = MutableStateFlow<List<InterceptedPrivacyRequest>>(emptyList())
    override val interceptedRequests: StateFlow<List<InterceptedPrivacyRequest>> =
        _interceptedRequests.asStateFlow()

    private val _blockedCountsByCategory = MutableStateFlow(
        GhostPrivacyCategory.entries.associateWith { 0 }
    )
    override val blockedCountsByCategory: StateFlow<Map<GhostPrivacyCategory, Int>> =
        _blockedCountsByCategory.asStateFlow()

    override fun currentPreferencesSnapshot(): GhostModePreferences = latestSnapshot.get()

    override suspend fun getPreferences(): GhostModePreferences =
        privacyPreferences.first()

    override suspend fun setStorySeenHidden(hidden: Boolean) {
        latestSnapshot.set(latestSnapshot.get().copy(hideStorySeen = hidden))
        prefsRepository.updateBoolean("anonymous_story_viewing", hidden)
    }

    override suspend fun setDmReadReceiptsHidden(hidden: Boolean) {
        latestSnapshot.set(latestSnapshot.get().copy(hideDmReadReceipts = hidden))
        prefsRepository.updateBoolean("disable_read_receipts", hidden)
    }

    override suspend fun setTypingIndicatorHidden(hidden: Boolean) {
        latestSnapshot.set(latestSnapshot.get().copy(hideTypingIndicator = hidden))
        prefsRepository.updateBoolean("hide_typing_indicator", hidden)
    }

    override suspend fun setOnlineStatusHidden(hidden: Boolean) {
        latestSnapshot.set(latestSnapshot.get().copy(hideOnlineStatus = hidden))
        prefsRepository.updateBoolean("hide_online_status", hidden)
    }

    override suspend fun setLogBlockedRequests(enabled: Boolean) {
        latestSnapshot.set(latestSnapshot.get().copy(logBlockedRequests = enabled))
        prefsRepository.updateBoolean("log_ghost_intercepts", enabled)
    }

    override suspend fun setMasterGhostMode(enabled: Boolean) {
        latestSnapshot.set(
            latestSnapshot.get().copy(
                hideStorySeen = enabled,
                hideDmReadReceipts = enabled,
                hideTypingIndicator = enabled,
                hideOnlineStatus = enabled
            )
        )
        prefsRepository.updateBoolean("hide_online_status", enabled)
        prefsRepository.updateBoolean("anonymous_story_viewing", enabled)
        prefsRepository.updateBoolean("disable_read_receipts", enabled)
        prefsRepository.updateBoolean("hide_typing_indicator", enabled)
    }

    override fun recordInterceptedRequest(
        endpoint: String,
        category: GhostPrivacyCategory,
        method: String,
        transport: String,
        reason: String
    ) {
        _blockedCountsByCategory.update { current ->
            current + (category to ((current[category] ?: 0) + 1))
        }
        if (!latestSnapshot.get().logBlockedRequests) return

        val entry = InterceptedPrivacyRequest(
            id = sequenceGenerator.incrementAndGet(),
            timestampMillis = System.currentTimeMillis(),
            category = category,
            method = method.uppercase(),
            endpoint = endpoint.take(240),
            transport = transport,
            reason = reason
        )
        _interceptedRequests.update { current ->
            (listOf(entry) + current).take(MAX_LOG_ENTRIES)
        }
    }

    override fun clearInterceptedLog() {
        _interceptedRequests.value = emptyList()
        _blockedCountsByCategory.value = GhostPrivacyCategory.entries.associateWith { 0 }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 60
    }
}
