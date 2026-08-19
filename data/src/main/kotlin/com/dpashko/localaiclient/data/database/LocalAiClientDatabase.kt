package com.dpashko.localaiclient.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dpashko.localaiclient.data.database.dao.ConversationDao
import com.dpashko.localaiclient.data.models.local.ConversationBranchEntity
import com.dpashko.localaiclient.data.models.local.ConversationEntity
import com.dpashko.localaiclient.data.models.local.MessageEntity
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings

/**
 * Room database that stores local conversations and chat messages.
 */
@Database(
    entities = [
        ConversationEntity::class,
        ConversationBranchEntity::class,
        MessageEntity::class,
    ],
    version = 8,
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

        /**
         * Adds a flag that protects manually renamed conversations from auto-title updates.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN isTitleManuallyEdited INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /**
         * Adds archive metadata for hiding conversations without deleting local history.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversations ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN archivedAtMillis INTEGER")
            }
        }

        /**
         * Adds generation settings that belong to a single conversation.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE conversations ADD COLUMN generationTimeoutMillis INTEGER NOT NULL DEFAULT " +
                        GenerationSettings.DEFAULT_GENERATION_TIMEOUT_MILLIS,
                )
                db.execSQL("ALTER TABLE conversations ADD COLUMN systemPrompt TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * Adds branch metadata and assigns existing chats to one main branch.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversation_branches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conversationId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_branches_conversationId " +
                        "ON conversation_branches(conversationId)",
                )
                db.execSQL("ALTER TABLE conversations ADD COLUMN activeBranchId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN branchId INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    INSERT INTO conversation_branches(id, conversationId, title, createdAtMillis, updatedAtMillis)
                    SELECT id, id, 'Main', createdAtMillis, updatedAtMillis FROM conversations
                    """.trimIndent(),
                )
                db.execSQL("UPDATE conversations SET activeBranchId = id")
                db.execSQL("UPDATE messages SET branchId = conversationId")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_branchId ON messages(branchId)")
            }
        }

        /**
         * Adds branch-level summaries for local conversation compaction.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE conversation_branches ADD COLUMN summary TEXT")
                db.execSQL("ALTER TABLE conversation_branches ADD COLUMN summaryUntilMessageId INTEGER")
                db.execSQL("ALTER TABLE conversation_branches ADD COLUMN summaryUpdatedAtMillis INTEGER")
            }
        }
    }
}
