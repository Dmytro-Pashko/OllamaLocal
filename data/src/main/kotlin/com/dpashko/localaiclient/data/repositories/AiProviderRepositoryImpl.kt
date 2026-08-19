package com.dpashko.localaiclient.data.repositories

import com.dpashko.localaiclient.data.mappers.toDomain
import com.dpashko.localaiclient.data.mappers.toRemoteDto
import com.dpashko.localaiclient.data.models.remote.LmStudioChatRequestDto
import com.dpashko.localaiclient.data.models.remote.LmStudioChatResponseDto
import com.dpashko.localaiclient.data.models.remote.LmStudioChatStreamResponseDto
import com.dpashko.localaiclient.data.models.remote.LmStudioModelsResponseDto
import com.dpashko.localaiclient.data.models.remote.OllamaChatRequestDto
import com.dpashko.localaiclient.data.models.remote.OllamaChatResponseDto
import com.dpashko.localaiclient.data.models.remote.OllamaErrorDto
import com.dpashko.localaiclient.data.models.remote.OllamaTagsResponseDto
import com.dpashko.localaiclient.data.models.remote.OpenAiErrorDto
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.ai.AiModel
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import org.slf4j.LoggerFactory

class AiProviderRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : AiProviderRepository {
    override suspend fun checkConnection(config: ConnectionConfig): AppResult<Unit> =
        safeNetworkCall(operation = "checkConnection") {
            val url = when (config.provider) {
                AiProvider.OLLAMA -> "${config.baseUrl}/api/version"
                AiProvider.LM_STUDIO -> "${config.baseUrl}/v1/models"
            }
            logger.info("{} checkConnection request url={}", config.provider.displayName, url)
            val response = httpClient.get(url)
            logger.info("{} checkConnection response status={}", config.provider.displayName, response.status)
            if (response.status.isSuccess()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(response.toAppError(config.provider, operation = "checkConnection"))
            }
        }

    override suspend fun getModels(config: ConnectionConfig): AppResult<List<AiModel>> =
        safeNetworkCall(operation = "getModels") {
            val url = when (config.provider) {
                AiProvider.OLLAMA -> "${config.baseUrl}/api/tags"
                AiProvider.LM_STUDIO -> "${config.baseUrl}/v1/models"
            }
            logger.info("{} getModels request url={}", config.provider.displayName, url)
            val response = httpClient.get(url)
            logger.info("{} getModels response status={}", config.provider.displayName, response.status)
            if (response.status.isSuccess()) {
                val models = when (config.provider) {
                    AiProvider.OLLAMA -> response.body<OllamaTagsResponseDto>()
                        .models
                        .map { it.toDomain() }

                    AiProvider.LM_STUDIO -> response.body<LmStudioModelsResponseDto>()
                        .data
                        .map { it.toDomain() }
                }.sortedBy { it.name }
                logger.info(
                    "{} getModels parsed modelCount={}",
                    config.provider.displayName,
                    models.size,
                )
                AppResult.Success(models)
            } else {
                AppResult.Failure(response.toAppError(config.provider, operation = "getModels"))
            }
        }

