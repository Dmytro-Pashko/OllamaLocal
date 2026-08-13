package com.dpashko.localollamaapp.domain.models.common

import com.dpashko.localollamaapp.domain.models.error.AppError

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
