package com.dpashko.localollamaapp.data.di

import android.content.Context
import androidx.room.Room
import com.dpashko.localollamaapp.data.database.LocalLlmDatabase
import com.dpashko.localollamaapp.data.database.dao.ConversationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LocalLlmDatabase =
        Room.databaseBuilder(
            context = context,
            klass = LocalLlmDatabase::class.java,
            name = "local_ollama.db",
        )
            .addMigrations(LocalLlmDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideConversationDao(database: LocalLlmDatabase): ConversationDao =
        database.conversationDao()
}
