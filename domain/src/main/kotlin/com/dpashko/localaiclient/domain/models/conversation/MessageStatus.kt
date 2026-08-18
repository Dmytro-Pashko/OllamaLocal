package com.dpashko.localaiclient.domain.models.conversation

/**
 * Persistence state for a message's generation lifecycle.
 */
enum class MessageStatus {
    /** Message is complete and can be used as future conversation context. */
    SENT,
    /** Assistant placeholder is waiting for or receiving local model output. */
    GENERATING,
    /** Generation failed and may be retried by the user. */
    FAILED,
    /** Generation was explicitly stopped before completion. */
    CANCELED,
}
