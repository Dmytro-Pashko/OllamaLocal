package com.dpashko.localaiclient.domain.repositories

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.models.conversation.Message
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for on-device conversation and message persistence.
 */
interface ConversationRepository {
    /**
     * Observes conversation list metadata ordered for display.
     */
    fun observeConversations(): Flow<List<Conversation>>

    /**
     * Observes all messages that belong to [conversationId].
     */
    fun observeMessages(conversationId: Long): Flow<List<Message>>

    /**
     * Observes whether [conversationId] currently has a generating assistant placeholder.
     */
    fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean>

    /**
     * Returns sent, non-empty messages that should be sent as provider context.
     */
    suspend fun getContextMessages(conversationId: Long): AppResult<List<Message>>

    /**
     * Returns whether a message still exists in local storage.
     */
    suspend fun messageExists(messageId: Long): AppResult<Boolean>

    /**
     * Creates a new conversation for [modelName] and returns its local id.
     */
    suspend fun createConversation(modelName: String): AppResult<Long>

    /**
     * Deletes a conversation and its messages from local storage.
     */
    suspend fun deleteConversation(conversationId: Long): AppResult<Unit>

    /**
     * Adds a user message to [conversationId] and returns the inserted message id.
     */
    suspend fun addUserMessage(
        conversationId: Long,
        content: String,
    ): AppResult<Long>

    /**
     * Adds an empty generating assistant placeholder and returns its message id.
     */
    suspend fun addAssistantPlaceholder(conversationId: Long): AppResult<Long>

    /**
     * Marks a generating assistant message as sent with completed [content].
     */
    suspend fun completeAssistantMessage(
        messageId: Long,
        content: String,
    ): AppResult<Unit>

    /**
     * Marks a generating assistant message as failed with [errorMessage].
     */
    suspend fun failAssistantMessage(
        messageId: Long,
        errorMessage: String,
    ): AppResult<Unit>

    /**
     * Returns conversation ids that currently contain generating assistant messages.
     */
    suspend fun getGeneratingConversationIds(): AppResult<List<Long>>

    /**
     * Marks all generating assistant messages in one conversation as canceled.
     */
    suspend fun cancelGeneratingAssistantMessages(conversationId: Long): AppResult<Unit>

    /**
     * Marks all generating assistant messages in multiple conversations as canceled.
     */
    suspend fun cancelGeneratingAssistantMessages(conversationIds: List<Long>): AppResult<Unit>

    /**
     * Resets a failed assistant message to generating so background work can retry it.
     */
    suspend fun retryAssistantMessage(messageId: Long): AppResult<Unit>

    /**
     * Updates a user message and removes newer messages so regeneration uses the edited context.
     */
    suspend fun editUserMessageAndDeleteNewer(
        conversationId: Long,
        messageId: Long,
        content: String,
    ): AppResult<Unit>
}
