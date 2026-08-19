package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.conversation.MessageRole
import com.dpashko.localaiclient.domain.models.conversation.MessageStatus
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.AiProviderRepository
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import javax.inject.Inject

/**
 * Creates a local summary of older active-branch messages for future context.
 */
class CompactConversationUseCase @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val aiProviderRepository: AiProviderRepository,
) {
    suspend operator fun invoke(
        config: ConnectionConfig,
        conversationId: Long,
    ): AppResult<Unit> {
        val settings = when (val result = conversationRepository.getConversationSettings(conversationId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val messages = when (val result = conversationRepository.getMessages(conversationId)) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data
        }
        val sentMessages = messages.filter { it.status == MessageStatus.SENT && it.content.isNotBlank() }
        if (sentMessages.size <= LIVE_MESSAGE_COUNT) {
            return AppResult.Failure(AppError.Unknown("Not enough conversation history to compact."))
        }

        val messagesToSummarize = sentMessages.dropLast(LIVE_MESSAGE_COUNT)
        val lastSummarizedMessage = messagesToSummarize.lastOrNull()
            ?: return AppResult.Failure(AppError.Unknown("Not enough conversation history to compact."))

        val summaryRequest = listOf(
            Message(
                id = 0L,
                conversationId = conversationId,
                branchId = 0L,
                role = MessageRole.USER,
                content = buildSummaryPrompt(messagesToSummarize),
                status = MessageStatus.SENT,
                errorMessage = null,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )

        val summary = when (
            val result = aiProviderRepository.sendChatMessage(
                config = config,
                modelName = settings.modelName,
                messages = summaryRequest,
                generationTimeoutMillis = settings.generationTimeoutMillis,
            )
        ) {
            is AppResult.Failure -> return result
            is AppResult.Success -> result.data.trim()
        }

        if (summary.isBlank()) {
            return AppResult.Failure(AppError.Unknown("Compaction returned an empty summary."))
        }

        return conversationRepository.saveActiveBranchSummary(
            conversationId = conversationId,
            summary = summary.take(MAX_SUMMARY_LENGTH),
            summaryUntilMessageId = lastSummarizedMessage.id,
        )
    }

    private fun buildSummaryPrompt(messages: List<Message>): String {
        val transcript = messages.joinToString(separator = "\n\n") { message ->
            "${message.role.name.lowercase()}: ${message.content}"
        }
        return """
            Summarize the following local chat history so the conversation can continue later.
            Keep durable facts, user preferences, decisions, constraints, open tasks, and unresolved questions.
            Do not add new facts.

            $transcript
        """.trimIndent()
    }

    private companion object {
        const val LIVE_MESSAGE_COUNT = 8
        const val MAX_SUMMARY_LENGTH = 4_000
    }
}
