package com.dpashko.localaiclient.domain.models.error

/**
 * Normalized failures that presentation can map to user-facing messages.
 */
sealed interface AppError {
    /** User attempted to send or save a blank chat message. */
    data object EmptyMessage : AppError

    /** Provider connection succeeded but no selectable models were returned. */
    data object EmptyModels : AppError

    /** Host, port, or provider values cannot form a valid local connection. */
    data object InvalidConnectionConfig : AppError

    /** Generation settings are outside the accepted range. */
    data object InvalidGenerationSettings : AppError

    /** Conversation title is blank or too long. */
    data object InvalidConversationTitle : AppError

    /** Local provider could not be reached over the network. */
    data object NetworkUnavailable : AppError

    /** Local generation or provider request exceeded the configured timeout. */
    data object Timeout : AppError

    /** HTTP failure returned by the local provider. */
    data class Http(
        /** Response status code. */
        val code: Int,
        /** Optional provider response message. */
        val message: String?,
    ) : AppError

    /** Provider returned a structured server-side error message. */
    data class Server(
        /** Error text supplied by the provider. */
        val message: String,
    ) : AppError

    /** Unexpected failure that does not fit a more specific app error. */
    data class Unknown(
        /** Optional diagnostic message. */
        val message: String?,
    ) : AppError
}
