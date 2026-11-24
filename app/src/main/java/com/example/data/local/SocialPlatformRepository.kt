package com.example.data.local

import com.example.data.model.GhostPrivacyCategory
import kotlinx.coroutines.flow.Flow

class SocialPlatformRepository(
    private val postDao: SocialPostDao,
    private val commentDao: PostCommentDao,
    private val storyDao: StoryDao,
    private val directChatDao: DirectChatDao,
    private val ghostModeRepository: GhostModeRepository
) {
    val posts: Flow<List<SocialPostEntity>> = postDao.observeAll()
    val comments: Flow<List<PostCommentEntity>> = commentDao.observeAll()
    val stories: Flow<List<StoryEntity>> = storyDao.observeAll()
    val threads: Flow<List<DirectThreadEntity>> = directChatDao.observeThreads()
    val messages: Flow<List<DirectMessageEntity>> = directChatDao.observeAllMessages()

    suspend fun ensureSeeded(currentUsername: String, currentFullName: String) {
        val now = System.currentTimeMillis()
        purgeExpiredContent(now)

        if (postDao.count() == 0) {
            val initialPosts = listOf(
                SocialPostEntity(
                    authorUsername = "kael_travels",
                    authorFullName = "Kael Sato",
                    authorAvatarColorHex = 0xFFDD2A7B,
                    caption = "Rain-washed neon alleys after midnight—when city lights turn wet cobblestones into a prism canvas.",
                    hashtags = "#NeonNights #StreetPhotography #NightWalk #InstaPlus",
                    categoryTag = "Travel",
                    mediaKey = "drawable:img_post_neon_tokyo",
                    likesCount = 1284,
                    isLiked = false,
                    isBookmarked = false,
                    isFollowingAuthor = true,
                    moodTag = "Wanderlust",
                    pollQuestion = "Which night edit vibe fits best?",
                    pollOptionA = "Cyberpunk Neon",
                    pollOptionB = "Analog Film",
                    pollVotesA = 142,
                    pollVotesB = 89,
                    createdAtMillis = now - 3_600_000L * 2
                ),
                SocialPostEntity(
                    authorUsername = "elena_alpine",
                    authorFullName = "Elena Vance",
                    authorAvatarColorHex = 0xFF10B981,
                    caption = "First light over the glacial ridge before the valley wakes up. Breathing crisp mountain air without a single notification.",
                    hashtags = "#AlpineMorning #GoldenHour #MindfulTravel #Nature",
                    categoryTag = "Nature",
                    mediaKey = "drawable:img_post_alpine_sunrise",
                    likesCount = 946,
                    isFollowingAuthor = true,
                    moodTag = "Grateful",
                    expiresAtMillis = now + 86_400_000L, // 24h Time Capsule showcase
                    createdAtMillis = now - 3_600_000L * 5
                ),
                SocialPostEntity(
                    authorUsername = "studio_nova",
                    authorFullName = "Nova Craft",
                    authorAvatarColorHex = 0xFFF58529,
                    caption = "Morning pour-over & analog sketches before coding. Clean desk, zero ad clutter, 100% creative focus.",
                    hashtags = "#CreatorDesk #MorningRitual #Minimalist #Design",
                    categoryTag = "Design",
                    mediaKey = "drawable:img_post_studio_desk",
                    likesCount = 612,
                    isFollowingAuthor = false,
                    moodTag = "Focused",
                    pollQuestion = "Best morning fuel for deep work?",
                    pollOptionA = "Pour-Over Coffee",
                    pollOptionB = "Matcha Latte",
                    pollVotesA = 95,
                    pollVotesB = 64,
                    createdAtMillis = now - 3_600_000L * 9
                ),
                SocialPostEntity(
                    authorUsername = "prism_motion",
                    authorFullName = "Prism Motion",
                    authorAvatarColorHex = 0xFF9333EA,
                    caption = "Liquid glass shaders spinning in real time at 60fps. Tap save to drop a full-res frame straight into your Vault!",
                    hashtags = "#Reels #MotionArt #PrismWave #3DRender",
                    categoryTag = "Tech",
                    mediaKey = "drawable:img_reel_cosmic_prism",
                    isReel = true,
                    audioTrack = "Prism Pulse • Synthwave Original Mix",
                    likesCount = 3420,
                    isFollowingAuthor = true,
                    moodTag = "Vibrant",
                    createdAtMillis = now - 3_600_000L * 14
                ),
                SocialPostEntity(
                    authorUsername = currentUsername.ifBlank { "instaplus_creator" },
                    authorFullName = currentFullName.ifBlank { "InstaPlus Creator" },
                    authorAvatarColorHex = 0xFF0095F6,
                    caption = "Welcome to your independent InstaPlus space—zero third-party trackers, Time Capsule posts, feed polls, biometric chat locks, and AI translation built in.",
                    hashtags = "#InstaPlus #PrivacyFirst #CreativeStudio",
                    categoryTag = "Lifestyle",
                    mediaKey = "gradient:sunset",
                    likesCount = 128,
                    isFollowingAuthor = false,
                    moodTag = "Chill",
                    createdAtMillis = now - 3_600_000L * 20
                )
            )
            postDao.insertAll(initialPosts)
        }

        if (commentDao.count() == 0) {
            val allPosts = postDao.count()
            if (allPosts > 0) {
                commentDao.insertAll(
                    listOf(
                        PostCommentEntity(
                            postId = 1L,
                            authorUsername = "studio_nova",
                            text = "¡Qué reflections increíbles en el suelo mojado!",
                            createdAtMillis = now - 2_400_000L
                        ),
                        PostCommentEntity(
                            postId = 1L,
                            authorUsername = "elena_alpine",
                            text = "The magenta glow on those lanterns looks cinematic.",
                            createdAtMillis = now - 1_800_000L
                        ),
                        PostCommentEntity(
                            postId = 2L,
                            authorUsername = "kael_travels",
                            text = "Wunderschöner Sonnenaufgang über den Bergen!",
                            createdAtMillis = now - 9_000_000L
                        ),
                        PostCommentEntity(
                            postId = 4L,
                            authorUsername = "aria_designs",
                            text = "Saved to my Vault—that prism gradient is unreal.",
                            createdAtMillis = now - 12_000_000L
                        )
                    )
                )
            }
        }

        if (storyDao.count() == 0) {
            storyDao.insertAll(
                listOf(
                    StoryEntity(
                        authorUsername = "kael_travels",
                        authorFullName = "Kael Sato",
                        mediaKey = "drawable:img_post_neon_tokyo",
                        caption = "Midnight ramen stop in Shinjuku before the rain cleared.",
                        moodTag = "Wanderlust",
                        createdAtMillis = now - 1_800_000L,
                        expiresAtMillis = now + 79_200_000L
                    ),
                    StoryEntity(
                        authorUsername = "elena_alpine",
                        authorFullName = "Elena Vance",
                        mediaKey = "drawable:img_post_alpine_sunrise",
                        caption = "Elevation 2,840m • Zero signal, pure calm.",
                        moodTag = "Grateful",
                        createdAtMillis = now - 3_600_000L,
                        expiresAtMillis = now + 72_000_000L
                    ),
                    StoryEntity(
                        authorUsername = "studio_nova",
                        authorFullName = "Nova Craft",
                        mediaKey = "drawable:img_post_studio_desk",
                        caption = "Sketching a fresh monochrome icon set today.",
                        moodTag = "Focused",
                        createdAtMillis = now - 5_400_000L,
                        expiresAtMillis = now + 68_000_000L
                    ),
                    StoryEntity(
                        authorUsername = "prism_motion",
                        authorFullName = "Prism Motion",
                        mediaKey = "drawable:img_reel_cosmic_prism",
                        caption = "New 60fps prism shader drop tonight at 8 PM.",
                        moodTag = "Vibrant",
                        createdAtMillis = now - 7_200_000L,
                        expiresAtMillis = now + 64_000_000L
                    )
                )
            )
        }

        if (directChatDao.threadCount() == 0) {
            directChatDao.upsertThreads(
                listOf(
                    DirectThreadEntity(
                        threadKey = "@aria_designs",
                        displayName = "Aria Lin",
                        statusText = "Designing dark UI kits",
                        isOnline = true,
                        avatarColorHex = 0xFFDD2A7B,
                        lastMessagePreview = "Tap lock to shield our private design drafts!",
                        unreadCount = 2,
                        updatedAtMillis = now - 900_000L
                    ),
                    DirectThreadEntity(
                        threadKey = "@kael_travels",
                        displayName = "Kael Sato",
                        statusText = "Tokyo • Midnight Walk",
                        isOnline = true,
                        avatarColorHex = 0xFF0095F6,
                        lastMessagePreview = "Envíame la foto en alta resolución cuando puedas",
                        unreadCount = 1,
                        updatedAtMillis = now - 4_200_000L
                    ),
                    DirectThreadEntity(
                        threadKey = "@studio_nova",
                        displayName = "Nova Craft",
                        statusText = "In focus mode",
                        isOnline = false,
                        avatarColorHex = 0xFF9333EA,
                        lastMessagePreview = "Self-destructing timer is super handy for codes.",
                        unreadCount = 0,
                        updatedAtMillis = now - 14_000_000L
                    )
                )
            )

            directChatDao.insertMessages(
                listOf(
                    DirectMessageEntity(
                        threadKey = "@aria_designs",
                        senderUsername = "aria_designs",
                        isOutgoing = false,
                        text = "Hey! Did you test the new Biometric Chat Lock in InstaPlus?",
                        isSeenSent = false,
                        createdAtMillis = now - 1_500_000L
                    ),
                    DirectMessageEntity(
                        threadKey = "@aria_designs",
                        senderUsername = "aria_designs",
                        isOutgoing = false,
                        text = "Tap lock to shield our private design drafts!",
                        isSeenSent = false,
                        createdAtMillis = now - 900_000L
                    ),
                    DirectMessageEntity(
                        threadKey = "@kael_travels",
                        senderUsername = "kael_travels",
                        isOutgoing = false,
                        text = "Envíame la foto en alta resolución cuando puedas — you can use 1-Tap Translate right on this bubble!",
                        isSeenSent = false,
                        createdAtMillis = now - 4_200_000L
                    ),
                    DirectMessageEntity(
                        threadKey = "@studio_nova",
                        senderUsername = "studio_nova",
                        isOutgoing = false,
                        text = "Self-destructing timer is super handy for sharing temporary invites.",
                        isSeenSent = true,
                        createdAtMillis = now - 14_000_000L
                    )
                )
            )
        }
    }

    suspend fun purgeExpiredContent(nowMillis: Long = System.currentTimeMillis()) {
        postDao.deleteExpiredPosts(nowMillis)
        storyDao.deleteExpiredStories(nowMillis)
        directChatDao.deleteExpiredMessages(nowMillis)
    }

    suspend fun toggleLike(post: SocialPostEntity) {
        val nextLiked = !post.isLiked
        val nextCount = (post.likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
        postDao.updateLike(post.id, nextLiked, nextCount)
    }

    suspend fun toggleBookmark(post: SocialPostEntity) {
        postDao.updateBookmark(post.id, !post.isBookmarked)
    }

    suspend fun toggleFollow(authorUsername: String, currentlyFollowing: Boolean) {
        postDao.updateFollowForAuthor(authorUsername, !currentlyFollowing)
    }

    suspend fun votePoll(post: SocialPostEntity, choice: Int) {
        if (post.pollQuestion.isBlank() || choice !in listOf(1, 2)) return
        if (post.userPollVote == choice) return
        var votesA = post.pollVotesA
        var votesB = post.pollVotesB
        if (post.userPollVote == 1) votesA = (votesA - 1).coerceAtLeast(0)
        if (post.userPollVote == 2) votesB = (votesB - 1).coerceAtLeast(0)
        if (choice == 1) votesA += 1 else votesB += 1
        postDao.updatePollVote(post.id, votesA, votesB, choice)
    }

    suspend fun addComment(postId: Long, authorUsername: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        commentDao.insert(
            PostCommentEntity(
                postId = postId,
                authorUsername = authorUsername,
                text = clean,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun publishPost(
        authorUsername: String,
        authorFullName: String,
        caption: String,
        hashtags: String,
        categoryTag: String,
        mediaKey: String,
        isReel: Boolean,
        moodTag: String,
        pollQuestion: String,
        pollOptionA: String,
        pollOptionB: String,
        ttlHours: Int?
    ): Long {
        val now = System.currentTimeMillis()
        val expiresAt = ttlHours?.let { now + it * 3_600_000L }
        return postDao.insert(
            SocialPostEntity(
                authorUsername = authorUsername.ifBlank { "instaplus_creator" },
                authorFullName = authorFullName.ifBlank { authorUsername },
                authorAvatarColorHex = 0xFF0095F6,
                caption = caption.trim(),
                hashtags = hashtags.trim(),
                categoryTag = categoryTag,
                mediaKey = mediaKey,
                isReel = isReel,
                audioTrack = if (isReel) "$authorUsername • Original Sound" else "",
                moodTag = moodTag,
                pollQuestion = pollQuestion.trim(),
                pollOptionA = pollOptionA.trim(),
                pollOptionB = pollOptionB.trim(),
                expiresAtMillis = expiresAt,
                createdAtMillis = now
            )
        )
    }

    suspend fun publishStory(
        authorUsername: String,
        authorFullName: String,
        mediaKey: String,
        caption: String,
        moodTag: String
    ): Long {
        val now = System.currentTimeMillis()
        return storyDao.insert(
            StoryEntity(
                authorUsername = authorUsername.ifBlank { "instaplus_creator" },
                authorFullName = authorFullName.ifBlank { authorUsername },
                mediaKey = mediaKey,
                caption = caption.trim(),
                moodTag = moodTag,
                isViewed = false,
                createdAtMillis = now,
                expiresAtMillis = now + 86_400_000L
            )
        )
    }

    suspend fun viewStoryRespectingGhostMode(story: StoryEntity) {
        val prefs = ghostModeRepository.currentPreferencesSnapshot()
        if (prefs.hideStorySeen) {
            ghostModeRepository.recordInterceptedRequest(
                endpoint = "instaplus://stories/${story.authorUsername}/seen",
                category = GhostPrivacyCategory.STORY_SEEN,
                method = "LOCAL",
                transport = "NativeEngine",
                reason = "Anonymous Story Viewing blocked seen receipt for @${story.authorUsername}"
            )
        } else {
            storyDao.markViewed(story.id, true)
        }
    }

    suspend fun openThreadRespectingGhostMode(threadKey: String) {
        val prefs = ghostModeRepository.currentPreferencesSnapshot()
        directChatDao.activateBurnTimersForThread(threadKey)
        if (prefs.hideDmReadReceipts) {
            ghostModeRepository.recordInterceptedRequest(
                endpoint = "instaplus://direct/$threadKey/mark_seen",
                category = GhostPrivacyCategory.DM_READ_RECEIPT,
                method = "LOCAL",
                transport = "NativeEngine",
                reason = "Suppressed DM 'Seen' read receipt in thread $threadKey"
            )
        } else {
            directChatDao.clearUnread(threadKey)
        }
    }

    fun notifyTypingRespectingGhostMode(threadKey: String) {
        val prefs = ghostModeRepository.currentPreferencesSnapshot()
        if (prefs.hideTypingIndicator) {
            ghostModeRepository.recordInterceptedRequest(
                endpoint = "instaplus://direct/$threadKey/typing",
                category = GhostPrivacyCategory.TYPING_INDICATOR,
                method = "LOCAL",
                transport = "NativeEngine",
                reason = "Blocked outgoing 'Typing...' indicator in $threadKey"
            )
        }
    }

    suspend fun sendMessage(
        threadKey: String,
        senderUsername: String,
        text: String,
        burnAfterSeconds: Int
    ) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        val expiresAt = if (burnAfterSeconds > 0) now + burnAfterSeconds * 1000L else null
        directChatDao.insertMessage(
            DirectMessageEntity(
                threadKey = threadKey,
                senderUsername = senderUsername,
                isOutgoing = true,
                text = clean,
                isSeenSent = !ghostModeRepository.currentPreferencesSnapshot().hideDmReadReceipts,
                burnAfterSeconds = burnAfterSeconds,
                expiresAtMillis = expiresAt,
                createdAtMillis = now
            )
        )
        directChatDao.updateThreadPreview(
            threadKey = threadKey,
            preview = if (burnAfterSeconds > 0) "Secret (${burnAfterSeconds}s): $clean" else clean,
            updatedAt = now
        )
    }

    suspend fun createOrOpenThread(handle: String, displayName: String): String {
        val cleanHandle = handle.trim().removePrefix("@")
        val normalizedKey = "@$cleanHandle"
        val now = System.currentTimeMillis()
        directChatDao.upsertThread(
            DirectThreadEntity(
                threadKey = normalizedKey,
                displayName = displayName.trim().ifBlank { cleanHandle },
                statusText = "InstaPlus Private Chat",
                isOnline = true,
                avatarColorHex = 0xFF0095F6,
                lastMessagePreview = "Started a new encrypted conversation",
                unreadCount = 0,
                updatedAtMillis = now
            )
        )
        return normalizedKey
    }

    suspend fun deletePost(postId: Long) {
        postDao.deleteById(postId)
    }
}
