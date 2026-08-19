package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.conversation.ConversationSettings
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.settings.GenerationSettings
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Validates and saves generation settings for one conversation.
 */
class SaveConversationSettingsUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
) {
    suspend operator fun invoke(settings: ConversationSettings): AppResult<Unit> {
        if (settings.modelName.isBlank()) {
            return AppResult.Failure(AppError.EmptyModels)
        }
        if (!GenerationSettings.isValidMinutes(settings.generationTimeoutMinutes)) {
            return AppResult.Failure(AppError.InvalidGenerationSettings)
        }
        if (settings.systemPrompt.length > ConversationSettings.MAX_SYSTEM_PROMPT_LENGTH) {
            return AppResult.Failure(AppError.InvalidGenerationSettings)
        }

        return conversationRepository.saveConversationSettings(
            settings.copy(
                modelName = settings.modelName.trim(),
                systemPrompt = settings.systemPrompt.trim(),
            ),
        )
    }
}
