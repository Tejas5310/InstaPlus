package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class ThemePreset(
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean,
    val previewPrimary: Color,
    val previewBackground: Color
) {
    INSTAGRAM_LIGHT(
        displayName = "InstaPlus Classic",
        subtitle = "Crisp white surface & Prism Blue",
        isDark = false,
        previewPrimary = Color(0xFF0095F6),
        previewBackground = Color(0xFFFFFFFF)
    ),
    AMOLED_DARK(
        displayName = "AMOLED Pitch Black",
        subtitle = "True #000000 OLED dark mode",
        isDark = true,
        previewPrimary = Color(0xFF0095F6),
        previewBackground = Color(0xFF000000)
    ),
    SUNSET_ROSE(
        displayName = "Sunset Rose",
        subtitle = "Warm coral-magenta glow",
        isDark = false,
        previewPrimary = Color(0xFFDD2A7B),
        previewBackground = Color(0xFFFFF8FA)
    ),
    MIDNIGHT_VIOLET(
        displayName = "Midnight Violet",
        subtitle = "Deep obsidian & electric purple",
        isDark = true,
        previewPrimary = Color(0xFFA855F7),
        previewBackground = Color(0xFF0B0714)
    ),
    EDITORIAL_SLATE(
        displayName = "Editorial Slate",
        subtitle = "High-contrast warm ink & paper",
        isDark = false,
        previewPrimary = Color(0xFF18181B),
        previewBackground = Color(0xFFF6F5F1)
    )
}

enum class AccentColorPreset(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    INSTAGRAM_BLUE("Prism Blue", Color(0xFF0095F6), Color(0xFF515BD4)),
    PRISM_MAGENTA("Prism Pink", Color(0xFFDD2A7B), Color(0xFFF58529)),
    ROYAL_VIOLET("Royal Violet", Color(0xFF9333EA), Color(0xFFDD2A7B)),
    SUNSET_AMBER("Sunset Gold", Color(0xFFF58529), Color(0xFFED4956)),
    EMERALD_TEAL("Emerald", Color(0xFF10B981), Color(0xFF0095F6)),
    CRIMSON_CORAL("Coral Red", Color(0xFFED4956), Color(0xFFF58529))
}

enum class FontPreset(
    val displayName: String,
    val cssName: String
) {
    INTER("Inter (Default)", "'Inter', -apple-system, sans-serif"),
    PLUS_JAKARTA_SANS("Plus Jakarta Sans", "'Plus Jakarta Sans', sans-serif"),
    PLAYFAIR_DISPLAY("Playfair Display", "'Playfair Display', Georgia, serif"),
    PACIFICO("Pacifico", "'Pacifico', cursive"),
    ROBOTO_MONO("Roboto Mono", "'Roboto Mono', monospace")
}

enum class ChatBackgroundPreset(
    val displayName: String,
    val cssTint: String
) {
    DEFAULT("Clean Default", "transparent"),
    PRISM_SILK("Prism Silk", "prism"),
    AMOLED_PITCH("Pure Midnight", "midnight"),
    SOFT_ROSE("Blush Tint", "rose"),
    CUSTOM_PHOTO("Custom Photo", "custom")
}

enum class LauncherIconVariant(
    val displayName: String,
    val aliasClassName: String,
    val previewAccent: Color,
    val previewBg: Color
) {
    CLASSIC("Classic Prism", "com.example.LauncherClassic", Color(0xFFDD2A7B), Color(0xFF110A1E)),
    OBSIDIAN("Obsidian Stealth", "com.example.LauncherObsidian", Color(0xFFA855F7), Color(0xFF09090B)),
    MINIMAL("Daylight Minimal", "com.example.LauncherMinimal", Color(0xFF111111), Color(0xFFFAFAFA)),
    SUNSET("Sunset Vapor", "com.example.LauncherSunset", Color(0xFFF58529), Color(0xFFDD2A7B))
}

enum class MediaType(val label: String) {
    PHOTO("Photo"),
    VIDEO("Video"),
    REEL("Reel"),
    STORY("Story"),
    AVATAR("Profile Pic")
}

enum class InstaPlusSection(val label: String) {
    HOME("Home"),
    SEARCH("Explore"),
    REELS("Reels"),
    MESSAGES("Messages"),
    PROFILE("Profile")
}

data class DetectedMediaItem(
    val url: String,
    val mediaType: MediaType,
    val authorHandle: String = "instaplus_creator",
    val caption: String = "",
    val isVideo: Boolean = false
)

data class InstaPlusSettings(
    // Media Downloader
    val highResPriority: Boolean = true,
    val autoDetectMedia: Boolean = true,
    val showFloatingActionDock: Boolean = true,
    // Ghost Mode & Privacy
    val hideOnlineStatus: Boolean = true,
    val anonymousStoryViewing: Boolean = true,
    val disableReadReceipts: Boolean = true,
    val hideTypingIndicator: Boolean = true,
    val logGhostIntercepts: Boolean = true,
    // Ad-Free Feed
    val blockFeedAds: Boolean = true,
    val blockStoryAds: Boolean = true,
    val blockReelsAds: Boolean = true,
    val hideSuggestedPosts: Boolean = false,
    // UI & Theme Customization
    val isDarkMode: Boolean = false,
    val themePreset: ThemePreset = ThemePreset.INSTAGRAM_LIGHT,
    val accentPreset: AccentColorPreset = AccentColorPreset.INSTAGRAM_BLUE,
    val fontPreset: FontPreset = FontPreset.INTER,
    val chatBackgroundPreset: ChatBackgroundPreset = ChatBackgroundPreset.DEFAULT,
    val customChatBackgroundUri: String? = null,
    val hideReelsTab: Boolean = false,
    val launcherIconVariant: LauncherIconVariant = LauncherIconVariant.CLASSIC,
    // Chat Lock & Security
    val isDmTabLocked: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    // Translation
    val targetLanguage: String = "English",
    val autoOfferTranslate: Boolean = true,
    // Exclusive InstaPlus Mindful Focus Guard
    val focusGuardEnabled: Boolean = false,
    val focusLimitMinutes: Int = 20
) {
    val isGhostModeAnyActive: Boolean
        get() = hideOnlineStatus || anonymousStoryViewing || disableReadReceipts || hideTypingIndicator

    val isAdBlockAnyActive: Boolean
        get() = blockFeedAds || blockStoryAds || blockReelsAds

    val hasPinConfigured: Boolean
        get() = !pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank()
}