    override suspend fun sendChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
        generationTimeoutMillis: Long,
    ): AppResult<String> =
        safeNetworkCall(operation = "sendChatMessage") {
            val url = when (config.provider) {
                AiProvider.OLLAMA -> "${config.baseUrl}/api/chat"
                AiProvider.LM_STUDIO -> "${config.baseUrl}/v1/chat/completions"
            }
            logger.info(
                "{} sendChatMessage request url={} model={} messageCount={}",
                config.provider.displayName,
                url,
                modelName,
                messages.size,
            )
            val response = httpClient.post(url) {
                timeout {
                    requestTimeoutMillis = generationTimeoutMillis
                    socketTimeoutMillis = generationTimeoutMillis
                }
                contentType(ContentType.Application.Json)
                when (config.provider) {
                    AiProvider.OLLAMA -> setBody(
                        OllamaChatRequestDto(
                            model = modelName,
                            messages = messages.map { it.toRemoteDto() },
                            stream = false,
                        ),
                    )

                    AiProvider.LM_STUDIO -> setBody(
                        LmStudioChatRequestDto(
                            model = modelName,
                            messages = messages.map { it.toRemoteDto() },
                            stream = false,
                        ),
                    )
                }
            }

            logger.info("{} sendChatMessage response status={}", config.provider.displayName, response.status)
            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                val assistantContent = when (config.provider) {
                    AiProvider.OLLAMA -> rawBody.decodeOllamaChatContent()
                    AiProvider.LM_STUDIO -> json.decodeFromString<LmStudioChatResponseDto>(rawBody)
                        .choices
                        .firstOrNull()
                        ?.message
                        ?.content
                        .orEmpty()
                }
                logger.info(
                    "{} sendChatMessage parsed responseChars={} rawBodyChars={}",
                    config.provider.displayName,
                    assistantContent.length,
                    rawBody.length,
                )
                AppResult.Success(assistantContent)
            } else {
                AppResult.Failure(response.toAppError(config.provider, operation = "sendChatMessage"))
            }
        }

    override suspend fun streamChatMessage(
        config: ConnectionConfig,
        modelName: String,
        messages: List<Message>,
        generationTimeoutMillis: Long,
        onDelta: suspend (String) -> Unit,
    ): AppResult<String> =
        safeNetworkCall(operation = "streamChatMessage") {
            val url = when (config.provider) {
                AiProvider.OLLAMA -> "${config.baseUrl}/api/chat"
                AiProvider.LM_STUDIO -> "${config.baseUrl}/v1/chat/completions"
            }
            logger.info(
                "{} streamChatMessage request url={} model={} messageCount={}",
                config.provider.displayName,
                url,
                modelName,
                messages.size,
            )
            val response = httpClient.post(url) {
                timeout {
                    requestTimeoutMillis = generationTimeoutMillis
                    socketTimeoutMillis = generationTimeoutMillis
                }
                contentType(ContentType.Application.Json)
                when (config.provider) {
                    AiProvider.OLLAMA -> setBody(
                        OllamaChatRequestDto(
                            model = modelName,
                            messages = messages.map { it.toRemoteDto() },
                            stream = true,
                        ),
                    )

                    AiProvider.LM_STUDIO -> setBody(
                        LmStudioChatRequestDto(
                            model = modelName,
                            messages = messages.map { it.toRemoteDto() },
                            stream = true,
                        ),
                    )
                }
            }

            logger.info("{} streamChatMessage response status={}", config.provider.displayName, response.status)
            if (!response.status.isSuccess()) {
                return@safeNetworkCall AppResult.Failure(
                    response.toAppError(config.provider, operation = "streamChatMessage"),
                )
            }

            val assistantContent = StringBuilder()
            var chunkCount = 0
            val channel = response.bodyAsChannel()
            while (true) {
                val line = channel.readUTF8Line() ?: break
                val delta = when (config.provider) {
                    AiProvider.OLLAMA -> line.decodeOllamaStreamDelta()
                    AiProvider.LM_STUDIO -> line.decodeLmStudioStreamDelta()
                }
                if (delta != null) {
                    chunkCount += 1
                    assistantContent.append(delta)
                    onDelta(delta)
                }
            }

            logger.info(
                "{} streamChatMessage parsed chunkCount={} responseChars={}",
                config.provider.displayName,
                chunkCount,
                assistantContent.length,
            )
            AppResult.Success(assistantContent.toString())
        }

    private suspend fun <T> safeNetworkCall(
        operation: String,
        block: suspend () -> AppResult<T>,
    ): AppResult<T> =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpRequestTimeoutException) {
            logger.warn("Provider {} timed out", operation, exception)
            AppResult.Failure(AppError.Timeout)
        } catch (exception: SocketTimeoutException) {
            logger.warn("Provider {} socket timed out", operation, exception)
            AppResult.Failure(AppError.Timeout)
        } catch (exception: ConnectException) {
            logger.warn("Provider {} could not connect", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: UnknownHostException) {
            logger.warn("Provider {} unknown host", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: NoRouteToHostException) {
            logger.warn("Provider {} no route to host", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: IOException) {
            logger.warn("Provider {} IO failure", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: Exception) {
            logger.error("Provider {} unexpected failure", operation, exception)
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private suspend fun HttpResponse.toAppError(
        provider: AiProvider,
        operation: String,
    ): AppError {
        val rawBody = bodyAsText()
        val errorMessage = rawBody.decodeErrorMessage()
        logger.warn(
            "{} {} failed status={} bodyChars={}",
            provider.displayName,
            operation,
            status,
            rawBody.length,
        )
        return when (status) {
            HttpStatusCode.RequestTimeout -> AppError.Timeout
            else -> {
                if (errorMessage != null) {
                    AppError.Server(errorMessage)
                } else {
                    AppError.Http(status.value, status.description)
                }
            }
        }
    }

    private fun String.decodeErrorMessage(): String? =
        try {
            json.decodeFromString<OllamaErrorDto>(this).error
        } catch (exception: Exception) {
            try {
                json.decodeFromString<OpenAiErrorDto>(this).error?.message
            } catch (exception: Exception) {
                null
            }
        }

    private fun String.decodeOllamaChatContent(): String {
        val chunks = lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        if (chunks.isEmpty()) {
            return ""
        }

        return chunks.joinToString(separator = "") { chunk ->
            json.decodeFromString<OllamaChatResponseDto>(chunk).message?.content.orEmpty()
        }
    }

    private fun String.decodeOllamaStreamDelta(): String? {
        val line = trim()
        if (line.isEmpty()) {
            return null
        }

        return try {
            json.decodeFromString<OllamaChatResponseDto>(line).message?.content?.takeIf { it.isNotEmpty() }
        } catch (exception: Exception) {
            throw StreamDecodeException()
        }
    }

    private fun String.decodeLmStudioStreamDelta(): String? {
        val line = trim()
        if (line.isEmpty() || !line.startsWith(SSE_DATA_PREFIX)) {
            return null
        }

        val eventData = line.removePrefix(SSE_DATA_PREFIX).trim()
        if (eventData.isEmpty() || eventData == SSE_DONE_EVENT) {
            return null
        }

        return try {
            json.decodeFromString<LmStudioChatStreamResponseDto>(eventData)
                .choices
                .firstOrNull()
                ?.delta
                ?.content
                ?.takeIf { it.isNotEmpty() }
        } catch (exception: Exception) {
            throw StreamDecodeException()
        }
    }

    private class StreamDecodeException : Exception("Invalid provider stream response.")

    private companion object {
        const val SSE_DATA_PREFIX = "data:"
        const val SSE_DONE_EVENT = "[DONE]"
        val logger = LoggerFactory.getLogger(AiProviderRepositoryImpl::class.java)
    }
}
