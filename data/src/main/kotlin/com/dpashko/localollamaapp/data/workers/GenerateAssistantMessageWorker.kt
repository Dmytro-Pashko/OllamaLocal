package com.dpashko.localollamaapp.data.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ConversationRepository
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class GenerateAssistantMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val conversationRepository: ConversationRepository,
    private val ollamaRepository: OllamaRepository,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val host = inputData.getString(KEY_HOST).orEmpty()
        val port = inputData.getInt(KEY_PORT, OllamaConnectionConfig.DEFAULT_PORT)
        val conversationId = inputData.getLong(KEY_CONVERSATION_ID, 0L)
        val assistantMessageId = inputData.getLong(KEY_ASSISTANT_MESSAGE_ID, 0L)
        val modelName = inputData.getString(KEY_MODEL_NAME).orEmpty()

        if (
            host.isBlank() ||
            conversationId == 0L ||
            assistantMessageId == 0L ||
            modelName.isBlank()
        ) {
            logger.warn("GenerateAssistantMessageWorker missing required input data")
            return Result.failure()
        }

        if (!assistantMessageExists(assistantMessageId)) {
            logger.info(
                "GenerateAssistantMessageWorker skipped deleted assistantMessageId={}",
                assistantMessageId,
            )
            return Result.success()
        }

        val contextMessages = when (
            val result = conversationRepository.getContextMessages(conversationId)
        ) {
            is AppResult.Failure -> {
                failAssistantMessage(assistantMessageId, result.error.toWorkerMessage())
                return Result.success()
            }

            is AppResult.Success -> result.data
        }

        val assistantContent = when (
            val result = ollamaRepository.sendChatMessage(
                config = OllamaConnectionConfig(host = host, port = port),
                modelName = modelName,
                messages = contextMessages,
            )
        ) {
            is AppResult.Failure -> {
                failAssistantMessage(assistantMessageId, result.error.toWorkerMessage())
                return Result.success()
            }

            is AppResult.Success -> result.data
        }

        if (!assistantMessageExists(assistantMessageId)) {
            logger.info(
                "GenerateAssistantMessageWorker completed after deletion assistantMessageId={}",
                assistantMessageId,
            )
            return Result.success()
        }

        return when (
            val result = conversationRepository.completeAssistantMessage(
                messageId = assistantMessageId,
                content = assistantContent,
            )
        ) {
            is AppResult.Failure -> {
                logger.warn(
                    "GenerateAssistantMessageWorker failed to persist response: {}",
                    result.error,
                )
                Result.success()
            }

            is AppResult.Success -> Result.success()
        }
    }

    private suspend fun assistantMessageExists(messageId: Long): Boolean =
        when (val result = conversationRepository.messageExists(messageId)) {
            is AppResult.Failure -> false
            is AppResult.Success -> result.data
        }

    private suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    ) {
        conversationRepository.failAssistantMessage(
            messageId = messageId,
            errorMessage = errorMessage,
        )
    }

    private fun AppError.toWorkerMessage(): String =
        when (this) {
            AppError.EmptyMessage -> "Message is empty."
            AppError.EmptyModels -> "No local models found."
            AppError.InvalidConnectionConfig -> "Invalid connection config."
            AppError.NetworkUnavailable -> "Ollama is not reachable."
            AppError.Timeout -> "Ollama request timed out."
            is AppError.Http -> "HTTP $code: ${message ?: "Request failed"}"
            is AppError.Server -> message
            is AppError.Unknown -> message ?: "Unknown error."
        }

    companion object {
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_CONVERSATION_ID = "conversation_id"
        const val KEY_ASSISTANT_MESSAGE_ID = "assistant_message_id"
        const val KEY_MODEL_NAME = "model_name"

        private val logger = LoggerFactory.getLogger(GenerateAssistantMessageWorker::class.java)
    }
}
