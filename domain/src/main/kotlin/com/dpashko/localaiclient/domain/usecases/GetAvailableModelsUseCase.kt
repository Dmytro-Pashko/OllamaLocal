package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.models.ai.AiModel
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import javax.inject.Inject

/**
 * Loads selectable models from a connected local AI provider.
 */
class GetAvailableModelsUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
) {
    /**
     * Returns a non-empty list of provider models or an [AppError.EmptyModels] failure.
     */
    suspend operator fun invoke(config: ConnectionConfig): AppResult<List<AiModel>> =
        when (val result = aiProviderRepository.getModels(config)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                if (result.data.isEmpty()) {
                    AppResult.Failure(AppError.EmptyModels)
                } else {
                    result
                }
            }
        }
}
