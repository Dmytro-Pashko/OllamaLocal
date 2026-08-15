package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.ConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.models.ai.AiModel
import com.dpashko.localollamaapp.domain.repositories.AiProviderRepository
import javax.inject.Inject

class GetAvailableModelsUseCase @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
) {
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
