package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DefaultGhostModeRepository
import com.example.data.local.InstaPlusDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.local.SocialPlatformRepository
import com.example.data.model.GhostModePreferences
import com.example.data.model.GhostPrivacyCategory
import com.example.network.GhostNetworkRuleEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: InstaPlusDatabase
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var ghostRepo: DefaultGhostModeRepository
    private lateinit var socialRepo: SocialPlatformRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, InstaPlusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepo = PreferencesRepository(context)
        ghostRepo = DefaultGhostModeRepository(prefsRepo)
        socialRepo = SocialPlatformRepository(
            postDao = db.socialPostDao(),
            commentDao = db.postCommentDao(),
            storyDao = db.storyDao(),
            directChatDao = db.directChatDao(),
            ghostModeRepository = ghostRepo
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("InstaPlus", appName)
    }

    @Test
    fun `ghost mode blocks story seen and dm read receipts when enabled`() {
        val ghostPrefs = GhostModePreferences(
            hideStorySeen = true,
            hideDmReadReceipts = true,
            hideTypingIndicator = true,
            hideOnlineStatus = true
        )

        val storyDecision = GhostNetworkRuleEngine.evaluate(
            url = "https://example.invalid/api/v1/stories/reel/seen/",
            prefs = ghostPrefs
        )
        assertEquals(GhostPrivacyCategory.STORY_SEEN, storyDecision?.category)

        val dmDecision = GhostNetworkRuleEngine.evaluate(
            url = "https://example.invalid/api/v1/direct_v2/threads/123/items/456/seen/",
            prefs = ghostPrefs
        )
        assertEquals(GhostPrivacyCategory.DM_READ_RECEIPT, dmDecision?.category)

        val typingDecision = GhostNetworkRuleEngine.evaluate(
            url = "https://example.invalid/api/v1/direct_v2/threads/123/indicate_activity/",
            prefs = ghostPrefs
        )
        assertEquals(GhostPrivacyCategory.TYPING_INDICATOR, typingDecision?.category)

        assertNull(
            GhostNetworkRuleEngine.evaluate(
                url = "https://example.invalid/api/v1/feed/timeline/",
                prefs = ghostPrefs
            )
        )
    }

    @Test
    fun `standalone social platform supports feed seeding, polls, time capsules, self-destruct DMs, and ghost interception`() = runBlocking {
        socialRepo.ensureSeeded("alex_dev", "Alex Dev")

        val seededPosts = socialRepo.posts.first()
        assertTrue(seededPosts.isNotEmpty())

        // Publish Time Capsule post with poll
        val createdId = socialRepo.publishPost(
            authorUsername = "alex_dev",
            authorFullName = "Alex Dev",
            caption = "Standalone poll test",
            hashtags = "#InstaPlus",
            categoryTag = "Tech",
            mediaKey = "gradient:sunset",
            isReel = false,
            moodTag = "Focused",
            pollQuestion = "Tabs or Spaces?",
            pollOptionA = "Spaces",
            pollOptionB = "Tabs",
            ttlHours = 1
        )
        val createdPost = socialRepo.posts.first().firstOrNull { it.id == createdId }
        assertNotNull(createdPost)

        // Vote on poll
        socialRepo.votePoll(createdPost!!, choice = 1)
        val votedPost = socialRepo.posts.first().first { it.id == createdId }
        assertEquals(1, votedPost.userPollVote)
        assertEquals(1, votedPost.pollVotesA)

        // Ghost Story Viewing intercepts seen receipt instead of marking viewed
        val firstStory = socialRepo.stories.first().first()
        socialRepo.viewStoryRespectingGhostMode(firstStory)
        assertTrue(ghostRepo.interceptedRequests.value.any { it.category == GhostPrivacyCategory.STORY_SEEN })

        // Self-destructing DM purges cleanly once expired
        socialRepo.sendMessage("@aria_designs", "alex_dev", "Burn code 999", burnAfterSeconds = 1)
        assertTrue(socialRepo.messages.first().any { it.text == "Burn code 999" })
        socialRepo.purgeExpiredContent(System.currentTimeMillis() + 5_000L)
        assertFalse(socialRepo.messages.first().any { it.text == "Burn code 999" })
    }
}
