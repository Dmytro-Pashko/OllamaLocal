package com.dpashko.localaiclient.domain.models.common

import com.dpashko.localaiclient.domain.models.error.AppError

/**
 * Domain-level operation result that keeps success values and app errors explicit.
 */
sealed interface AppResult<out T> {
    /** Successful operation result. */
    data class Success<T>(
        /** Value returned by the completed operation. */
        val data: T,
    ) : AppResult<T>

    /** Failed operation result. */
    data class Failure(
        /** Normalized domain error for presentation mapping. */
        val error: AppError,
    ) : AppResult<Nothing>
}
