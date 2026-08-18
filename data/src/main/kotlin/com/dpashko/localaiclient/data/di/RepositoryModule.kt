package com.dpashko.localaiclient.data.di

import com.dpashko.localaiclient.data.scheduler.WorkManagerChatGenerationScheduler
import com.dpashko.localaiclient.data.repositories.AiProviderRepositoryImpl
import com.dpashko.localaiclient.data.repositories.ConversationRepositoryImpl
import com.dpashko.localaiclient.data.repositories.GenerationSettingsRepositoryImpl
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        repository: ConversationRepositoryImpl,
    ): ConversationRepository

    @Binds
    @Singleton
    abstract fun bindAiProviderRepository(
        repository: AiProviderRepositoryImpl,
    ): AiProviderRepository

    @Binds
    @Singleton
    abstract fun bindGenerationSettingsRepository(
        repository: GenerationSettingsRepositoryImpl,
    ): GenerationSettingsRepository

    @Binds
    @Singleton
    abstract fun bindChatGenerationScheduler(
        scheduler: WorkManagerChatGenerationScheduler,
    ): ChatGenerationScheduler
}
