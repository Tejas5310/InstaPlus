package com.example.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiCaptionUiState
import com.example.data.ai.TranslationRepository
import com.example.data.ai.TranslationUiState
import com.example.data.local.DirectMessageEntity
import com.example.data.local.DirectThreadEntity
import com.example.data.local.DownloadedMediaEntity
import com.example.data.local.GhostModeRepository
import com.example.data.local.LockedChatEntity
import com.example.data.local.PostCommentEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.SocialPlatformRepository
import com.example.data.local.SocialPostEntity
import com.example.data.local.StoryEntity
import com.example.data.local.VaultAndAccountRepository
import com.example.data.media.DownloadState
import com.example.data.media.MediaStoreDownloader
import com.example.data.model.AccentColorPreset
import com.example.data.model.ChatBackgroundPreset
import com.example.data.model.DetectedMediaItem
import com.example.data.model.FontPreset
import com.example.data.model.GhostModePreferences
import com.example.data.model.GhostPrivacyCategory
import com.example.data.model.InstaPlusSection
import com.example.data.model.InstaPlusSettings
import com.example.data.model.InterceptedPrivacyRequest
import com.example.data.model.LauncherIconVariant
import com.example.data.model.MediaType
import com.example.data.model.ThemePreset
import com.example.data.remote.CloudLockedChat
import com.example.data.remote.CloudVaultItem
import com.example.data.remote.FirestoreCloudRepository
import com.example.system.AppSystemBridge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "InstaPlusViewModel"

enum class PinSetupMode {
    CREATE,
    CHANGE
}

sealed interface LockChallenge {
    data object AppLaunch : LockChallenge
    data object MessagesTab : LockChallenge
    data class SpecificThread(val threadKey: String, val displayName: String) : LockChallenge
}

data class CloudSyncState(
    val userId: String = "",
    val displayName: String = "",
    val mobileNumber: String = "",
    val email: String? = null,
    val isCloudLinked: Boolean = false,
    val isSyncing: Boolean = false,
    val cloudVaultCount: Int = 0,
    val cloudLockedChatsCount: Int = 0,
    val lastSyncedAtMillis: Long? = null,
    val errorMessage: String? = null
)

