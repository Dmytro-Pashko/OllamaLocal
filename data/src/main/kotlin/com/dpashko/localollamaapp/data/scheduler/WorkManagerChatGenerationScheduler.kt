package com.dpashko.localollamaapp.data.scheduler

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.dpashko.localollamaapp.data.workers.GenerateAssistantMessageWorker
import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.repositories.ChatGenerationScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkManagerChatGenerationScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ChatGenerationScheduler {
    override suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
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
                    ),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "conversation-$conversationId-generation",
                if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
            AppResult.Success(Unit)
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Unknown(exception.message))
        }
}
