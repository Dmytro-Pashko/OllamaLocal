package com.dpashko.localollamaapp.data.repositories

import com.dpashko.localollamaapp.data.mappers.toDomain
import com.dpashko.localollamaapp.data.mappers.toOllamaDto
import com.dpashko.localollamaapp.data.models.remote.OllamaChatRequestDto
import com.dpashko.localollamaapp.data.models.remote.OllamaChatResponseDto
import com.dpashko.localollamaapp.data.models.remote.OllamaErrorDto
import com.dpashko.localollamaapp.data.models.remote.OllamaTagsResponseDto
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.conversation.Message
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.models.ollama.OllamaModel
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import org.slf4j.LoggerFactory

class OllamaRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : OllamaRepository {
    override suspend fun checkConnection(config: OllamaConnectionConfig): AppResult<Unit> =
        safeNetworkCall(operation = "checkConnection") {
            val url = "${config.baseUrl}/api/version"
            logger.info("Ollama checkConnection request url={}", url)
            val response = httpClient.get(url)
            logger.info("Ollama checkConnection response status={}", response.status)
            if (response.status.isSuccess()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(response.toAppError(operation = "checkConnection"))
            }
        }

    override suspend fun getModels(config: OllamaConnectionConfig): AppResult<List<OllamaModel>> =
        safeNetworkCall(operation = "getModels") {
            val url = "${config.baseUrl}/api/tags"
            logger.info("Ollama getModels request url={}", url)
            val response = httpClient.get(url)
            logger.info("Ollama getModels response status={}", response.status)
            if (response.status.isSuccess()) {
                val models = response.body<OllamaTagsResponseDto>()
                    .models
                    .map { it.toDomain() }
                    .sortedBy { it.name }
                logger.info(
                    "Ollama getModels parsed modelCount={} models={}",
                    models.size,
                    models.joinToString { it.name },
                )
                AppResult.Success(models)
            } else {
                AppResult.Failure(response.toAppError(operation = "getModels"))
            }
        }

    override suspend fun sendChatMessage(
        config: OllamaConnectionConfig,
        modelName: String,
        messages: List<Message>,
    ): AppResult<String> =
        safeNetworkCall(operation = "sendChatMessage") {
            val url = "${config.baseUrl}/api/chat"
            logger.info(
                "Ollama sendChatMessage request url={} model={} messageCount={}",
                url,
                modelName,
                messages.size,
            )
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequestDto(
                        model = modelName,
                        messages = messages.map { it.toOllamaDto() },
                        stream = false,
                    ),
                )
            }

            logger.info("Ollama sendChatMessage response status={}", response.status)
            if (response.status.isSuccess()) {
                val rawBody = response.bodyAsText()
                val assistantContent = rawBody.decodeChatContent()
                logger.info(
                    "Ollama sendChatMessage parsed responseChars={} rawBodyChars={}",
                    assistantContent.length,
                    rawBody.length,
                )
                AppResult.Success(assistantContent)
            } else {
                AppResult.Failure(response.toAppError(operation = "sendChatMessage"))
            }
        }

    private suspend fun <T> safeNetworkCall(
        operation: String,
        block: suspend () -> AppResult<T>,
    ): AppResult<T> =
        try {
            block()
        } catch (exception: HttpRequestTimeoutException) {
            logger.warn("Ollama {} timed out", operation, exception)
            AppResult.Failure(AppError.Timeout)
        } catch (exception: SocketTimeoutException) {
            logger.warn("Ollama {} socket timed out", operation, exception)
            AppResult.Failure(AppError.Timeout)
        } catch (exception: ConnectException) {
            logger.warn("Ollama {} could not connect", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: UnknownHostException) {
            logger.warn("Ollama {} unknown host", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: NoRouteToHostException) {
            logger.warn("Ollama {} no route to host", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: IOException) {
            logger.warn("Ollama {} IO failure", operation, exception)
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: Exception) {
            logger.error("Ollama {} unexpected failure", operation, exception)
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private suspend fun HttpResponse.toAppError(operation: String): AppError {
        val rawBody = bodyAsText()
        val errorMessage = rawBody.decodeErrorMessage()
        logger.warn(
            "Ollama {} failed status={} rawBody={}",
            operation,
            status,
            rawBody.take(MAX_LOG_BODY_LENGTH),
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
            null
        }

    private fun String.decodeChatContent(): String {
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

    private companion object {
        const val MAX_LOG_BODY_LENGTH = 4_000
        val logger = LoggerFactory.getLogger(OllamaRepositoryImpl::class.java)
    }
}
