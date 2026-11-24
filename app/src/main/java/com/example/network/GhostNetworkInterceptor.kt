package com.example.network

import com.example.data.local.GhostModeRepository
import com.example.data.model.GhostInterceptionDecision
import com.example.data.model.GhostModePreferences
import com.example.data.model.GhostPrivacyCategory
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * Canonical rule engine evaluating URLs and request payloads for Story 'seen',
 * Direct Message 'seen' read receipts, typing indicators, and online presence pings.
 */
object GhostNetworkRuleEngine {

    private val storySeenTokens = listOf(
        "/stories/reel/seen",
        "/stories/seen",
        "stories_seen",
        "reel_seen",
        "polarisstoriesv3seen",
        "storiesv3seen",
        "viewseenat"
    )

    private val dmSeenTokens = listOf(
        "mark_seen",
        "item_seen",
        "thread_seen",
        "mark_thread_seen",
        "direct_v2_mark_seen"
    )

    private val typingTokens = listOf(
        "indicate_activity",
        "typing_indicator",
        "is_typing"
    )

    private val presenceTokens = listOf(
        "/presence",
        "get_presence",
        "active_status",
        "presence_status"
    )

    fun evaluate(
        url: String,
        method: String = "POST",
        bodyPayload: String? = null,
        prefs: GhostModePreferences
    ): GhostInterceptionDecision? {
        val lowerUrl = url.lowercase()
        val lowerBody = bodyPayload.orEmpty().lowercase()

        if (prefs.hideStorySeen) {
            val matchedStory = storySeenTokens.any { lowerUrl.contains(it) || lowerBody.contains(it) } ||
                (lowerUrl.contains("stor") && lowerUrl.contains("/seen"))
            if (matchedStory) {
                return GhostInterceptionDecision(
                    category = GhostPrivacyCategory.STORY_SEEN,
                    reason = "Blocked Story 'seen' receipt ($method)"
                )
            }
        }

        if (prefs.hideDmReadReceipts) {
            val matchedDmPath = (lowerUrl.contains("direct") && lowerUrl.contains("/seen")) ||
                (lowerBody.contains("direct_v2") && lowerBody.contains("seen"))
            val matchedDmToken = dmSeenTokens.any { lowerUrl.contains(it) || lowerBody.contains(it) }
            if (matchedDmPath || matchedDmToken) {
                return GhostInterceptionDecision(
                    category = GhostPrivacyCategory.DM_READ_RECEIPT,
                    reason = "Blocked DM 'seen' read receipt ($method)"
                )
            }
        }

        if (prefs.hideTypingIndicator) {
            val matchedTyping = typingTokens.any { lowerUrl.contains(it) || lowerBody.contains(it) }
            if (matchedTyping) {
                return GhostInterceptionDecision(
                    category = GhostPrivacyCategory.TYPING_INDICATOR,
                    reason = "Suppressed typing indicator ping ($method)"
                )
            }
        }

        if (prefs.hideOnlineStatus) {
            val matchedPresence = presenceTokens.any { lowerUrl.contains(it) || lowerBody.contains(it) }
            if (matchedPresence) {
                return GhostInterceptionDecision(
                    category = GhostPrivacyCategory.ONLINE_PRESENCE,
                    reason = "Masked online presence heartbeat ($method)"
                )
            }
        }

        return null
    }
}

/**
 * OkHttp application-layer Interceptor that checks outgoing HTTP requests against
 * [GhostModeRepository] preferences and short-circuits Story/DM 'seen', typing,
 * and presence telemetry with a synthetic 200 OK JSON response before hitting the network.
 */
class GhostModeOkHttpInterceptor(
    private val repository: GhostModeRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method
        val prefs = repository.currentPreferencesSnapshot()

        val bodySnippet = peekTextBody(request.body)
        val decision = GhostNetworkRuleEngine.evaluate(
            url = url,
            method = method,
            bodyPayload = bodySnippet,
            prefs = prefs
        )

        if (decision != null) {
            repository.recordInterceptedRequest(
                endpoint = url,
                category = decision.category,
                method = method,
                transport = "OkHttp",
                reason = decision.reason
            )
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(decision.syntheticStatusCode)
                .message("OK")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("X-InstaPlus-Ghost-Blocked", decision.category.name)
                .body(decision.syntheticJsonBody.toResponseBody("application/json; charset=utf-8".toMediaType()))
                .build()
        }

        return chain.proceed(request)
    }

    private fun peekTextBody(body: RequestBody?): String? {
        if (body == null || body.isOneShot() || body.isDuplex()) return null
        val contentType = body.contentType()?.toString()?.lowercase().orEmpty()
        val isInspectableText = contentType.isBlank() ||
            contentType.contains("json") ||
            contentType.contains("form") ||
            contentType.contains("text") ||
            contentType.contains("graphql")
        if (!isInspectableText) return null

        return runCatching {
            val contentLength = body.contentLength()
            if (contentLength > MAX_INSPECT_BYTES) return null
            val buffer = Buffer()
            body.writeTo(buffer)
            val byteCount = minOf(buffer.size, MAX_INSPECT_BYTES.toLong())
            buffer.readUtf8(byteCount)
        }.getOrNull()
    }

    companion object {
        private const val MAX_INSPECT_BYTES = 16 * 1024
    }
}
