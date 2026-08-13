package com.dpashko.localollamaapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dpashko.localollamaapp.data.database.dao.ConversationDao
import com.dpashko.localollamaapp.data.models.local.ConversationEntity
import com.dpashko.localollamaapp.data.models.local.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LocalOllamaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
