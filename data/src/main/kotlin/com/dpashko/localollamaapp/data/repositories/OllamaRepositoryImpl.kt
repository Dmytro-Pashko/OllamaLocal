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

class OllamaRepositoryImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) : OllamaRepository {
    override suspend fun checkConnection(config: OllamaConnectionConfig): AppResult<Unit> =
        safeNetworkCall {
            val response = httpClient.get("${config.baseUrl}/api/version")
            if (response.status.isSuccess()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(response.toAppError())
            }
        }

    override suspend fun getModels(config: OllamaConnectionConfig): AppResult<List<OllamaModel>> =
        safeNetworkCall {
            val response = httpClient.get("${config.baseUrl}/api/tags")
            if (response.status.isSuccess()) {
                val models = response.body<OllamaTagsResponseDto>()
                    .models
                    .map { it.toDomain() }
                    .sortedBy { it.name }
                AppResult.Success(models)
            } else {
                AppResult.Failure(response.toAppError())
            }
        }

    override suspend fun sendChatMessage(
        config: OllamaConnectionConfig,
        modelName: String,
        messages: List<Message>,
    ): AppResult<String> =
        safeNetworkCall {
            val response = httpClient.post("${config.baseUrl}/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(
                    OllamaChatRequestDto(
                        model = modelName,
                        messages = messages.map { it.toOllamaDto() },
                        stream = false,
                    ),
                )
            }

            if (response.status.isSuccess()) {
                val chatResponse = response.body<OllamaChatResponseDto>()
                AppResult.Success(chatResponse.message?.content.orEmpty())
            } else {
                AppResult.Failure(response.toAppError())
            }
        }

    private suspend fun <T> safeNetworkCall(block: suspend () -> AppResult<T>): AppResult<T> =
        try {
            block()
        } catch (exception: HttpRequestTimeoutException) {
            AppResult.Failure(AppError.Timeout)
        } catch (exception: SocketTimeoutException) {
            AppResult.Failure(AppError.Timeout)
        } catch (exception: ConnectException) {
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: UnknownHostException) {
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: NoRouteToHostException) {
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: IOException) {
            AppResult.Failure(AppError.NetworkUnavailable)
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    private suspend fun HttpResponse.toAppError(): AppError {
        val errorMessage = bodyAsText().decodeErrorMessage()
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
}
