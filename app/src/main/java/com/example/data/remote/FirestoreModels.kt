package com.example.data.remote

import com.example.data.local.DownloadedMediaEntity
import com.example.data.model.ChatBackgroundPreset
import com.example.data.model.FontPreset
import com.example.data.model.InstaPlusSettings
import com.example.data.model.LauncherIconVariant
import com.example.data.model.ThemePreset
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest

@IgnoreExtraProperties
data class UserCloudProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "InstaPlus User",
    val email: String? = null,
    val highResPriority: Boolean = true,
    val autoDetectMedia: Boolean = true,
    val showFloatingActionDock: Boolean = true,
    val hideOnlineStatus: Boolean = true,
    val anonymousStoryViewing: Boolean = true,
    val disableReadReceipts: Boolean = true,
    val hideTypingIndicator: Boolean = true,
    val blockFeedAds: Boolean = true,
    val blockStoryAds: Boolean = true,
    val blockReelsAds: Boolean = true,
    val hideSuggestedPosts: Boolean = false,
    val themePreset: String = ThemePreset.INSTAGRAM_LIGHT.name,
    val fontPreset: String = FontPreset.INTER.name,
    val chatBackgroundPreset: String = ChatBackgroundPreset.DEFAULT.name,
    val hideReelsTab: Boolean = false,
    val launcherIconVariant: String = LauncherIconVariant.CLASSIC.name,
    val isDmTabLocked: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val targetLanguage: String = "English",
    val autoOfferTranslate: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        return mapOf(
            "userId" to userId,
            "displayName" to displayName.ifBlank { "InstaPlus User" }.take(80),
            "email" to email?.take(120),
            "highResPriority" to highResPriority,
            "autoDetectMedia" to autoDetectMedia,
            "showFloatingActionDock" to showFloatingActionDock,
            "hideOnlineStatus" to hideOnlineStatus,
            "anonymousStoryViewing" to anonymousStoryViewing,
            "disableReadReceipts" to disableReadReceipts,
            "hideTypingIndicator" to hideTypingIndicator,
            "blockFeedAds" to blockFeedAds,
            "blockStoryAds" to blockStoryAds,
            "blockReelsAds" to blockReelsAds,
            "hideSuggestedPosts" to hideSuggestedPosts,
            "themePreset" to themePreset,
            "fontPreset" to fontPreset,
            "chatBackgroundPreset" to chatBackgroundPreset,
            "hideReelsTab" to hideReelsTab,
            "launcherIconVariant" to launcherIconVariant,
            "isDmTabLocked" to isDmTabLocked,
            "isAppLockEnabled" to isAppLockEnabled,
            "isBiometricEnabled" to isBiometricEnabled,
            "targetLanguage" to targetLanguage.ifBlank { "English" }.take(40),
            "autoOfferTranslate" to autoOfferTranslate,
            "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        return mapOf(
            "displayName" to displayName.ifBlank { "InstaPlus User" }.take(80),
            "email" to email?.take(120),
            "highResPriority" to highResPriority,
            "autoDetectMedia" to autoDetectMedia,
            "showFloatingActionDock" to showFloatingActionDock,
            "hideOnlineStatus" to hideOnlineStatus,
            "anonymousStoryViewing" to anonymousStoryViewing,
            "disableReadReceipts" to disableReadReceipts,
            "hideTypingIndicator" to hideTypingIndicator,
            "blockFeedAds" to blockFeedAds,
            "blockStoryAds" to blockStoryAds,
            "blockReelsAds" to blockReelsAds,
            "hideSuggestedPosts" to hideSuggestedPosts,
            "themePreset" to themePreset,
            "fontPreset" to fontPreset,
            "chatBackgroundPreset" to chatBackgroundPreset,
            "hideReelsTab" to hideReelsTab,
            "launcherIconVariant" to launcherIconVariant,
            "isDmTabLocked" to isDmTabLocked,
            "isAppLockEnabled" to isAppLockEnabled,
            "isBiometricEnabled" to isBiometricEnabled,
            "targetLanguage" to targetLanguage.ifBlank { "English" }.take(40),
            "autoOfferTranslate" to autoOfferTranslate,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
    }

    companion object {
        fun fromSettings(
            userId: String,
            displayName: String,
            email: String?,
            settings: InstaPlusSettings,
            existingCreatedAt: Timestamp? = null
        ): UserCloudProfile = UserCloudProfile(
            id = userId,
            userId = userId,
            displayName = displayName.ifBlank { "InstaPlus User" }.take(80),
            email = email?.takeIf { it.isNotBlank() }?.take(120),
            highResPriority = settings.highResPriority,
            autoDetectMedia = settings.autoDetectMedia,
            showFloatingActionDock = settings.showFloatingActionDock,
            hideOnlineStatus = settings.hideOnlineStatus,
            anonymousStoryViewing = settings.anonymousStoryViewing,
            disableReadReceipts = settings.disableReadReceipts,
            hideTypingIndicator = settings.hideTypingIndicator,
            blockFeedAds = settings.blockFeedAds,
            blockStoryAds = settings.blockStoryAds,
            blockReelsAds = settings.blockReelsAds,
            hideSuggestedPosts = settings.hideSuggestedPosts,
            themePreset = settings.themePreset.name,
            fontPreset = settings.fontPreset.name,
            chatBackgroundPreset = settings.chatBackgroundPreset.name,
            hideReelsTab = settings.hideReelsTab,
            launcherIconVariant = settings.launcherIconVariant.name,
            isDmTabLocked = settings.isDmTabLocked,
            isAppLockEnabled = settings.isAppLockEnabled,
            isBiometricEnabled = settings.isBiometricEnabled,
            targetLanguage = settings.targetLanguage.ifBlank { "English" }.take(40),
            autoOfferTranslate = settings.autoOfferTranslate,
            createdAt = existingCreatedAt
        )
    }
}

