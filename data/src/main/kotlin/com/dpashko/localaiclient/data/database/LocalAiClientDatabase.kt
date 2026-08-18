package com.dpashko.localaiclient.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dpashko.localaiclient.data.database.dao.ConversationDao
import com.dpashko.localaiclient.data.models.local.ConversationEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity

/**
 * Room database that stores local conversations and chat messages.
 */
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LocalAiClientDatabase : RoomDatabase() {
    /**
     * Provides conversation and message DAO operations.
     */
    abstract fun conversationDao(): ConversationDao

    companion object {
        /**
         * Adds assistant message lifecycle fields introduced for persisted background generation.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'")
                db.execSQL("ALTER TABLE messages ADD COLUMN errorMessage TEXT")
            }
        }

        /**
         * Adds a local pin flag for favorite conversations.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
