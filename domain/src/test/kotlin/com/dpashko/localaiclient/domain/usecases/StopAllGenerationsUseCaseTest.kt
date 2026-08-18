package com.dpashko.localaiclient.domain.usecases

import com.dpashko.localaiclient.domain.models.common.AppResult
import com.dpashko.localaiclient.domain.models.connection.ConnectionConfig
import com.dpashko.localaiclient.domain.models.conversation.Conversation
import com.dpashko.localaiclient.domain.models.conversation.Message
import com.dpashko.localaiclient.domain.models.error.AppError
import com.dpashko.localaiclient.domain.repositories.ChatGenerationScheduler
import com.dpashko.localaiclient.domain.repositories.ConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopAllGenerationsUseCaseTest {
    @Test
    fun `returns success without scheduler or repository cancel when no generations are active`() = runBlocking {
        val conversationRepository = FakeConversationRepository(
            generatingConversationIdsResult = AppResult.Success(emptyList()),
        )
        val chatGenerationScheduler = FakeChatGenerationScheduler()
        val useCase = StopAllGenerationsUseCase(conversationRepository, chatGenerationScheduler)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(emptyList<Long>(), chatGenerationScheduler.canceledConversationIds)
        assertFalse(conversationRepository.cancelGeneratingMessagesCalled)
    }

    @Test
    fun `cancels scheduler work and generating assistant messages for active conversations`() = runBlocking {
        val conversationRepository = FakeConversationRepository(
            generatingConversationIdsResult = AppResult.Success(listOf(1L, 2L)),
        )
        val chatGenerationScheduler = FakeChatGenerationScheduler()
        val useCase = StopAllGenerationsUseCase(conversationRepository, chatGenerationScheduler)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(listOf(1L, 2L), chatGenerationScheduler.canceledConversationIds)
        assertEquals(listOf(1L, 2L), conversationRepository.canceledGeneratingConversationIds)
    }

    @Test
    fun `does not mutate messages when scheduler cancel fails`() = runBlocking {
        val failure = AppResult.Failure(AppError.Unknown("cancel failed"))
        val conversationRepository = FakeConversationRepository(
            generatingConversationIdsResult = AppResult.Success(listOf(7L)),
        )
        val chatGenerationScheduler = FakeChatGenerationScheduler(cancelGenerationsResult = failure)
        val useCase = StopAllGenerationsUseCase(conversationRepository, chatGenerationScheduler)

        val result = useCase()

        assertEquals(failure, result)
        assertEquals(listOf(7L), chatGenerationScheduler.canceledConversationIds)
        assertFalse(conversationRepository.cancelGeneratingMessagesCalled)
    }
}

private class FakeConversationRepository(
    private val generatingConversationIdsResult: AppResult<List<Long>>,
) : ConversationRepository {
    var cancelGeneratingMessagesCalled = false
        private set
    var canceledGeneratingConversationIds = emptyList<Long>()
        private set

    override fun observeConversations(): Flow<List<Conversation>> = emptyFlow()

    override fun observeMessages(conversationId: Long): Flow<List<Message>> = emptyFlow()

    override fun observeHasGeneratingMessage(conversationId: Long): Flow<Boolean> = emptyFlow()

    override suspend fun getContextMessages(conversationId: Long): AppResult<List<Message>> =
        AppResult.Success(emptyList())

    override suspend fun messageExists(messageId: Long): AppResult<Boolean> =
        AppResult.Success(false)

    override suspend fun createConversation(modelName: String): AppResult<Long> =
        AppResult.Success(0L)

    override suspend fun deleteConversation(conversationId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun addUserMessage(conversationId: Long, content: String): AppResult<Long> =
        AppResult.Success(0L)

    override suspend fun addAssistantPlaceholder(conversationId: Long): AppResult<Long> =
        AppResult.Success(0L)

    override suspend fun completeAssistantMessage(messageId: Long, content: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun failAssistantMessage(messageId: Long, errorMessage: String): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun getGeneratingConversationIds(): AppResult<List<Long>> =
        generatingConversationIdsResult

    override suspend fun cancelGeneratingAssistantMessages(conversationId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun cancelGeneratingAssistantMessages(conversationIds: List<Long>): AppResult<Unit> {
        cancelGeneratingMessagesCalled = true
        canceledGeneratingConversationIds = conversationIds
        return AppResult.Success(Unit)
    }

    override suspend fun retryAssistantMessage(messageId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun editUserMessageAndDeleteNewer(
        conversationId: Long,
        messageId: Long,
        content: String,
    ): AppResult<Unit> = AppResult.Success(Unit)
}

private class FakeChatGenerationScheduler(
    private val cancelGenerationsResult: AppResult<Unit> = AppResult.Success(Unit),
) : ChatGenerationScheduler {
    var canceledConversationIds = emptyList<Long>()
        private set

    override suspend fun enqueueGeneration(
        config: ConnectionConfig,
        conversationId: Long,
        assistantMessageId: Long,
        modelName: String,
        replaceExisting: Boolean,
    ): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun cancelGeneration(conversationId: Long): AppResult<Unit> =
        AppResult.Success(Unit)

    override suspend fun cancelGenerations(conversationIds: List<Long>): AppResult<Unit> {
        canceledConversationIds = conversationIds
        return cancelGenerationsResult
    }
}
