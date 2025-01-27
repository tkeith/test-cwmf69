package com.delayedmessaging.android.domain.model

/**
 * Represents all possible states of a message in the delayed messaging system.
 * Used for tracking message lifecycle from composition through delivery.
 */
enum class MessageStatus {
    /**
     * Initial state when message is composed but not yet sent
     */
    DRAFT,

    /**
     * Message is queued for delayed delivery (30-60 seconds)
     */
    QUEUED,

    /**
     * Message is being processed for delivery after delay period
     */
    SENDING,

    /**
     * Message has been successfully delivered to recipient
     */
    DELIVERED,

    /**
     * Message delivery failed after maximum retry attempts
     */
    FAILED;

    /**
     * Returns a human-readable text representation of the message status for UI display.
     *
     * @return Localized string representing the current status
     */
    fun getDisplayText(): String {
        return when (this) {
            DRAFT -> "Draft"
            QUEUED -> "Queued"
            SENDING -> "Sending"
            DELIVERED -> "Delivered"
            FAILED -> "Failed"
        }
    }

    /**
     * Checks if the current status represents a terminal state where no further
     * status transitions are expected (delivered or failed).
     *
     * @return true if status is terminal (DELIVERED or FAILED), false otherwise
     */
    fun isTerminalState(): Boolean {
        return when (this) {
            DELIVERED, FAILED -> true
            else -> false
        }
    }
}