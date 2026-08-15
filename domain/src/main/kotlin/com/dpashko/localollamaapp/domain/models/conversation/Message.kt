package com.dpashko.localollamaapp.domain.models.conversation

data class Message(
    val id: Long,
    val conversationId: Long,
    val role: MessageRole,
    val content: String,
    val status: MessageStatus,
    val errorMessage: String?,
    val createdAtMillis: Long,
)
