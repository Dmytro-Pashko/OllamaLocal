package com.dpashko.localaiclient.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.dpashko.localaiclient.data.workers.GenerateAssistantMessageWorker
import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class WorkManagerChatGenerationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ChatGenerationScheduler {
    override suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
        generationTimeoutMillis: Long,
        systemPrompt: String,
        replaceExisting: Boolean,
    ): AppResult<Unit> =
        try {
            val request = OneTimeWorkRequestBuilder<GenerateAssistantMessageWorker>()
                .setInputData(
                    workDataOf(
                        GenerateAssistantMessageWorker.KEY_PROVIDER to config.provider.routeValue,
                        GenerateAssistantMessageWorker.KEY_HOST to config.host,
                        GenerateAssistantMessageWorker.KEY_PORT to config.port,
                        GenerateAssistantMessageWorker.KEY_CONVERSATION_ID to conversationId,
                        GenerateAssistantMessageWorker.KEY_ASSISTANT_MESSAGE_ID to assistantMessageId,
                        GenerateAssistantMessageWorker.KEY_MODEL_NAME to modelName,
                        GenerateAssistantMessageWorker.KEY_GENERATION_TIMEOUT_MILLIS to generationTimeoutMillis,
                        GenerateAssistantMessageWorker.KEY_SYSTEM_PROMPT to systemPrompt,
                    ),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "conversation-$conversationId-generation",
                if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    override suspend fun cancelGeneration(conversationId: Long): AppResult<Unit> =
        try {
            WorkManager.getInstance(context)
                .cancelUniqueWork("conversation-$conversationId-generation")
                .await()
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }

    override suspend fun cancelGenerations(conversationIds: List<Long>): AppResult<Unit> =
        try {
            val workManager = WorkManager.getInstance(context)
            conversationIds.forEach { conversationId ->
                workManager.cancelUniqueWork("conversation-$conversationId-generation").await()
            }
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }
}
