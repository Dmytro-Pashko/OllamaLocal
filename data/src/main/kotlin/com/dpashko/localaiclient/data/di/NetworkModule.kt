package com.dpashko.localaiclient.data.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import org.slf4j.LoggerFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private val ktorLogger = LoggerFactory.getLogger("AiProviderKtorClient")

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient =
        HttpClient(Android) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 3_600_000
                socketTimeoutMillis = 3_600_000
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        ktorLogger.trace(message)
                    }
                }
                level = LogLevel.ALL
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
}
