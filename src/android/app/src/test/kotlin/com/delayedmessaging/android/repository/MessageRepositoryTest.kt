package com.delayedmessaging.android.repository

import com.delayedmessaging.android.data.api.ApiService
import com.delayedmessaging.android.data.local.dao.MessageDao
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import com.google.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryTest {

    private lateinit var mockMessageDao: MessageDao
    private lateinit var mockApiService: ApiService
    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var repository: MessageRepository
    private lateinit var testScope: TestScope
    private lateinit var testDispatcher: TestDispatcher

    companion object {
        private const val TEST_MESSAGE_ID = "test-message-id"
        private const val TEST_CONTENT = "Test message content"
        private const val TEST_SENDER_ID = "sender-123"
        private const val TEST_RECIPIENT_ID = "recipient-456"
        private const val MIN_DELAY_MS = MESSAGE_CONFIG.MIN_DELAY_SECONDS * 1000L
        private const val MAX_DELAY_MS = MESSAGE_CONFIG.MAX_DELAY_SECONDS * 1000L
        private const val DELIVERY_ACCURACY_MS = 1000L
    }

    @Before
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        mockMessageDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkMonitor = mockk(relaxed = true)

        // Setup default mock behaviors
        every { mockNetworkMonitor.isOnline } returns flowOf(true)
        coEvery { mockMessageDao.getAllMessages() } returns flowOf(emptyList())
        coEvery { mockMessageDao.getMessageById(any()) } returns flowOf(null)
        coEvery { mockApiService.sendMessage(any(), any()) } returns Response.success(createTestMessage())

        repository = MessageRepository(
            messageDao = mockMessageDao,
            apiService = mockApiService,
            coroutineScope = testScope,
            networkMonitor = mockNetworkMonitor
        )
    }

    @Test
    fun `test message delivery delay enforcement`() = runTest {
        // Create test message
        val message = createTestMessage()
        val messageSlot = slot<Message>()
        
        coEvery { mockMessageDao.insertMessage(capture(messageSlot)) } returns 1L
        
        // Send message
        val result = repository.sendMessage(message)
        
        // Verify message was queued
        assertThat(result.isSuccess).isTrue()
        assertThat(messageSlot.captured.status).isEqualTo(MessageStatus.QUEUED)
        
        // Verify message not delivered before minimum delay
        advanceTimeBy(MIN_DELAY_MS - DELIVERY_ACCURACY_MS)
        coVerify(exactly = 0) { mockApiService.sendMessage(any(), any()) }
        
        // Verify message delivered after minimum delay
        advanceTimeBy(DELIVERY_ACCURACY_MS * 2)
        coVerify(exactly = 1) { mockApiService.sendMessage(any(), any()) }
        
        // Verify status transitions
        coVerify { 
            mockMessageDao.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.SENDING,
                any()
            )
        }
        coVerify { 
            mockMessageDao.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.DELIVERED,
                any()
            )
        }
    }

    @Test
    fun `test offline synchronization`() = runTest {
        // Setup offline state
        every { mockNetworkMonitor.isOnline } returns flowOf(false)
        val message = createTestMessage()
        
        // Send message while offline
        repository.sendMessage(message)
        
        // Verify stored locally
        coVerify { mockMessageDao.insertMessage(any()) }
        coVerify(exactly = 0) { mockApiService.sendMessage(any(), any()) }
        
        // Simulate coming online
        every { mockNetworkMonitor.isOnline } returns flowOf(true)
        
        // Advance past delay period
        advanceTimeBy(MAX_DELAY_MS)
        
        // Verify sync attempt
        coVerify { mockApiService.sendMessage(any(), any()) }
        coVerify { 
            mockMessageDao.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.DELIVERED,
                any()
            )
        }
    }

    @Test
    fun `test message status flow`() = runTest {
        val message = createTestMessage()
        val statusSlot = slot<MessageStatus>()
        
        coEvery { 
            mockMessageDao.updateMessageStatus(
                messageId = any(),
                status = capture(statusSlot),
                timestamp = any()
            )
        } returns 1
        
        // Send message
        repository.sendMessage(message)
        
        // Verify initial status
        assertThat(statusSlot.captured).isEqualTo(MessageStatus.QUEUED)
        
        // Advance to delivery time
        advanceTimeBy(MIN_DELAY_MS)
        
        // Verify status transitions
        assertThat(statusSlot.captured).isEqualTo(MessageStatus.SENDING)
        coVerify { mockApiService.sendMessage(any(), any()) }
        assertThat(statusSlot.captured).isEqualTo(MessageStatus.DELIVERED)
    }

    private fun createTestMessage() = Message(
        id = TEST_MESSAGE_ID,
        content = TEST_CONTENT,
        senderId = TEST_SENDER_ID,
        recipientId = TEST_RECIPIENT_ID,
        status = MessageStatus.DRAFT,
        createdAt = System.currentTimeMillis(),
        scheduledFor = System.currentTimeMillis() + MIN_DELAY_MS,
        deliveredAt = null
    )
}