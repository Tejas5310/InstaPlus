package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DownloadedMediaEntity::class,
        AccountSessionEntity::class,
        LockedChatEntity::class,
        SocialPostEntity::class,
        PostCommentEntity::class,
        StoryEntity::class,
        DirectThreadEntity::class,
        DirectMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class InstaPlusDatabase : RoomDatabase() {
    abstract fun downloadedMediaDao(): DownloadedMediaDao
    abstract fun accountSessionDao(): AccountSessionDao
    abstract fun lockedChatDao(): LockedChatDao
    abstract fun socialPostDao(): SocialPostDao
    abstract fun postCommentDao(): PostCommentDao
    abstract fun storyDao(): StoryDao
    abstract fun directChatDao(): DirectChatDao

    companion object {
        @Volatile
        private var INSTANCE: InstaPlusDatabase? = null

        fun getInstance(context: Context): InstaPlusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InstaPlusDatabase::class.java,
                    "instaplus_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
