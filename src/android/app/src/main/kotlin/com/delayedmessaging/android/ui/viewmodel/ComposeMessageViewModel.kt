package com.delayedmessaging.android.ui.viewmodel

import androidx.lifecycle.ViewModel // version: 2.6.2
import androidx.lifecycle.viewModelScope // version: 2.6.2
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.data.repository.UserRepository
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import javax.inject.Inject // version: 1
import kotlinx.coroutines.flow.MutableStateFlow // version: 1.7.3
import kotlinx.coroutines.flow.StateFlow // version: 1.7.3
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job // version: 1.7.3
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
import java.util.UUID

/**
 * ViewModel responsible for managing message composition state and operations.
 * Implements enforced delivery delays (30-60 seconds) and comprehensive error handling.
 */
class ComposeMessageViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _messageContent = MutableStateFlow("")
    val messageContent: StateFlow<String> = _messageContent.asStateFlow()

    private val _recipientId = MutableStateFlow<String?>(null)
    val recipientId: StateFlow<String?> = _recipientId.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _remainingDeliveryTime = MutableStateFlow(0L)
    val remainingDeliveryTime: StateFlow<Long> = _remainingDeliveryTime.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Updates message content with validation.
     * Enforces maximum length constraint of 1000 characters.
     */
    fun updateMessageContent(content: String) {
        if (content.length <= Message.MAX_CONTENT_LENGTH) {
            _messageContent.value = content
            if (_error.value?.contains("length") == true) {
                _error.value = null
            }
        } else {
            _error.value = "Message cannot exceed ${Message.MAX_CONTENT_LENGTH} characters"
        }
    }

    /**
     * Sets and validates message recipient.
     * Verifies recipient exists before allowing message composition.
     */
    suspend fun setRecipient(userId: String) {
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(userId).collect { user ->
                    if (user != null) {
                        _recipientId.value = userId
                        _error.value = null
                    } else {
                        _error.value = "Invalid recipient"
                        _recipientId.value = null
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to validate recipient"
                _recipientId.value = null
            }
        }
    }

    /**
     * Initiates message sending with enforced delivery delay.
     * Implements 30-60 second delivery delay as per requirements.
     */
    suspend fun sendMessage() {
        if (!validateMessage()) {
            return
        }

        _isSending.value = true
        try {
            val message = Message(
                id = UUID.randomUUID().toString(),
                content = _messageContent.value,
                recipientId = _recipientId.value!!,
                status = MessageStatus.DRAFT,
                createdAt = System.currentTimeMillis(),
                scheduledFor = System.currentTimeMillis() + Message.MIN_DELAY_MS,
                deliveredAt = null
            )

            val result = messageRepository.sendMessage(message)
            if (result.isSuccess) {
                startDeliveryCountdown()
            } else {
                _error.value = "Failed to send message: ${result.exceptionOrNull()?.message}"
                _isSending.value = false
            }
        } catch (e: Exception) {
            _error.value = "Failed to send message: ${e.message}"
            _isSending.value = false
        }
    }

    /**
     * Manages delivery countdown timer for real-time status updates.
     * Updates remaining delivery time every second.
     */
    private fun startDeliveryCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remainingTime = Message.MAX_DELAY_MS
            while (remainingTime > 0) {
                _remainingDeliveryTime.value = remainingTime
                delay(1000) // Update every second
                remainingTime -= 1000
            }
            _remainingDeliveryTime.value = 0
            resetState()
        }
    }

    /**
     * Validates message before sending.
     * Checks content length and recipient validity.
     */
    private fun validateMessage(): Boolean {
        return when {
            _messageContent.value.isBlank() -> {
                _error.value = "Message content cannot be empty"
                false
            }
            _messageContent.value.length > Message.MAX_CONTENT_LENGTH -> {
                _error.value = "Message exceeds maximum length"
                false
            }
            _recipientId.value == null -> {
                _error.value = "Please select a recipient"
                false
            }
            else -> true
        }
    }

    /**
     * Resets ViewModel state after message sending or on error.
     */
    private fun resetState() {
        viewModelScope.launch {
            countdownJob?.cancelAndJoin()
            _messageContent.value = ""
            _recipientId.value = null
            _error.value = null
            _isSending.value = false
            _remainingDeliveryTime.value = 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}