package com.dpashko.localaiclient.presentation.common

import com.dpashko.localaiclient.domain.models.error.AppError

fun AppError.toUserMessage(): String =
    when (this) {
        AppError.EmptyMessage -> "Message is empty."
        AppError.EmptyModels -> "No local models found on this provider."
        AppError.InvalidConnectionConfig -> "Check the IP address and port."
        AppError.InvalidGenerationSettings -> "Enter a timeout from 1 to 1440 minutes."
        AppError.InvalidConversationTitle -> "Enter a title from 1 to 48 characters."
        AppError.NetworkUnavailable -> "Cannot reach this provider on this address."
        AppError.Timeout -> "The request timed out."
        is AppError.Http -> "HTTP $code: ${message ?: "Request failed"}"
        is AppError.Server -> message
        is AppError.Unknown -> message ?: "Something went wrong."
    }
