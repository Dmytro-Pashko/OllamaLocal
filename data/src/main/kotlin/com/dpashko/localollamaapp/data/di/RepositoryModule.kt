package com.dpashko.localollamaapp.data.di

import com.dpashko.localollamaapp.data.scheduler.WorkManagerChatGenerationScheduler
import com.dpashko.localollamaapp.data.repositories.AiProviderRepositoryImpl
import com.dpashko.localollamaapp.data.repositories.ConversationRepositoryImpl
import com.dpashko.localollamaapp.domain.repositories.ChatGenerationScheduler
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import com.dpashko.localollamaapp.domain.repositories.AiProviderRepository
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
    abstract fun bindChatGenerationScheduler(
        scheduler: WorkManagerChatGenerationScheduler,
    ): ChatGenerationScheduler
}
