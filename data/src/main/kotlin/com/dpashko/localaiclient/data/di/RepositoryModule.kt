package com.dpashko.localaiclient.data.di

import com.dpashko.localaiclient.data.scheduler.WorkManagerChatGenerationScheduler
import com.dpashko.localaiclient.data.repositories.AiProviderRepositoryImpl
import com.dpashko.localaiclient.data.repositories.ConnectionPresetRepositoryImpl
import com.dpashko.localaiclient.data.repositories.ConversationRepositoryImpl
import com.dpashko.localaiclient.data.repositories.GenerationSettingsRepositoryImpl
import com.dpashko.localaiclient.data.repositories.LastConnectionRepositoryImpl
import com.dpashko.localaiclient.data.repositories.SecuritySettingsRepositoryImpl
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConnectionPresetRepository
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.GenerationSettingsRepository
import com.dpashko.localaiclient.domain.repositories.LastConnectionRepository
import com.dpashko.localaiclient.domain.repositories.SecuritySettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt binding contract from domain repository interfaces to data implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /**
     * Binds local Room-backed conversation storage to the domain repository contract.
     */
    @Binds
    @Singleton
    abstract fun bindConversationRepository(
        repository: ConversationRepositoryImpl,
    ): ConversationRepository

    /**
     * Binds local provider HTTP communication to the domain provider contract.
     */
    @Binds
    @Singleton
    abstract fun bindAiProviderRepository(
        repository: AiProviderRepositoryImpl,
    ): AiProviderRepository

    /**
     * Binds local connection preset storage to the domain preset contract.
     */
    @Binds
    @Singleton
    abstract fun bindConnectionPresetRepository(
        repository: ConnectionPresetRepositoryImpl,
    ): ConnectionPresetRepository

    /**
     * Binds last successful connection storage to the domain connection contract.
     */
    @Binds
    @Singleton
    abstract fun bindLastConnectionRepository(
        repository: LastConnectionRepositoryImpl,
    ): LastConnectionRepository

    /**
     * Binds generation settings persistence to the domain settings contract.
     */
    @Binds
    @Singleton
    abstract fun bindGenerationSettingsRepository(
        repository: GenerationSettingsRepositoryImpl,
    ): GenerationSettingsRepository

    /**
     * Binds security settings persistence to the domain settings contract.
     */
    @Binds
    @Singleton
    abstract fun bindSecuritySettingsRepository(
        repository: SecuritySettingsRepositoryImpl,
    ): SecuritySettingsRepository

    /**
     * Binds WorkManager-backed scheduling to the domain generation scheduler contract.
     */
    @Binds
    @Singleton
    abstract fun bindChatGenerationScheduler(
        scheduler: WorkManagerChatGenerationScheduler,
    ): ChatGenerationScheduler
}
