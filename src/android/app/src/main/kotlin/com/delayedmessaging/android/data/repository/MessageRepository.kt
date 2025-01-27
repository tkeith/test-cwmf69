package com.delayedmessaging.android.data.repository

import com.delayedmessaging.android.data.api.ApiService
import com.delayedmessaging.android.data.local.dao.MessageDao
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import javax.inject.Inject // version: 1
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope // version: 1.7.3
import kotlinx.coroutines.flow.Flow // version: 1.7.3
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import retrofit2.Response // version: 2.9.0
import java.io.IOException
import kotlin.math.max

/**
 * Repository implementation that coordinates message operations between local database and remote API.
 * Implements offline-first architecture with synchronization capabilities and enforced message delays.
 */
@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val apiService: ApiService,
    private val coroutineScope: CoroutineScope,
    private val networkMonitor: NetworkMonitor
) {
    private val syncMutex = Mutex()
    private val retryPolicy = RetryPolicy(
        maxAttempts = MESSAGE_CONFIG.MESSAGE_RETRY_ATTEMPTS,
        initialDelayMs = MESSAGE_CONFIG.RETRY_DELAY_MS
    )

    init {
        setupNetworkMonitoring()
    }

    /**
     * Retrieves all messages with real-time updates.
     * Implements offline-first pattern with background synchronization.
     */
    fun getAllMessages(): Flow<List<Message>> = messageDao.getAllMessages()
        .combine(networkMonitor.isOnline) { messages, isOnline ->
            if (isOnline) {
                coroutineScope.launch {
                    syncMessages()
                }
            }
            messages
        }
        .catch { error ->
            // Log error and emit empty list as fallback
            error.printStackTrace()
            emit(emptyList())
        }

    /**
     * Sends a new message with enforced 30-60 second delivery delay.
     * Implements local-first storage with background synchronization.
     */
    suspend fun sendMessage(message: Message): Result<String> = try {
        // Validate message content
        if (message.content.length > Message.MAX_CONTENT_LENGTH) {
            return Result.failure(IllegalArgumentException("Message exceeds maximum length"))
        }

        // Calculate random delay between 30-60 seconds
        val delayMs = Random.nextLong(
            Message.MIN_DELAY_MS,
            Message.MAX_DELAY_MS
        )

        // Store message locally with QUEUED status
        val messageWithDelay = message.copy(
            status = MessageStatus.QUEUED,
            scheduledFor = System.currentTimeMillis() + delayMs
        )
        messageDao.insertMessage(messageWithDelay)

        // Schedule delayed delivery
        coroutineScope.launch {
            delay(delayMs)
            deliverMessage(messageWithDelay)
        }

        Result.success(message.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Synchronizes local messages with remote server using conflict resolution.
     */
    private suspend fun syncMessages() = syncMutex.withLock {
        try {
            // Fetch remote changes
            val response = retryPolicy.execute {
                apiService.getMessages(
                    authToken = getAuthToken(),
                    page = 0,
                    size = MESSAGE_CONFIG.BATCH_SIZE
                )
            }

            if (response.isSuccessful) {
                val remoteMessages = response.body() ?: emptyList()
                updateLocalMessages(remoteMessages)
                syncPendingMessages()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Delivers a message after the delay period has elapsed.
     */
    private suspend fun deliverMessage(message: Message) {
        try {
            // Update status to SENDING
            messageDao.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.SENDING,
                timestamp = System.currentTimeMillis()
            )

            // Attempt delivery with retry policy
            val response = retryPolicy.execute {
                apiService.sendMessage(
                    authToken = getAuthToken(),
                    message = message
                )
            }

            // Update local status based on delivery result
            val finalStatus = if (response.isSuccessful) {
                MessageStatus.DELIVERED
            } else {
                MessageStatus.FAILED
            }

            messageDao.updateMessageStatus(
                messageId = message.id,
                status = finalStatus,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            messageDao.updateMessageStatus(
                messageId = message.id,
                status = MessageStatus.FAILED,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    /**
     * Updates local database with remote messages using conflict resolution.
     */
    private suspend fun updateLocalMessages(remoteMessages: List<Message>) {
        remoteMessages.forEach { remoteMessage ->
            messageDao.getMessageById(remoteMessage.id)
                .collect { localMessage ->
                    if (localMessage == null || 
                        shouldUpdateLocalMessage(localMessage, remoteMessage)) {
                        messageDao.insertMessage(remoteMessage)
                    }
                }
        }
    }

    /**
     * Synchronizes pending local messages to remote server.
     */
    private suspend fun syncPendingMessages() {
        messageDao.getPendingMessages()
            .collect { pendingMessages ->
                pendingMessages.forEach { message ->
                    if (message.getRemainingDeliveryTime() <= 0) {
                        deliverMessage(message)
                    }
                }
            }
    }

    /**
     * Determines if local message should be updated with remote version.
     */
    private fun shouldUpdateLocalMessage(local: Message, remote: Message): Boolean {
        return when {
            remote.status.isTerminalState() -> true
            local.status == MessageStatus.FAILED -> true
            else -> false
        }
    }

    /**
     * Sets up network state monitoring for automatic synchronization.
     */
    private fun setupNetworkMonitoring() {
        coroutineScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    syncMessages()
                }
            }
        }
    }

    private fun getAuthToken(): String {
        // Implementation would retrieve token from secure storage
        return "Bearer ${getStoredAuthToken()}"
    }

    private fun getStoredAuthToken(): String {
        // Implementation would access encrypted SharedPreferences
        return ""
    }
}

/**
 * Helper class for implementing exponential backoff retry logic.
 */
private class RetryPolicy(
    private val maxAttempts: Int,
    private val initialDelayMs: Long
) {
    suspend fun <T> execute(block: suspend () -> Response<T>): Response<T> {
        var currentDelay = initialDelayMs
        repeat(maxAttempts) { attempt ->
            try {
                val response = block()
                if (response.isSuccessful || attempt == maxAttempts - 1) {
                    return response
                }
            } catch (e: IOException) {
                if (attempt == maxAttempts - 1) throw e
            }
            delay(currentDelay)
            currentDelay *= 2
        }
        throw IOException("Max retry attempts exceeded")
    }
}