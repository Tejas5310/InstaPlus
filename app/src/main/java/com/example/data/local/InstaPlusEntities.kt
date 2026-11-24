package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_media")
data class DownloadedMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaType: String,
    val authorHandle: String,
    val captionSnippet: String,
    val sourceUrl: String,
    val galleryUri: String,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "account_sessions")
data class AccountSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val cookiesJson: String,
    val isActive: Boolean,
    val lastActiveAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "locked_chats")
data class LockedChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadKey: String,
    val displayName: String,
    val lockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorUsername: String,
    val authorFullName: String,
    val authorAvatarColorHex: Long = 0xFFDD2A7B,
    val caption: String,
    val hashtags: String = "",
    val categoryTag: String = "Lifestyle",
    val mediaKey: String,
    val isReel: Boolean = false,
    val audioTrack: String = "",
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isBookmarked: Boolean = false,
    val isFollowingAuthor: Boolean = false,
    val moodTag: String = "",
    val pollQuestion: String = "",
    val pollOptionA: String = "",
    val pollOptionB: String = "",
    val pollVotesA: Int = 0,
    val pollVotesB: Int = 0,
    val userPollVote: Int = 0, // 0 = none, 1 = Option A, 2 = Option B
    val expiresAtMillis: Long? = null,
    val scheduledLabel: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "post_comments")
data class PostCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val authorUsername: String,
    val text: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "social_stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorUsername: String,
    val authorFullName: String,
    val mediaKey: String,
    val caption: String,
    val moodTag: String = "",
    val isViewed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 86_400_000L
)

@Entity(tableName = "direct_threads")
data class DirectThreadEntity(
    @PrimaryKey val threadKey: String,
    val displayName: String,
    val statusText: String = "Active on InstaPlus",
    val isOnline: Boolean = true,
    val avatarColorHex: Long = 0xFF515BD4,
    val lastMessagePreview: String = "",
    val unreadCount: Int = 0,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "direct_messages")
data class DirectMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadKey: String,
    val senderUsername: String,
    val isOutgoing: Boolean,
    val text: String,
    val isSeenSent: Boolean = true,
    val burnAfterSeconds: Int = 0,
    val expiresAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
