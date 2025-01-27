package com.delayedmessaging.android.domain.usecase

import javax.inject.Inject // version: 1
import kotlinx.coroutines.flow.Flow // version: 1.7.3
import kotlinx.coroutines.flow.map // version: 1.7.3
import kotlinx.coroutines.flow.flowOn // version: 1.7.3
import kotlinx.coroutines.Dispatchers // version: 1.7.3
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG

/**
 * Use case implementation for retrieving messages with enforced delay logic.
 * Implements comprehensive delivery time validation and status tracking.
 * Follows clean architecture principles for separation of concerns.
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {
    /**
     * Retrieves all messages with delivery time validation and status tracking.
     * Messages are filtered based on delivery constraints and sorted by creation time.
     *
     * @return Flow emitting a list of validated messages
     */
    fun execute(): Flow<List<Message>> = messageRepository.getAllMessages()
        .map { messages ->
            messages
                .filter { message -> isMessageDeliverable(message) }
                .sortedByDescending { it.createdAt }
        }
        .flowOn(Dispatchers.Default)

    /**
     * Retrieves a specific message by ID with delivery time validation.
     *
     * @param messageId Unique identifier of the message to retrieve
     * @return Flow emitting the validated message or null if not found/deliverable
     */
    fun getMessageById(messageId: String): Flow<Message?> = messageRepository
        .getMessageById(messageId)
        .map { message ->
            message?.takeIf { isMessageDeliverable(it) }
        }
        .flowOn(Dispatchers.Default)

    /**
     * Validates message delivery constraints including status and timing requirements.
     *
     * @param message Message to validate
     * @return true if message meets all delivery criteria
     */
    private fun isMessageDeliverable(message: Message): Boolean {
        val currentTime = System.currentTimeMillis()
        val remainingTime = message.getRemainingDeliveryTime()

        return when {
            // Allow immediate access for delivered or failed messages
            message.status == MessageStatus.DELIVERED || 
            message.status == MessageStatus.FAILED -> true

            // Enforce minimum delay of 30 seconds
            remainingTime > 0 && 
            remainingTime < MESSAGE_CONFIG.MIN_DELAY_SECONDS * 1000 -> false

            // Enforce maximum delay of 60 seconds
            remainingTime > MESSAGE_CONFIG.MAX_DELAY_SECONDS * 1000 -> false

            // Validate delivery window
            currentTime < message.deliveryTime -> false

            // Allow access for messages within valid delivery window
            else -> true
        }
    }

    companion object {
        private const val TAG = "GetMessagesUseCase"
    }
}