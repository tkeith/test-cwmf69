package com.delayedmessaging.android.domain.usecase

import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import javax.inject.Inject // version: 1
import kotlinx.coroutines.flow.Flow // version: 1.7.3
import kotlinx.coroutines.flow.flow
import kotlin.Result // version: 1.9.0
import kotlin.random.Random

/**
 * Use case implementation for sending messages with enforced delay in the Android application.
 * Ensures precise delivery timing and comprehensive validation of message parameters.
 *
 * @property messageRepository Repository handling message persistence and delivery
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository
) {

    /**
     * Executes the message sending operation with enforced 30-60 second delay.
     * Implements comprehensive validation and precise timing control.
     *
     * @param message Message to be sent
     * @return Flow emitting Result containing message ID on success or error details on failure
     */
    suspend fun execute(message: Message): Flow<Result<String>> = flow {
        try {
            // Validate message content and parameters
            validateMessage(message).fold(
                onSuccess = {
                    // Calculate random delay between 30-60 seconds
                    val delayMs = Random.nextLong(
                        MESSAGE_CONFIG.MIN_DELAY_SECONDS * 1000,
                        MESSAGE_CONFIG.MAX_DELAY_SECONDS * 1000
                    )

                    // Create message with calculated delay and QUEUED status
                    val messageToSend = message.copy(
                        status = MessageStatus.QUEUED,
                        scheduledFor = System.currentTimeMillis() + delayMs,
                        createdAt = System.currentTimeMillis()
                    )

                    // Send message through repository
                    messageRepository.sendMessage(messageToSend).fold(
                        onSuccess = { messageId ->
                            emit(Result.success(messageId))
                        },
                        onFailure = { error ->
                            emit(Result.failure(error))
                        }
                    )
                },
                onFailure = { error ->
                    emit(Result.failure(error))
                }
            )
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Performs comprehensive message validation including content length,
     * recipient validity, and timing parameters.
     *
     * @param message Message to validate
     * @return Result indicating validation success or detailed error
     */
    private suspend fun validateMessage(message: Message): Result<Boolean> {
        return try {
            when {
                message.content.isBlank() -> {
                    Result.failure(IllegalArgumentException("Message content cannot be empty"))
                }
                message.content.length > Message.MAX_CONTENT_LENGTH -> {
                    Result.failure(IllegalArgumentException("Message exceeds maximum length of ${Message.MAX_CONTENT_LENGTH} characters"))
                }
                message.recipientId.isBlank() -> {
                    Result.failure(IllegalArgumentException("Recipient ID cannot be empty"))
                }
                message.status != MessageStatus.DRAFT -> {
                    Result.failure(IllegalArgumentException("Message must be in DRAFT status for sending"))
                }
                else -> {
                    // Verify recipient exists and is valid
                    messageRepository.validateRecipient(message.recipientId).fold(
                        onSuccess = {
                            Result.success(true)
                        },
                        onFailure = { error ->
                            Result.failure(error)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}