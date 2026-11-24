package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedMediaDao {
    @Query("SELECT * FROM downloaded_media ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<DownloadedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadedMediaEntity): Long

    @Query("DELETE FROM downloaded_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloaded_media")
    suspend fun clearAll()
}

@Dao
interface AccountSessionDao {
    @Query("SELECT * FROM account_sessions ORDER BY isActive DESC, lastActiveAt DESC")
    fun observeAll(): Flow<List<AccountSessionEntity>>

    @Query("SELECT * FROM account_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): AccountSessionEntity?

    @Query("SELECT * FROM account_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AccountSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AccountSessionEntity): Long

    @Query("UPDATE account_sessions SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE account_sessions SET isActive = 1, lastActiveAt = :timestamp WHERE id = :id")
    suspend fun markActive(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE account_sessions SET cookiesJson = :cookiesJson, lastActiveAt = :timestamp WHERE id = :id")
    suspend fun updateCookies(id: Long, cookiesJson: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM account_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface LockedChatDao {
    @Query("SELECT * FROM locked_chats ORDER BY lockedAt DESC")
    fun observeAll(): Flow<List<LockedChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LockedChatEntity): Long

    @Query("DELETE FROM locked_chats WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SocialPostEntity>>

    @Query("SELECT COUNT(*) FROM social_posts")
    suspend fun count(): Int

    @Query("SELECT * FROM social_posts WHERE id = :postId LIMIT 1")
    suspend fun getById(postId: Long): SocialPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SocialPostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SocialPostEntity>)

    @Query("UPDATE social_posts SET isLiked = :isLiked, likesCount = :likesCount WHERE id = :postId")
    suspend fun updateLike(postId: Long, isLiked: Boolean, likesCount: Int)

    @Query("UPDATE social_posts SET isBookmarked = :bookmarked WHERE id = :postId")
    suspend fun updateBookmark(postId: Long, bookmarked: Boolean)

    @Query("UPDATE social_posts SET isFollowingAuthor = :following WHERE authorUsername = :authorUsername")
    suspend fun updateFollowForAuthor(authorUsername: String, following: Boolean)

    @Query("UPDATE social_posts SET pollVotesA = :votesA, pollVotesB = :votesB, userPollVote = :choice WHERE id = :postId")
    suspend fun updatePollVote(postId: Long, votesA: Int, votesB: Int, choice: Int)

    @Query("DELETE FROM social_posts WHERE expiresAtMillis IS NOT NULL AND expiresAtMillis <= :nowMillis")
    suspend fun deleteExpiredPosts(nowMillis: Long)

    @Query("DELETE FROM social_posts WHERE id = :postId")
    suspend fun deleteById(postId: Long)
}

@Dao
interface PostCommentDao {
    @Query("SELECT * FROM post_comments ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<PostCommentEntity>>

    @Query("SELECT COUNT(*) FROM post_comments")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PostCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PostCommentEntity>)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM social_stories ORDER BY isViewed ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<StoryEntity>>

    @Query("SELECT COUNT(*) FROM social_stories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<StoryEntity>)

    @Query("UPDATE social_stories SET isViewed = :viewed WHERE id = :storyId")
    suspend fun markViewed(storyId: Long, viewed: Boolean)

    @Query("DELETE FROM social_stories WHERE expiresAtMillis <= :nowMillis")
    suspend fun deleteExpiredStories(nowMillis: Long)
}

@Dao
interface DirectChatDao {
    @Query("SELECT * FROM direct_threads ORDER BY updatedAtMillis DESC")
    fun observeThreads(): Flow<List<DirectThreadEntity>>

    @Query("SELECT * FROM direct_messages ORDER BY createdAtMillis ASC")
    fun observeAllMessages(): Flow<List<DirectMessageEntity>>

    @Query("SELECT COUNT(*) FROM direct_threads")
    suspend fun threadCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThread(thread: DirectThreadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertThreads(threads: List<DirectThreadEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DirectMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<DirectMessageEntity>)

    @Query("UPDATE direct_threads SET unreadCount = 0 WHERE threadKey = :threadKey")
    suspend fun clearUnread(threadKey: String)

    @Query("UPDATE direct_threads SET lastMessagePreview = :preview, updatedAtMillis = :updatedAt WHERE threadKey = :threadKey")
    suspend fun updateThreadPreview(threadKey: String, preview: String, updatedAt: Long)

    @Query("UPDATE direct_messages SET expiresAtMillis = createdAtMillis + (burnAfterSeconds * 1000) WHERE threadKey = :threadKey AND burnAfterSeconds > 0 AND expiresAtMillis IS NULL")
    suspend fun activateBurnTimersForThread(threadKey: String)

    @Query("DELETE FROM direct_messages WHERE expiresAtMillis IS NOT NULL AND expiresAtMillis <= :nowMillis")
    suspend fun deleteExpiredMessages(nowMillis: Long)
}
