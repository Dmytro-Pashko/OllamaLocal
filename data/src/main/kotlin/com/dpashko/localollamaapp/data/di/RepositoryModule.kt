package com.dpashko.localollamaapp.data.di

import com.dpashko.localollamaapp.data.repositories.ConversationRepositoryImpl
import com.dpashko.localollamaapp.data.repositories.OllamaRepositoryImpl
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
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
    abstract fun bindOllamaRepository(
        repository: OllamaRepositoryImpl,
    ): OllamaRepository
}
