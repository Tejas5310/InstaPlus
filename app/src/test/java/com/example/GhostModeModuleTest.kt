package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DefaultGhostModeRepository
import com.example.data.local.PreferencesRepository
import com.example.data.model.GhostPrivacyCategory
import com.example.network.GhostModeOkHttpInterceptor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GhostModeModuleTest {

    @Test
    fun `ghost mode repository manages and persists privacy preferences`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        val ghostRepo = DefaultGhostModeRepository(prefsRepo)

        ghostRepo.setMasterGhostMode(true)
        val full = ghostRepo.privacyPreferences.first()
        assertTrue(full.isAllActive)
        assertEquals(4, full.activeRuleCount)

        ghostRepo.setStorySeenHidden(false)
        ghostRepo.setDmReadReceiptsHidden(true)
        val partial = ghostRepo.privacyPreferences.first()
        assertFalse(partial.hideStorySeen)
        assertTrue(partial.hideDmReadReceipts)
        assertEquals(3, partial.activeRuleCount)

        ghostRepo.setMasterGhostMode(false)
        val paused = ghostRepo.privacyPreferences.first()
        assertFalse(paused.isAnyActive)
        assertEquals(0, paused.activeRuleCount)
    }

    @Test
    fun `okhttp ghost interceptor blocks story and dm seen calls and logs telemetry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefsRepo = PreferencesRepository(context)
        val ghostRepo = DefaultGhostModeRepository(prefsRepo)
        ghostRepo.setMasterGhostMode(true)
        ghostRepo.clearInterceptedLog()

        val upstreamHits = AtomicInteger(0)
        val ghostInterceptor = GhostModeOkHttpInterceptor(ghostRepo)
        val client = OkHttpClient.Builder()
            .addInterceptor(ghostInterceptor)
            .addInterceptor(Interceptor { chain ->
                upstreamHits.incrementAndGet()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{\"upstream\":true}".toResponseBody("application/json".toMediaType()))
                    .build()
            })
            .build()

        // 1. Story seen REST ping should be intercepted locally (upstreamHits stays 0)
        val storyReq = Request.Builder()
            .url("https://www.instagram.com/api/v1/stories/reel/seen/")
            .post("{\"reel_id\":\"123\"}".toRequestBody("application/json".toMediaType()))
            .build()
        val storyResp = client.newCall(storyReq).execute()
        assertEquals(200, storyResp.code)
        assertEquals("STORY_SEEN", storyResp.header("X-InstaPlus-Ghost-Blocked"))
        assertTrue(storyResp.body?.string()?.contains("ghost_intercepted") == true)
        assertEquals(0, upstreamHits.get())

        // 2. Story GraphQL PolarisStoriesV3SeenMutation payload should be intercepted
        val gqlReq = Request.Builder()
            .url("https://www.instagram.com/api/graphql")
            .post("fb_api_req_friendly_name=PolarisStoriesV3SeenMutation".toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()
        val gqlResp = client.newCall(gqlReq).execute()
        assertEquals("STORY_SEEN", gqlResp.header("X-InstaPlus-Ghost-Blocked"))
        assertEquals(0, upstreamHits.get())

        // 3. DM read receipt seen endpoint should be intercepted locally
        val dmReq = Request.Builder()
            .url("https://www.instagram.com/api/v1/direct_v2/threads/555/items/777/seen/")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val dmResp = client.newCall(dmReq).execute()
        assertEquals("DM_READ_RECEIPT", dmResp.header("X-InstaPlus-Ghost-Blocked"))
        assertEquals(0, upstreamHits.get())

        // 4. Normal feed timeline request passes straight through to upstream
        val feedReq = Request.Builder()
            .url("https://www.instagram.com/api/v1/feed/timeline/")
            .get()
            .build()
        val feedResp = client.newCall(feedReq).execute()
        assertEquals(200, feedResp.code)
        assertTrue(feedResp.body?.string()?.contains("upstream") == true)
        assertEquals(1, upstreamHits.get())

        // Telemetry counts match 2 story + 1 DM
        val counts = ghostRepo.blockedCountsByCategory.value
        assertEquals(2, counts[GhostPrivacyCategory.STORY_SEEN])
        assertEquals(1, counts[GhostPrivacyCategory.DM_READ_RECEIPT])
        assertEquals(3, ghostRepo.interceptedRequests.value.size)

        // 5. Disabling DM read receipt protection allows DM seen through upstream
        ghostRepo.setDmReadReceiptsHidden(false)
        val dmPassResp = client.newCall(dmReq).execute()
        assertTrue(dmPassResp.body?.string()?.contains("upstream") == true)
        assertEquals(2, upstreamHits.get())
    }
}
