package com.example.data.model

enum class GhostPrivacyCategory(
    val displayName: String,
    val shortBadge: String,
    val ruleSummary: String
) {
    STORY_SEEN(
        displayName = "Story Seen Receipt",
        shortBadge = "STORY",
        ruleSummary = "/stories/reel/seen, PolarisStoriesV3Seen"
    ),
    DM_READ_RECEIPT(
        displayName = "DM Read Receipt",
        shortBadge = "DM SEEN",
        ruleSummary = "/direct_v2/.../seen, mark_thread_seen"
    ),
    TYPING_INDICATOR(
        displayName = "Typing Indicator",
        shortBadge = "TYPING",
        ruleSummary = "indicate_activity, is_typing"
    ),
    ONLINE_PRESENCE(
        displayName = "Online Presence",
        shortBadge = "ONLINE",
        ruleSummary = "/presence, active_status"
    )
}

data class GhostModePreferences(
    val hideStorySeen: Boolean = true,
    val hideDmReadReceipts: Boolean = true,
    val hideTypingIndicator: Boolean = true,
    val hideOnlineStatus: Boolean = true,
    val logBlockedRequests: Boolean = true
) {
    val isAnyActive: Boolean
        get() = hideStorySeen || hideDmReadReceipts || hideTypingIndicator || hideOnlineStatus

    val isAllActive: Boolean
        get() = hideStorySeen && hideDmReadReceipts && hideTypingIndicator && hideOnlineStatus

    val activeRuleCount: Int
        get() = listOf(
            hideStorySeen,
            hideDmReadReceipts,
            hideTypingIndicator,
            hideOnlineStatus
        ).count { it }

    companion object {
        fun fromSettings(settings: InstaPlusSettings): GhostModePreferences = GhostModePreferences(
            hideStorySeen = settings.anonymousStoryViewing,
            hideDmReadReceipts = settings.disableReadReceipts,
            hideTypingIndicator = settings.hideTypingIndicator,
            hideOnlineStatus = settings.hideOnlineStatus,
            logBlockedRequests = settings.logGhostIntercepts
        )
    }
}

data class InterceptedPrivacyRequest(
    val id: Long,
    val timestampMillis: Long,
    val category: GhostPrivacyCategory,
    val method: String,
    val endpoint: String,
    val transport: String,
    val reason: String
)

data class GhostInterceptionDecision(
    val category: GhostPrivacyCategory,
    val reason: String,
    val syntheticStatusCode: Int = 200,
    val syntheticJsonBody: String = "{\"status\":\"ok\",\"ghost_intercepted\":true}"
)
