package com.dpashko.localaiclient.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.AiProvider
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.LoggerFactory

@HiltWorker
class GenerateAssistantMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val conversationRepository: ConversationRepository,
    private val aiProviderRepository: AiProviderRepository,
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val provider = AiProvider.fromRouteValue(inputData.getString(KEY_PROVIDER))
        val host = inputData.getString(KEY_HOST).orEmpty()
        val port = inputData.getInt(KEY_PORT, provider.defaultPort)
        val conversationId = inputData.getLong(KEY_CONVERSATION_ID, 0L)
        val assistantMessageId = inputData.getLong(KEY_ASSISTANT_MESSAGE_ID, 0L)
        val modelName = inputData.getString(KEY_MODEL_NAME).orEmpty()
        val generationTimeoutMillis = inputData.getLong(
            KEY_GENERATION_TIMEOUT_MILLIS,
            GenerationSettings.DEFAULT_GENERATION_TIMEOUT_MILLIS,
        )

        if (
            host.isBlank() ||
            conversationId == 0L ||
            assistantMessageId == 0L ||
            modelName.isBlank()
        ) {
            logger.warn("GenerateAssistantMessageWorker missing required input data")
            return Result.failure()
        }

        setForeground(createForegroundInfo(provider, modelName))

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
            val result = aiProviderRepository.sendChatMessage(
                config = ConnectionConfig(
                    provider = provider,
                    host = host,
                    port = port,
                ),
                modelName = modelName,
                messages = contextMessages,
                generationTimeoutMillis = generationTimeoutMillis,
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

    private fun createForegroundInfo(
        provider: AiProvider,
        modelName: String,
    ): ForegroundInfo {
        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Generating response")
            .setContentText("${provider.displayName} • $modelName")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Local AI Client generation",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows active local model response generation."
            },
        )
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
            AppError.InvalidGenerationSettings -> "Invalid generation settings."
            AppError.InvalidConversationTitle -> "Invalid conversation title."
            AppError.NetworkUnavailable -> "Provider is not reachable."
            AppError.Timeout -> "Provider request timed out."
            is AppError.Http -> "HTTP $code: ${message ?: "Request failed"}"
            is AppError.Server -> message
            is AppError.Unknown -> message ?: "Unknown error."
        }

    companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_CONVERSATION_ID = "conversation_id"
        const val KEY_ASSISTANT_MESSAGE_ID = "assistant_message_id"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_GENERATION_TIMEOUT_MILLIS = "generation_timeout_millis"
        private const val CHANNEL_ID = "local_ai_client_generation"
        private const val NOTIFICATION_ID = 1001

        private val logger = LoggerFactory.getLogger(GenerateAssistantMessageWorker::class.java)
    }
}
