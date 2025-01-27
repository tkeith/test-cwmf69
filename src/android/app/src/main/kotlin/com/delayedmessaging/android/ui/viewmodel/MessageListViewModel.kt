package com.delayedmessaging.android.ui.viewmodel

import androidx.lifecycle.ViewModel // version: 2.6.1
import androidx.lifecycle.viewModelScope // version: 2.6.1
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.data.repository.MessageRepository
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import javax.inject.Inject // version: 1
import kotlinx.coroutines.flow.StateFlow // version: 1.7.3
import kotlinx.coroutines.flow.MutableStateFlow // version: 1.7.3
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.coroutineScope // version: 1.7.3

/**
 * ViewModel responsible for managing message list state and operations.
 * Provides real-time updates of message statuses and delivery times.
 */
class MessageListViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    // UI state holders
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _selectedMessage = MutableStateFlow<Message?>(null)
    val selectedMessage: StateFlow<Message?> = _selectedMessage

    // Job for managing message updates
    private var messageUpdateJob: Job? = null

    init {
        startMessageUpdates()
        refreshMessages()
    }

    /**
     * Refreshes the message list from repository with enhanced error handling.
     */
    fun refreshMessages() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                messageRepository.getAllMessages()
                    .map { messages -> 
                        messages.sortedByDescending { it.createdAt }
                    }
                    .collect { sortedMessages ->
                        _messages.value = sortedMessages
                    }
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Starts collecting real-time message updates from repository.
     * Implements automatic retry mechanism and error handling.
     */
    private fun startMessageUpdates() {
        messageUpdateJob?.cancel()
        messageUpdateJob = viewModelScope.launch {
            try {
                messageRepository.getAllMessages()
                    .catch { e ->
                        _error.value = "Update stream error: ${e.localizedMessage}"
                        delay(MESSAGE_CONFIG.RETRY_DELAY_MS)
                        startMessageUpdates()
                    }
                    .collectLatest { updatedMessages ->
                        val sortedMessages = updatedMessages.sortedByDescending { it.createdAt }
                        _messages.value = sortedMessages
                        updateDeliveryTimes(sortedMessages)
                    }
            } catch (e: Exception) {
                _error.value = "Failed to start message updates: ${e.localizedMessage}"
                delay(MESSAGE_CONFIG.RETRY_DELAY_MS)
                startMessageUpdates()
            }
        }
    }

    /**
     * Updates delivery times for messages in real-time.
     * Triggers UI updates for remaining delivery time countdown.
     */
    private suspend fun updateDeliveryTimes(messages: List<Message>) = coroutineScope {
        val queuedMessages = messages.filter { 
            it.status == MessageStatus.QUEUED || it.status == MessageStatus.SENDING 
        }
        
        if (queuedMessages.isNotEmpty()) {
            launch {
                while (true) {
                    val updatedMessages = _messages.value.map { message ->
                        if (message.status == MessageStatus.QUEUED || 
                            message.status == MessageStatus.SENDING) {
                            message.copy(
                                deliveryTime = message.getRemainingDeliveryTime()
                            )
                        } else message
                    }
                    _messages.value = updatedMessages
                    delay(MESSAGE_CONFIG.STATUS_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Updates the status of a specific message with validation.
     * 
     * @param messageId ID of the message to update
     * @param status New status for the message
     * @return Success status of the update operation
     */
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus): Boolean {
        return try {
            val currentMessage = _messages.value.find { it.id == messageId }
            if (currentMessage == null) {
                _error.value = "Message not found"
                return false
            }

            if (!isValidStatusTransition(currentMessage.status, status)) {
                _error.value = "Invalid status transition"
                return false
            }

            messageRepository.updateMessageStatus(messageId, status)
            true
        } catch (e: Exception) {
            _error.value = "Failed to update message status: ${e.localizedMessage}"
            false
        }
    }

    /**
     * Selects a message for detailed view.
     * 
     * @param messageId ID of the message to select
     */
    fun selectMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val message = _messages.value.find { it.id == messageId }
                if (message != null) {
                    _selectedMessage.value = message
                } else {
                    _error.value = "Message not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to select message: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Validates if a status transition is allowed based on the message lifecycle.
     */
    private fun isValidStatusTransition(currentStatus: MessageStatus, newStatus: MessageStatus): Boolean {
        return when (currentStatus) {
            MessageStatus.QUEUED -> newStatus == MessageStatus.SENDING || newStatus == MessageStatus.FAILED
            MessageStatus.SENDING -> newStatus == MessageStatus.DELIVERED || newStatus == MessageStatus.FAILED
            MessageStatus.DELIVERED, MessageStatus.FAILED -> false
            else -> true
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageUpdateJob?.cancel()
    }
}