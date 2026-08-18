package com.dpashko.localaiclient.data.di

import android.content.Context
import androidx.room.Room
import com.dpashko.localaiclient.data.database.LocalAiClientDatabase
import com.dpashko.localaiclient.data.database.dao.ConversationDao
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
    ): LocalAiClientDatabase =
        Room.databaseBuilder(
            context = context,
            klass = LocalAiClientDatabase::class.java,
            name = "local_ai_client.db",
        )
            .addMigrations(LocalAiClientDatabase.MIGRATION_1_2)
            .addMigrations(LocalAiClientDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideConversationDao(database: LocalAiClientDatabase): ConversationDao =
        database.conversationDao()
}
