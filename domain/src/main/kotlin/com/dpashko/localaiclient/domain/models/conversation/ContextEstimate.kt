package com.dpashko.localaiclient.domain.models.conversation

import kotlin.math.ceil

/**
 * Approximate local context size for one conversation.
 */
data class ContextEstimate(
    val messageCount: Int,
    val characterCount: Int,
    val estimatedTokens: Int,
    val includedSystemPrompt: Boolean,
    val updatedAtMillis: Long,
) {
    companion object {
        fun from(
            messages: List<Message>,
            systemPrompt: String,
        ): ContextEstimate {
            val promptCharacters = systemPrompt.length
            val messageCharacters = messages.sumOf { it.content.length }
            val totalCharacters = promptCharacters + messageCharacters
            return ContextEstimate(
                messageCount = messages.size,
                characterCount = totalCharacters,
                estimatedTokens = ceil(totalCharacters / CHARS_PER_TOKEN).toInt(),
                includedSystemPrompt = systemPrompt.isNotBlank(),
                updatedAtMillis = System.currentTimeMillis(),
            )
        }

        private const val CHARS_PER_TOKEN = 4.0
    }
}
