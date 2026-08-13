package com.dpashko.localollamaapp.domain.usecases

import com.dpashko.localollamaapp.domain.models.common.AppResult
import com.dpashko.localollamaapp.domain.models.connection.OllamaConnectionConfig
import com.dpashko.localollamaapp.domain.models.error.AppError
import com.dpashko.localollamaapp.domain.models.ollama.OllamaModel
import com.dpashko.localollamaapp.domain.repositories.OllamaRepository
import javax.inject.Inject

class GetAvailableModelsUseCase @Inject constructor(
    private val ollamaRepository: OllamaRepository,
) {
    suspend operator fun invoke(config: OllamaConnectionConfig): AppResult<List<OllamaModel>> =
        when (val result = ollamaRepository.getModels(config)) {
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
