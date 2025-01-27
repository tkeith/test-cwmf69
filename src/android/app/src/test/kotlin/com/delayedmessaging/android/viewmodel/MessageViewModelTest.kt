package com.delayedmessaging.android.viewmodel

import app.cash.turbine.test // version: 1.0.0
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.ui.viewmodel.MessageListViewModel
import com.google.common.truth.Truth.assertThat // version: 1.1.3
import io.mockk.coEvery // version: 1.13.5
import io.mockk.coVerify // version: 1.13.5
import io.mockk.mockk // version: 1.13.5
import kotlinx.coroutines.ExperimentalCoroutinesApi // version: 1.7.3
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher // version: 1.7.3
import kotlinx.coroutines.test.runBlockingTest // version: 1.7.3
import org.junit.Before // version: 4.13.2
import org.junit.Rule // version: 4.13.2
import org.junit.Test // version: 4.13.2
import androidx.arch.core.executor.testing.InstantTaskExecutorRule // version: 2.2.0
import kotlinx.coroutines.test.TestCoroutineScope
import java.util.UUID

@ExperimentalCoroutinesApi
class MessageViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()
    private val testScope = TestCoroutineScope(testDispatcher)
    
    private lateinit var viewModel: MessageListViewModel
    private lateinit var messageRepository: MessageRepository

    private companion object {
        const val TEST_USER_ID = "test_user_id"
        const val TEST_MESSAGE_ID = "test_message_id"
        const val TEST_CONTENT = "Test message content"
        const val TEST_ERROR_MESSAGE = "Test error message"
    }

    @Before
    fun setup() {
        messageRepository = mockk(relaxed = true)
        viewModel = MessageListViewModel(messageRepository)
    }

    @Test
    fun `refreshMessages should update state with sorted messages`() = testScope.runBlockingTest {
        // Arrange
        val messages = createTestMessages()
        coEvery { messageRepository.getAllMessages() } returns flowOf(messages)

        // Act
        viewModel.refreshMessages()

        // Assert
        viewModel.messages.test {
            val emittedMessages = awaitItem()
            assertThat(emittedMessages).isNotEmpty()
            assertThat(emittedMessages).isSortedByDescending { it.createdAt }
            assertThat(emittedMessages.size).isEqualTo(messages.size)
        }
        
        viewModel.isLoading.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `refreshMessages should handle error and update error state`() = testScope.runBlockingTest {
        // Arrange
        coEvery { messageRepository.getAllMessages() } throws Exception(TEST_ERROR_MESSAGE)

        // Act
        viewModel.refreshMessages()

        // Assert
        viewModel.error.test {
            val error = awaitItem()
            assertThat(error).contains(TEST_ERROR_MESSAGE)
        }
        
        viewModel.isLoading.test {
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `updateMessageStatus should validate status transition`() = testScope.runBlockingTest {
        // Arrange
        val message = createTestMessage(MessageStatus.QUEUED)
        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(message))
        viewModel.refreshMessages()

        // Act & Assert
        // Valid transition: QUEUED -> SENDING
        viewModel.messages.test {
            val success = viewModel.updateMessageStatus(message.id, MessageStatus.SENDING)
            assertThat(success).isTrue()
        }

        // Invalid transition: DELIVERED -> SENDING
        val deliveredMessage = message.copy(status = MessageStatus.DELIVERED)
        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(deliveredMessage))
        viewModel.refreshMessages()

        viewModel.messages.test {
            val success = viewModel.updateMessageStatus(deliveredMessage.id, MessageStatus.SENDING)
            assertThat(success).isFalse()
        }
    }

    @Test
    fun `selectMessage should update selected message state`() = testScope.runBlockingTest {
        // Arrange
        val message = createTestMessage(MessageStatus.QUEUED)
        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(message))
        viewModel.refreshMessages()

        // Act
        viewModel.selectMessage(message.id)

        // Assert
        viewModel.selectedMessage.test {
            val selectedMessage = awaitItem()
            assertThat(selectedMessage).isNotNull()
            assertThat(selectedMessage?.id).isEqualTo(message.id)
        }
    }

    @Test
    fun `selectMessage should handle non-existent message`() = testScope.runBlockingTest {
        // Arrange
        coEvery { messageRepository.getAllMessages() } returns flowOf(emptyList())
        viewModel.refreshMessages()

        // Act
        viewModel.selectMessage("non_existent_id")

        // Assert
        viewModel.error.test {
            val error = awaitItem()
            assertThat(error).contains("Message not found")
        }
    }

    @Test
    fun `real-time updates should reflect message status changes`() = testScope.runBlockingTest {
        // Arrange
        val initialMessage = createTestMessage(MessageStatus.QUEUED)
        val updatedMessage = initialMessage.copy(status = MessageStatus.SENDING)
        
        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(initialMessage))
        viewModel.refreshMessages()

        // Act
        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(updatedMessage))
        viewModel.refreshMessages()

        // Assert
        viewModel.messages.test {
            val messages = awaitItem()
            assertThat(messages.first().status).isEqualTo(MessageStatus.SENDING)
        }
    }

    @Test
    fun `delivery time updates should be calculated correctly`() = testScope.runBlockingTest {
        // Arrange
        val currentTime = System.currentTimeMillis()
        val scheduledTime = currentTime + 30000 // 30 seconds delay
        val message = createTestMessage(
            MessageStatus.QUEUED,
            scheduledFor = scheduledTime
        )

        coEvery { messageRepository.getAllMessages() } returns flowOf(listOf(message))
        viewModel.refreshMessages()

        // Assert
        viewModel.messages.test {
            val messages = awaitItem()
            val remainingTime = messages.first().getRemainingDeliveryTime()
            assertThat(remainingTime).isAtMost(30000)
            assertThat(remainingTime).isGreaterThan(0)
        }
    }

    private fun createTestMessage(
        status: MessageStatus,
        scheduledFor: Long = System.currentTimeMillis() + 30000
    ): Message {
        return Message(
            id = TEST_MESSAGE_ID,
            content = TEST_CONTENT,
            senderId = TEST_USER_ID,
            recipientId = UUID.randomUUID().toString(),
            status = status,
            createdAt = System.currentTimeMillis(),
            scheduledFor = scheduledFor,
            deliveredAt = null
        )
    }

    private fun createTestMessages(): List<Message> {
        val currentTime = System.currentTimeMillis()
        return listOf(
            createTestMessage(MessageStatus.QUEUED, currentTime + 30000),
            createTestMessage(MessageStatus.SENDING, currentTime + 45000),
            createTestMessage(MessageStatus.DELIVERED, currentTime + 15000)
        )
    }
}