@IgnoreExtraProperties
data class CloudVaultItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mediaType: String = "PHOTO",
    val authorHandle: String = "instaplus_creator",
    val captionSnippet: String = "",
    val sourceUrl: String = "",
    val fileName: String = "",
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "mediaType" to mediaType,
        "authorHandle" to authorHandle.ifBlank { "instaplus_creator" }.take(64),
        "captionSnippet" to captionSnippet.take(500),
        "sourceUrl" to sourceUrl.ifBlank { "instaplus://vault" }.take(2048),
        "fileName" to fileName.ifBlank { "media_item" }.take(180),
        "mimeType" to mimeType.ifBlank { "application/octet-stream" }.take(80),
        "fileSizeBytes" to fileSizeBytes.coerceAtLeast(0L),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        fun fromEntity(userId: String, entity: DownloadedMediaEntity): CloudVaultItem {
            return CloudVaultItem(
                id = sanitizeCloudId("${entity.fileName}_${entity.savedAt}"),
                userId = userId,
                mediaType = entity.mediaType.takeIf {
                    it in setOf("PHOTO", "VIDEO", "REEL", "STORY", "AVATAR")
                } ?: "PHOTO",
                authorHandle = entity.authorHandle.ifBlank { "instaplus_creator" }.take(64),
                captionSnippet = entity.captionSnippet.take(500),
                sourceUrl = entity.sourceUrl.ifBlank { "instaplus://vault" }.take(2048),
                fileName = entity.fileName.ifBlank { "instaplus_media" }.take(180),
                mimeType = entity.mimeType.ifBlank { "image/jpeg" }.take(80),
                fileSizeBytes = entity.fileSizeBytes.coerceAtLeast(0L)
            )
        }
    }
}

@IgnoreExtraProperties
data class CloudLockedChat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val threadKey: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "threadKey" to threadKey.take(128),
        "displayName" to displayName.ifBlank { threadKey }.take(80),
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.ifBlank { threadKey }.take(80),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CloudAccountProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val isActive: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username.take(64),
        "displayName" to displayName.ifBlank { username }.take(80),
        "isActive" to isActive,
        "createdAt" to (createdAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.ifBlank { username }.take(80),
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

fun sanitizeCloudId(rawKey: String): String {
    val clean = rawKey.trim().lowercase().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
    if (clean.isNotBlank() && clean.length <= 96 && clean.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
        return clean
    }
    val digest = MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray(Charsets.UTF_8))
    return "id_" + digest.joinToString("") { "%02x".format(it) }.take(40)
}
