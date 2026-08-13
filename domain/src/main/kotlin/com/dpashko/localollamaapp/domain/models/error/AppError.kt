package com.dpashko.localollamaapp.domain.models.error

sealed interface AppError {
    data object EmptyMessage : AppError
    data object EmptyModels : AppError
    data object InvalidConnectionConfig : AppError
    data object NetworkUnavailable : AppError
    data object Timeout : AppError
    data class Http(val code: Int, val message: String?) : AppError
    data class Server(val message: String) : AppError
    data class Unknown(val message: String?) : AppError
}
