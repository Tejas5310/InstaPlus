package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AccentColorPreset
import com.example.data.model.ChatBackgroundPreset
import com.example.data.model.FontPreset
import com.example.data.model.InstaPlusSettings
import com.example.data.model.LauncherIconVariant
import com.example.data.model.ThemePreset
import com.example.data.remote.UserCloudProfile
import com.example.security.SecureLocalCipher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "instaplus_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val HIGH_RES_PRIORITY = booleanPreferencesKey("high_res_priority")
        val AUTO_DETECT_MEDIA = booleanPreferencesKey("auto_detect_media")
        val SHOW_FLOATING_DOCK = booleanPreferencesKey("show_floating_dock")

        val HIDE_ONLINE_STATUS = booleanPreferencesKey("hide_online_status")
        val ANONYMOUS_STORY_VIEWING = booleanPreferencesKey("anonymous_story_viewing")
        val DISABLE_READ_RECEIPTS = booleanPreferencesKey("disable_read_receipts")
        val HIDE_TYPING_INDICATOR = booleanPreferencesKey("hide_typing_indicator")
        val LOG_GHOST_INTERCEPTS = booleanPreferencesKey("log_ghost_intercepts")

        val BLOCK_FEED_ADS = booleanPreferencesKey("block_feed_ads")
        val BLOCK_STORY_ADS = booleanPreferencesKey("block_story_ads")
        val BLOCK_REELS_ADS = booleanPreferencesKey("block_reels_ads")
        val HIDE_SUGGESTED_POSTS = booleanPreferencesKey("hide_suggested_posts")

        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val PRIMARY_ACCENT = stringPreferencesKey("primary_accent_preset")
        val FONT_PRESET = stringPreferencesKey("font_preset")
        val CHAT_BG_PRESET = stringPreferencesKey("chat_bg_preset")
        val CUSTOM_CHAT_BG_URI = stringPreferencesKey("custom_chat_bg_uri")
        val HIDE_REELS_TAB = booleanPreferencesKey("hide_reels_tab")
        val LAUNCHER_ICON_VARIANT = stringPreferencesKey("launcher_icon_variant")

        val IS_DM_TAB_LOCKED = booleanPreferencesKey("is_dm_tab_locked")
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")

        val TARGET_LANGUAGE = stringPreferencesKey("target_language")
        val AUTO_OFFER_TRANSLATE = booleanPreferencesKey("auto_offer_translate")

        val FOCUS_GUARD_ENABLED = booleanPreferencesKey("focus_guard_enabled")
        val FOCUS_LIMIT_MINUTES = intPreferencesKey("focus_limit_minutes")
    }

    val settingsFlow: Flow<InstaPlusSettings> = context.dataStore.data.map { prefs ->
        InstaPlusSettings(
            highResPriority = prefs[Keys.HIGH_RES_PRIORITY] ?: true,
            autoDetectMedia = prefs[Keys.AUTO_DETECT_MEDIA] ?: true,
            showFloatingActionDock = prefs[Keys.SHOW_FLOATING_DOCK] ?: true,
            hideOnlineStatus = prefs[Keys.HIDE_ONLINE_STATUS] ?: true,
            anonymousStoryViewing = prefs[Keys.ANONYMOUS_STORY_VIEWING] ?: true,
            disableReadReceipts = prefs[Keys.DISABLE_READ_RECEIPTS] ?: true,
            hideTypingIndicator = prefs[Keys.HIDE_TYPING_INDICATOR] ?: true,
            logGhostIntercepts = prefs[Keys.LOG_GHOST_INTERCEPTS] ?: true,
            blockFeedAds = prefs[Keys.BLOCK_FEED_ADS] ?: true,
            blockStoryAds = prefs[Keys.BLOCK_STORY_ADS] ?: true,
            blockReelsAds = prefs[Keys.BLOCK_REELS_ADS] ?: true,
            hideSuggestedPosts = prefs[Keys.HIDE_SUGGESTED_POSTS] ?: false,
            isDarkMode = prefs[Keys.IS_DARK_MODE]
                ?: enumValueOrDefault(prefs[Keys.THEME_PRESET], ThemePreset.INSTAGRAM_LIGHT).isDark,
            themePreset = enumValueOrDefault(prefs[Keys.THEME_PRESET], ThemePreset.INSTAGRAM_LIGHT),
            accentPreset = enumValueOrDefault(prefs[Keys.PRIMARY_ACCENT], AccentColorPreset.INSTAGRAM_BLUE),
            fontPreset = enumValueOrDefault(prefs[Keys.FONT_PRESET], FontPreset.INTER),
            chatBackgroundPreset = enumValueOrDefault(prefs[Keys.CHAT_BG_PRESET], ChatBackgroundPreset.DEFAULT),
            customChatBackgroundUri = prefs[Keys.CUSTOM_CHAT_BG_URI],
            hideReelsTab = prefs[Keys.HIDE_REELS_TAB] ?: false,
            launcherIconVariant = enumValueOrDefault(prefs[Keys.LAUNCHER_ICON_VARIANT], LauncherIconVariant.CLASSIC),
            isDmTabLocked = prefs[Keys.IS_DM_TAB_LOCKED] ?: false,
            isAppLockEnabled = prefs[Keys.IS_APP_LOCK_ENABLED] ?: false,
            isBiometricEnabled = prefs[Keys.IS_BIOMETRIC_ENABLED] ?: true,
            pinHash = prefs[Keys.PIN_HASH],
            pinSalt = prefs[Keys.PIN_SALT],
            targetLanguage = prefs[Keys.TARGET_LANGUAGE] ?: "English",
            autoOfferTranslate = prefs[Keys.AUTO_OFFER_TRANSLATE] ?: true,
            focusGuardEnabled = prefs[Keys.FOCUS_GUARD_ENABLED] ?: false,
            focusLimitMinutes = (prefs[Keys.FOCUS_LIMIT_MINUTES] ?: 20).coerceIn(5, 120)
        )
    }

    suspend fun getCurrentSettings(): InstaPlusSettings = settingsFlow.first()

    suspend fun applyCloudProfile(profile: UserCloudProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGH_RES_PRIORITY] = profile.highResPriority
            prefs[Keys.AUTO_DETECT_MEDIA] = profile.autoDetectMedia
            prefs[Keys.SHOW_FLOATING_DOCK] = profile.showFloatingActionDock
            prefs[Keys.HIDE_ONLINE_STATUS] = profile.hideOnlineStatus
            prefs[Keys.ANONYMOUS_STORY_VIEWING] = profile.anonymousStoryViewing
            prefs[Keys.DISABLE_READ_RECEIPTS] = profile.disableReadReceipts
            prefs[Keys.HIDE_TYPING_INDICATOR] = profile.hideTypingIndicator
            prefs[Keys.BLOCK_FEED_ADS] = profile.blockFeedAds
            prefs[Keys.BLOCK_STORY_ADS] = profile.blockStoryAds
            prefs[Keys.BLOCK_REELS_ADS] = profile.blockReelsAds
            prefs[Keys.HIDE_SUGGESTED_POSTS] = profile.hideSuggestedPosts
            val mappedPreset = enumValueOrDefault(profile.themePreset, ThemePreset.INSTAGRAM_LIGHT)
            prefs[Keys.THEME_PRESET] = mappedPreset.name
            prefs[Keys.IS_DARK_MODE] = mappedPreset.isDark
            prefs[Keys.FONT_PRESET] = enumValueOrDefault(profile.fontPreset, FontPreset.INTER).name
            prefs[Keys.CHAT_BG_PRESET] = enumValueOrDefault(profile.chatBackgroundPreset, ChatBackgroundPreset.DEFAULT).name
            prefs[Keys.HIDE_REELS_TAB] = profile.hideReelsTab
            prefs[Keys.LAUNCHER_ICON_VARIANT] = enumValueOrDefault(profile.launcherIconVariant, LauncherIconVariant.CLASSIC).name
            prefs[Keys.IS_DM_TAB_LOCKED] = profile.isDmTabLocked
            prefs[Keys.IS_APP_LOCK_ENABLED] = profile.isAppLockEnabled
            prefs[Keys.IS_BIOMETRIC_ENABLED] = profile.isBiometricEnabled
            prefs[Keys.TARGET_LANGUAGE] = profile.targetLanguage
            prefs[Keys.AUTO_OFFER_TRANSLATE] = profile.autoOfferTranslate
        }
    }

    suspend fun updateBoolean(keyName: String, value: Boolean) {
        val key = when (keyName) {
            "high_res_priority" -> Keys.HIGH_RES_PRIORITY
            "auto_detect_media" -> Keys.AUTO_DETECT_MEDIA
            "show_floating_dock" -> Keys.SHOW_FLOATING_DOCK
            "hide_online_status" -> Keys.HIDE_ONLINE_STATUS
            "anonymous_story_viewing" -> Keys.ANONYMOUS_STORY_VIEWING
            "disable_read_receipts" -> Keys.DISABLE_READ_RECEIPTS
            "hide_typing_indicator" -> Keys.HIDE_TYPING_INDICATOR
            "log_ghost_intercepts" -> Keys.LOG_GHOST_INTERCEPTS
            "block_feed_ads" -> Keys.BLOCK_FEED_ADS
            "block_story_ads" -> Keys.BLOCK_STORY_ADS
            "block_reels_ads" -> Keys.BLOCK_REELS_ADS
            "hide_suggested_posts" -> Keys.HIDE_SUGGESTED_POSTS
            "hide_reels_tab" -> Keys.HIDE_REELS_TAB
            "is_dm_tab_locked" -> Keys.IS_DM_TAB_LOCKED
            "is_app_lock_enabled" -> Keys.IS_APP_LOCK_ENABLED
            "is_biometric_enabled" -> Keys.IS_BIOMETRIC_ENABLED
            "auto_offer_translate" -> Keys.AUTO_OFFER_TRANSLATE
            "focus_guard_enabled" -> Keys.FOCUS_GUARD_ENABLED
            else -> return
        }
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setFocusLimitMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.FOCUS_LIMIT_MINUTES] = minutes.coerceIn(5, 120) }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_DARK_MODE] = enabled
            val currentPreset = enumValueOrDefault(prefs[Keys.THEME_PRESET], ThemePreset.INSTAGRAM_LIGHT)
            if (enabled && !currentPreset.isDark) {
                prefs[Keys.THEME_PRESET] = ThemePreset.AMOLED_DARK.name
            } else if (!enabled && currentPreset.isDark) {
                prefs[Keys.THEME_PRESET] = ThemePreset.INSTAGRAM_LIGHT.name
            }
        }
    }

    suspend fun setThemePreset(preset: ThemePreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = preset.name
            prefs[Keys.IS_DARK_MODE] = preset.isDark
        }
    }

    suspend fun setAccentPreset(accent: AccentColorPreset) {
        context.dataStore.edit { it[Keys.PRIMARY_ACCENT] = accent.name }
    }

    suspend fun setFontPreset(preset: FontPreset) {
        context.dataStore.edit { it[Keys.FONT_PRESET] = preset.name }
    }

    suspend fun setChatBackground(preset: ChatBackgroundPreset, customUri: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CHAT_BG_PRESET] = preset.name
            if (customUri != null) {
                prefs[Keys.CUSTOM_CHAT_BG_URI] = customUri
            }
        }
    }

    suspend fun setLauncherIconVariant(variant: LauncherIconVariant) {
        context.dataStore.edit { it[Keys.LAUNCHER_ICON_VARIANT] = variant.name }
    }

    suspend fun setTargetLanguage(language: String) {
        context.dataStore.edit { it[Keys.TARGET_LANGUAGE] = language }
    }

    suspend fun setPin(pin: String) {
        val salt = SecureLocalCipher.generateSaltBase64()
        val hash = SecureLocalCipher.hashPin(pin, salt)
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PIN_HASH)
            prefs.remove(Keys.PIN_SALT)
            prefs[Keys.IS_DM_TAB_LOCKED] = false
            prefs[Keys.IS_APP_LOCK_ENABLED] = false
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val storedHash = prefs[Keys.PIN_HASH] ?: return false
        val salt = prefs[Keys.PIN_SALT] ?: return false
        return SecureLocalCipher.verifyPin(pin, salt, storedHash)
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
