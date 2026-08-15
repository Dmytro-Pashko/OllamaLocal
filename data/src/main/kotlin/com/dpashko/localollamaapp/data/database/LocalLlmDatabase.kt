package com.dpashko.localollamaapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dpashko.localollamaapp.data.database.dao.ConversationDao
import com.dpashko.localollamaapp.data.models.local.ConversationEntity
import com.dpashko.localollamaapp.data.models.local.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class LocalLlmDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN status TEXT NOT NULL DEFAULT 'SENT'")
                db.execSQL("ALTER TABLE messages ADD COLUMN errorMessage TEXT")
            }
        }
    }
}