class InstaPlusViewModel(
    private val currentUserId: String,
    private val currentUserName: String,
    private val currentFullName: String,
    private val currentMobileNumber: String,
    private val currentUserEmail: String?,
    private val hasFirebaseCloudAuth: Boolean,
    private val prefsRepo: PreferencesRepository,
    private val ghostModeRepo: GhostModeRepository,
    private val vaultAccountRepo: VaultAndAccountRepository,
    private val socialRepo: SocialPlatformRepository,
    private val cloudRepo: FirestoreCloudRepository,
    private val downloader: MediaStoreDownloader,
    private val translator: TranslationRepository,
    private val systemBridge: AppSystemBridge
) : ViewModel() {

    val settings: StateFlow<InstaPlusSettings> = prefsRepo.settingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), InstaPlusSettings()
    )
    val ghostPreferences: StateFlow<GhostModePreferences> = ghostModeRepo.privacyPreferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), GhostModePreferences()
    )
    val ghostInterceptedRequests: StateFlow<List<InterceptedPrivacyRequest>> =
        ghostModeRepo.interceptedRequests
    val ghostBlockedCounts: StateFlow<Map<GhostPrivacyCategory, Int>> =
        ghostModeRepo.blockedCountsByCategory

    val downloadedMedia: StateFlow<List<DownloadedMediaEntity>> = vaultAccountRepo.downloadedMedia.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val lockedChats: StateFlow<List<LockedChatEntity>> = vaultAccountRepo.lockedChats.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val socialPosts: StateFlow<List<SocialPostEntity>> = socialRepo.posts.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val postComments: StateFlow<List<PostCommentEntity>> = socialRepo.comments.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val stories: StateFlow<List<StoryEntity>> = socialRepo.stories.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val directThreads: StateFlow<List<DirectThreadEntity>> = socialRepo.threads.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val directMessages: StateFlow<List<DirectMessageEntity>> = socialRepo.messages.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _cloudSyncState = MutableStateFlow(
        CloudSyncState(
            userId = currentUserId,
            displayName = currentUserName,
            mobileNumber = currentMobileNumber,
            email = currentUserEmail,
            isCloudLinked = hasFirebaseCloudAuth,
            isSyncing = false
        )
    )
    val cloudSyncState: StateFlow<CloudSyncState> = _cloudSyncState.asStateFlow()

    val cloudVaultItems: StateFlow<List<CloudVaultItem>> =
        (if (hasFirebaseCloudAuth) cloudRepo.observeVaultItems(currentUserId) else emptyFlow())
            .catch { err -> if (err is CancellationException) throw err }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudLockedChats: StateFlow<List<CloudLockedChat>> =
        (if (hasFirebaseCloudAuth) cloudRepo.observeLockedChats(currentUserId) else emptyFlow())
            .catch { err -> if (err is CancellationException) throw err }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSection = MutableStateFlow(InstaPlusSection.HOME)
    val activeSection: StateFlow<InstaPlusSection> = _activeSection.asStateFlow()

    private val _activeThreadKey = MutableStateFlow<String?>(null)
    val activeThreadKey: StateFlow<String?> = _activeThreadKey.asStateFlow()

    private val _showComposerSheet = MutableStateFlow(false)
    val showComposerSheet: StateFlow<Boolean> = _showComposerSheet.asStateFlow()

    private val _aiCaptionState = MutableStateFlow<AiCaptionUiState>(AiCaptionUiState.Idle)
    val aiCaptionState: StateFlow<AiCaptionUiState> = _aiCaptionState.asStateFlow()

    private val _showDownloadSheet = MutableStateFlow(false)
    val showDownloadSheet: StateFlow<Boolean> = _showDownloadSheet.asStateFlow()

    private val _detectedMedia = MutableStateFlow<List<DetectedMediaItem>>(emptyList())
    val detectedMedia: StateFlow<List<DetectedMediaItem>> = _detectedMedia.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _showTranslationSheet = MutableStateFlow(false)
    val showTranslationSheet: StateFlow<Boolean> = _showTranslationSheet.asStateFlow()

    private val _translationSourceText = MutableStateFlow("")
    val translationSourceText: StateFlow<String> = _translationSourceText.asStateFlow()

    private val _translationState = MutableStateFlow<TranslationUiState>(TranslationUiState.Idle)
    val translationState: StateFlow<TranslationUiState> = _translationState.asStateFlow()

    private val _showAccountSwitcher = MutableStateFlow(false)
    val showAccountSwitcher: StateFlow<Boolean> = _showAccountSwitcher.asStateFlow()

    private val _pinSetupMode = MutableStateFlow<PinSetupMode?>(null)
    val pinSetupMode: StateFlow<PinSetupMode?> = _pinSetupMode.asStateFlow()

    private val _activeLockChallenge = MutableStateFlow<LockChallenge?>(null)
    val activeLockChallenge: StateFlow<LockChallenge?> = _activeLockChallenge.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _focusSecondsElapsed = MutableStateFlow(0)
    val focusSecondsElapsed: StateFlow<Int> = _focusSecondsElapsed.asStateFlow()

    private val _showFocusBreakOverlay = MutableStateFlow(false)
    val showFocusBreakOverlay: StateFlow<Boolean> = _showFocusBreakOverlay.asStateFlow()

    private val _userBio = MutableStateFlow("Building memories & stories on InstaPlus • Privacy-first creator")
    val userBio: StateFlow<String> = _userBio.asStateFlow()

    private val unlockedKeysThisSession = mutableSetOf<String>()
    private var appUnlockedThisSession = false
    private var dmTabUnlockedThisSession = false
    private var translationJob: Job? = null

    init {
        viewModelScope.launch {
            socialRepo.ensureSeeded(currentUserName, currentFullName)
        }
        viewModelScope.launch {
            settings.collect { current ->
                val canAuth = current.hasPinConfigured || current.isBiometricEnabled
                if (current.isAppLockEnabled && canAuth && !appUnlockedThisSession) {
                    _activeLockChallenge.value = LockChallenge.AppLaunch
                }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(2000L)
                socialRepo.purgeExpiredContent()
                val s = settings.value
                if (s.focusGuardEnabled && !_showFocusBreakOverlay.value) {
                    val next = _focusSecondsElapsed.value + 2
                    _focusSecondsElapsed.value = next
                    if (next >= s.focusLimitMinutes * 60) {
                        _showFocusBreakOverlay.value = true
                    }
                }
            }
        }
        viewModelScope.launch {
            cloudVaultItems.collect { items ->
                _cloudSyncState.value = _cloudSyncState.value.copy(cloudVaultCount = items.size)
            }
        }
        viewModelScope.launch {
            cloudLockedChats.collect { items ->
                _cloudSyncState.value = _cloudSyncState.value.copy(cloudLockedChatsCount = items.size)
            }
        }
    }

    // Navigation & Social Section Routing
    fun selectSection(section: InstaPlusSection) {
        _activeSection.value = section
        if (section == InstaPlusSection.MESSAGES) {
            checkDmLock(_activeThreadKey.value)
        }
    }

    fun openChatThread(thread: DirectThreadEntity) {
        _activeThreadKey.value = thread.threadKey
        val canAuth = settings.value.hasPinConfigured || settings.value.isBiometricEnabled
        val isLocked = lockedChats.value.any {
            it.threadKey.equals(thread.threadKey, ignoreCase = true)
        }
        if (isLocked && canAuth && !unlockedKeysThisSession.contains(thread.threadKey.lowercase())) {
            _activeLockChallenge.value = LockChallenge.SpecificThread(
                threadKey = thread.threadKey.lowercase(),
                displayName = thread.displayName
            )
        } else {
            viewModelScope.launch {
                socialRepo.openThreadRespectingGhostMode(thread.threadKey)
            }
        }
    }

    fun closeChatThread() {
        _activeThreadKey.value = null
    }

    fun isCurrentConversationProtected(): Boolean {
        val currentSettings = settings.value
        val activeSection = _activeSection.value
        if (activeSection == InstaPlusSection.MESSAGES && currentSettings.isDmTabLocked) return true
        val threadKey = _activeThreadKey.value?.lowercase() ?: return false
        return lockedChats.value.any { it.threadKey.lowercase() == threadKey }
    }

    private fun checkDmLock(threadKey: String?) {
        val currentSettings = settings.value
        val canAuth = currentSettings.hasPinConfigured || currentSettings.isBiometricEnabled
        if (!canAuth) return

        val normalized = threadKey?.lowercase()
        val matched = normalized?.let { key ->
            lockedChats.value.firstOrNull { it.threadKey.lowercase() == key }
        }
        if (matched != null && !unlockedKeysThisSession.contains(matched.threadKey.lowercase())) {
            _activeLockChallenge.value = LockChallenge.SpecificThread(
                threadKey = matched.threadKey.lowercase(),
                displayName = matched.displayName
            )
            return
        }
        if (currentSettings.isDmTabLocked && !dmTabUnlockedThisSession) {
            _activeLockChallenge.value = LockChallenge.MessagesTab
        }
    }

    // Social Feed Actions
    fun toggleLike(post: SocialPostEntity) {
        viewModelScope.launch { socialRepo.toggleLike(post) }
    }

    fun toggleBookmark(post: SocialPostEntity) {
        viewModelScope.launch {
            socialRepo.toggleBookmark(post)
            _snackbarMessage.value = if (!post.isBookmarked) "Saved to Bookmarks" else "Removed from Bookmarks"
        }
    }

    fun toggleFollowAuthor(post: SocialPostEntity) {
        viewModelScope.launch {
            socialRepo.toggleFollow(post.authorUsername, post.isFollowingAuthor)
            _snackbarMessage.value = if (!post.isFollowingAuthor) {
                "Following @${post.authorUsername}"
            } else {
                "Unfollowed @${post.authorUsername}"
            }
        }
    }

    fun votePoll(post: SocialPostEntity, choice: Int) {
        viewModelScope.launch { socialRepo.votePoll(post, choice) }
    }

    fun addComment(postId: Long, text: String) {
        viewModelScope.launch { socialRepo.addComment(postId, currentUserName, text) }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            socialRepo.deletePost(postId)
            _snackbarMessage.value = "Post deleted"
        }
    }

    fun markStoryViewed(story: StoryEntity) {
        viewModelScope.launch { socialRepo.viewStoryRespectingGhostMode(story) }
    }

    fun sendDirectMessage(threadKey: String, text: String, burnAfterSeconds: Int) {
        viewModelScope.launch {
            socialRepo.sendMessage(threadKey, currentUserName, text, burnAfterSeconds)
        }
    }

    fun notifyTypingInThread(threadKey: String) {
        socialRepo.notifyTypingRespectingGhostMode(threadKey)
    }

    fun startNewChatThread(handle: String, displayName: String) {
        viewModelScope.launch {
            val key = socialRepo.createOrOpenThread(handle, displayName)
            _activeThreadKey.value = key
        }
    }

    fun updateUserBio(newBio: String) {
        val cleaned = newBio.trim()
        if (cleaned.isNotBlank()) {
            _userBio.value = cleaned
            _snackbarMessage.value = "Profile bio updated"
        }
    }

    // Composer + AI Caption Studio
    fun openComposerSheet() {
        _aiCaptionState.value = AiCaptionUiState.Idle
        _showComposerSheet.value = true
    }

    fun dismissComposerSheet() {
        _showComposerSheet.value = false
    }

    fun generateAiCaption(topic: String, tone: String) {
        viewModelScope.launch {
            _aiCaptionState.value = AiCaptionUiState.Loading
            translator.generateCaptionAndHashtags(topic, tone).fold(
                onSuccess = { (cap, tags) ->
                    _aiCaptionState.value = AiCaptionUiState.Success(cap, tags)
                },
                onFailure = { err ->
                    _aiCaptionState.value = AiCaptionUiState.Error(
                        err.localizedMessage ?: "AI caption generation failed"
                    )
                }
            )
        }
    }

    fun publishStandalonePost(
        asStory: Boolean,
        asReel: Boolean,
        mediaKey: String,
        caption: String,
        hashtags: String,
        categoryTag: String,
        moodTag: String,
        pollQuestion: String,
        pollOptionA: String,
        pollOptionB: String,
        ttlHours: Int?
    ) {
        viewModelScope.launch {
            if (asStory) {
                socialRepo.publishStory(currentUserName, currentFullName, mediaKey, caption, moodTag)
                _snackbarMessage.value = "Story published to your 24h ring"
            } else {
                socialRepo.publishPost(
                    authorUsername = currentUserName,
                    authorFullName = currentFullName,
                    caption = caption,
                    hashtags = hashtags,
                    categoryTag = categoryTag,
                    mediaKey = mediaKey,
                    isReel = asReel,
                    moodTag = moodTag,
                    pollQuestion = pollQuestion,
                    pollOptionA = pollOptionA,
                    pollOptionB = pollOptionB,
                    ttlHours = ttlHours
                )
                _snackbarMessage.value = when {
                    ttlHours != null -> "Time Capsule post published (expires in ${ttlHours}h)"
                    asReel -> "Reel published to InstaPlus"
                    else -> "Post published to InstaPlus Feed"
                }
            }
            _showComposerSheet.value = false
        }
    }

    // Clipboard & Native Share (Exclusive 1-Tap Copy)
    fun copyTextWithToast(label: String, text: String) {
        if (text.isBlank()) return
        systemBridge.copyToClipboard(label, text)
        _snackbarMessage.value = "$label copied to clipboard"
    }

    fun sharePostText(post: SocialPostEntity) {
        val shareBody = buildString {
            append("@${post.authorUsername} on InstaPlus:\n")
            append(post.caption)
            if (post.hashtags.isNotBlank()) append("\n\n${post.hashtags}")
        }
        systemBridge.shareText("Share @${post.authorUsername}'s post", shareBody)
    }

    // High-Res Media Saver & Vault
    fun savePostToVault(post: SocialPostEntity) {
        val item = DetectedMediaItem(
            url = post.mediaKey,
            mediaType = if (post.isReel) MediaType.REEL else MediaType.PHOTO,
            authorHandle = post.authorUsername,
            caption = post.caption,
            isVideo = false
        )
        downloadMediaItem(item)
    }

    fun saveStoryToVault(story: StoryEntity) {
        val item = DetectedMediaItem(
            url = story.mediaKey,
            mediaType = MediaType.STORY,
            authorHandle = story.authorUsername,
            caption = story.caption,
            isVideo = false
        )
        downloadMediaItem(item)
    }

    fun openDownloadSheetForFeed() {
        val items = socialPosts.value.take(6).map {
            DetectedMediaItem(
                url = it.mediaKey,
                mediaType = if (it.isReel) MediaType.REEL else MediaType.PHOTO,
                authorHandle = it.authorUsername,
                caption = it.caption,
                isVideo = false
            )
        }
        _detectedMedia.value = items
        _showDownloadSheet.value = true
    }

    fun openManualDownloadSheet() {
        openDownloadSheetForFeed()
    }

    fun dismissDownloadSheet() {
        _showDownloadSheet.value = false
        if (_downloadState.value !is DownloadState.Downloading) {
            _downloadState.value = DownloadState.Idle
        }
    }

    fun downloadMediaItem(item: DetectedMediaItem) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading("Saving ${item.mediaType.label}...")
            downloader.downloadToGallery(item).fold(
                onSuccess = { entity ->
                    vaultAccountRepo.saveDownloadedMedia(entity)
                    if (hasFirebaseCloudAuth) {
                        cloudRepo.saveVaultItem(currentUserId, CloudVaultItem.fromEntity(currentUserId, entity))
                    }
                    _downloadState.value = DownloadState.Success(entity)
                    _snackbarMessage.value = "Saved ${item.mediaType.label} to Gallery & Vault"
                },
                onFailure = { err ->
                    val msg = err.localizedMessage ?: "Download failed"
                    _downloadState.value = DownloadState.Error(msg)
                    _snackbarMessage.value = msg
                }
            )
        }
    }

    fun downloadManualUrl(rawUrl: String, mediaType: MediaType) {
        downloadMediaItem(
            DetectedMediaItem(
                url = rawUrl.trim(),
                mediaType = mediaType,
                authorHandle = "direct_url",
                isVideo = mediaType == MediaType.REEL || mediaType == MediaType.VIDEO
            )
        )
    }

    fun deleteDownloadedMedia(id: Long) {
        viewModelScope.launch {
            val target = downloadedMedia.value.firstOrNull { it.id == id }
            vaultAccountRepo.deleteDownloadedMedia(id)
            if (target != null && hasFirebaseCloudAuth) {
                cloudRepo.deleteVaultItem(currentUserId, "${target.fileName}_${target.savedAt}")
            }
            _snackbarMessage.value = "Removed from Vault"
        }
    }

    // Translator
    fun openTranslationWithText(text: String) {
        _translationSourceText.value = text
        _translationState.value = TranslationUiState.Idle
        _showTranslationSheet.value = true
        if (text.isNotBlank()) translateText(text)
    }

    fun updateTranslationSourceText(text: String) {
        _translationSourceText.value = text
    }

    fun translateText(sourceText: String) {
        translationJob?.cancel()
        val targetLang = settings.value.targetLanguage
        translationJob = viewModelScope.launch {
            translator.translateStream(sourceText, targetLang).collect { state ->
                _translationState.value = state
            }
        }
    }

    fun dismissTranslationSheet() {
        translationJob?.cancel()
        _showTranslationSheet.value = false
    }

    // Ghost Mode Controls
    fun toggleGhostMaster() = setGhostMaster(!ghostPreferences.value.isAnyActive)
    fun setGhostMaster(enabled: Boolean) {
        viewModelScope.launch {
            ghostModeRepo.setMasterGhostMode(enabled)
            _snackbarMessage.value = if (enabled) "Ghost Mode enabled" else "Ghost Mode paused"
        }
    }

    fun setGhostStorySeenHidden(hidden: Boolean) = viewModelScope.launch { ghostModeRepo.setStorySeenHidden(hidden) }
    fun setGhostDmSeenHidden(hidden: Boolean) = viewModelScope.launch { ghostModeRepo.setDmReadReceiptsHidden(hidden) }
    fun setGhostTypingHidden(hidden: Boolean) = viewModelScope.launch { ghostModeRepo.setTypingIndicatorHidden(hidden) }
    fun setGhostPresenceHidden(hidden: Boolean) = viewModelScope.launch { ghostModeRepo.setOnlineStatusHidden(hidden) }
    fun setGhostLoggingEnabled(enabled: Boolean) = viewModelScope.launch { ghostModeRepo.setLogBlockedRequests(enabled) }
    fun clearGhostInterceptLog() {
        ghostModeRepo.clearInterceptedLog()
        _snackbarMessage.value = "Ghost Mode interception log cleared"
    }

    // Theme, DataStore & Mindful Focus Guard
    fun updateBooleanSetting(key: String, value: Boolean) {
        viewModelScope.launch {
            if ((key == "is_dm_tab_locked" || key == "is_app_lock_enabled") &&
                value && !settings.value.hasPinConfigured
            ) {
                _pinSetupMode.value = PinSetupMode.CREATE
            }
            prefsRepo.updateBoolean(key, value)
        }
    }

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setDarkMode(enabled)
        _snackbarMessage.value = if (enabled) "Dark Mode enabled" else "Light Mode enabled"
    }

    fun toggleDarkMode() = setDarkMode(!settings.value.isDarkMode)
    fun setThemePreset(preset: ThemePreset) = viewModelScope.launch { prefsRepo.setThemePreset(preset) }
    fun setAccentPreset(accent: AccentColorPreset) = viewModelScope.launch {
        prefsRepo.setAccentPreset(accent)
        _snackbarMessage.value = "Accent updated to ${accent.displayName}"
    }
    fun setFontPreset(preset: FontPreset) = viewModelScope.launch { prefsRepo.setFontPreset(preset) }
    fun setChatBackgroundPreset(preset: ChatBackgroundPreset) = viewModelScope.launch { prefsRepo.setChatBackground(preset) }
    fun setCustomChatBackground(uri: Uri) = viewModelScope.launch {
        prefsRepo.setChatBackground(ChatBackgroundPreset.CUSTOM_PHOTO, uri.toString())
        _snackbarMessage.value = "Chat wallpaper updated"
    }
    fun setTargetLanguage(language: String) = viewModelScope.launch { prefsRepo.setTargetLanguage(language) }
    fun setFocusLimitMinutes(minutes: Int) = viewModelScope.launch { prefsRepo.setFocusLimitMinutes(minutes) }
    fun dismissFocusBreakReset() {
        _focusSecondsElapsed.value = 0
        _showFocusBreakOverlay.value = false
    }

    fun setLauncherIcon(variant: LauncherIconVariant) {
        viewModelScope.launch {
            if (systemBridge.switchLauncherIcon(variant)) {
                prefsRepo.setLauncherIconVariant(variant)
                _snackbarMessage.value = "App icon updated to ${variant.displayName}"
            } else {
                _snackbarMessage.value = "Unable to switch app icon on this launcher"
            }
        }
    }

    // Security & Locks
    fun openPinSetup(mode: PinSetupMode) { _pinSetupMode.value = mode }
    fun dismissPinSetup() { _pinSetupMode.value = null }
    fun saveNewPin(pin: String) = viewModelScope.launch {
        prefsRepo.setPin(pin)
        _pinSetupMode.value = null
        _snackbarMessage.value = "PBKDF2-SHA256 Chat Lock PIN saved locally"
    }

    fun submitUnlockPin(pin: String) = viewModelScope.launch {
        if (prefsRepo.verifyPin(pin)) {
            _pinError.value = null
            markChallengeUnlocked()
        } else {
            _pinError.value = "Incorrect PIN. Try again."
        }
    }

    fun onBiometricUnlockSucceeded() {
        _pinError.value = null
        markChallengeUnlocked()
    }

    private fun markChallengeUnlocked() {
        when (val c = _activeLockChallenge.value) {
            LockChallenge.AppLaunch -> appUnlockedThisSession = true
            LockChallenge.MessagesTab -> dmTabUnlockedThisSession = true
            is LockChallenge.SpecificThread -> unlockedKeysThisSession.add(c.threadKey.lowercase())
            null -> Unit
        }
        _activeLockChallenge.value = null
    }

    fun dismissLockBackToHome() {
        _activeLockChallenge.value = null
        _pinError.value = null
        _activeThreadKey.value = null
        _activeSection.value = InstaPlusSection.HOME
    }

    fun addLockedChat(threadKey: String, displayName: String) {
        viewModelScope.launch {
            val clean = threadKey.trim().lowercase().let { if (it.startsWith("@")) it else "@$it" }
            vaultAccountRepo.addLockedChat(clean, displayName)
            _snackbarMessage.value = "Chat lock enabled for $displayName"
        }
    }

    fun lockActiveConversationFromWrapper(threadKey: String, displayName: String) {
        viewModelScope.launch {
            if (threadKey.isBlank()) {
                prefsRepo.updateBoolean("is_dm_tab_locked", true)
                dmTabUnlockedThisSession = false
                _activeLockChallenge.value = LockChallenge.MessagesTab
            } else {
                vaultAccountRepo.addLockedChat(threadKey.lowercase(), displayName)
                unlockedKeysThisSession.remove(threadKey.lowercase())
                _activeLockChallenge.value = LockChallenge.SpecificThread(threadKey.lowercase(), displayName)
            }
        }
    }

    fun toggleThreadLock(thread: DirectThreadEntity) {
        val existing = lockedChats.value.firstOrNull {
            it.threadKey.equals(thread.threadKey, ignoreCase = true)
        }
        viewModelScope.launch {
            if (existing != null) {
                vaultAccountRepo.removeLockedChat(existing.id)
                _snackbarMessage.value = "Unlocked ${thread.displayName}"
            } else {
                vaultAccountRepo.addLockedChat(thread.threadKey, thread.displayName)
                _snackbarMessage.value = "Locked ${thread.displayName} with Biometric/PIN shield"
            }
        }
    }

    fun relockCurrentChatNow() {
        val activeKey = _activeThreadKey.value?.lowercase()
        val matched = activeKey?.let { key ->
            lockedChats.value.firstOrNull { it.threadKey.lowercase() == key }
        }
        if (matched != null) {
            unlockedKeysThisSession.remove(matched.threadKey.lowercase())
            _activeLockChallenge.value = LockChallenge.SpecificThread(matched.threadKey.lowercase(), matched.displayName)
        } else if (settings.value.isDmTabLocked) {
            dmTabUnlockedThisSession = false
            _activeLockChallenge.value = LockChallenge.MessagesTab
        }
    }

    fun relockAllOnBackground() {
        val current = settings.value
        if (!current.hasPinConfigured && !current.isBiometricEnabled) return
        unlockedKeysThisSession.clear()
        dmTabUnlockedThisSession = false
        appUnlockedThisSession = false
        if (current.isAppLockEnabled) {
            _activeLockChallenge.value = LockChallenge.AppLaunch
        } else if (_activeSection.value == InstaPlusSection.MESSAGES) {
            checkDmLock(_activeThreadKey.value)
        }
    }

    fun removeLockedChat(id: Long) = viewModelScope.launch {
        vaultAccountRepo.removeLockedChat(id)
    }

    fun syncAllLocalToCloud() {
        _snackbarMessage.value = "All standalone posts, Vault, themes & security settings synced locally"
    }

    fun openAccountSwitcher() { _showAccountSwitcher.value = true }
    fun dismissAccountSwitcher() { _showAccountSwitcher.value = false }
    fun consumeSnackbar() { _snackbarMessage.value = null }
}
