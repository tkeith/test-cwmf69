package com.delayedmessaging.android.domain.model

import android.os.Parcelable // version: latest
import kotlinx.parcelize.Parcelize // version: latest
import com.delayedmessaging.android.domain.model.MessageStatus

/**
 * Data class representing a message in the delayed messaging system.
 * Implements Parcelable for efficient data transfer between Android components.
 *
 * @property id Unique identifier for the message
 * @property content The actual message text content
 * @property senderId Identifier of the user sending the message
 * @property recipientId Identifier of the message recipient
 * @property status Current status of the message in its lifecycle
 * @property createdAt Timestamp when the message was created (milliseconds since epoch)
 * @property scheduledFor Timestamp when the message is scheduled for delivery (milliseconds since epoch)
 * @property deliveredAt Timestamp when the message was delivered, null if not yet delivered
 */
@Parcelize
data class Message(
    val id: String,
    val content: String,
    val senderId: String,
    val recipientId: String,
    val status: MessageStatus,
    val createdAt: Long,
    val scheduledFor: Long,
    val deliveredAt: Long?
) : Parcelable {

    /**
     * Checks if the message has been delivered by verifying its current status.
     *
     * @return true if the message status is DELIVERED, false otherwise
     */
    fun isDelivered(): Boolean {
        return status == MessageStatus.DELIVERED
    }

    /**
     * Calculates the remaining time until message delivery in milliseconds.
     * Returns 0 if the scheduled delivery time has already passed.
     *
     * @return remaining time in milliseconds until scheduled delivery
     */
    fun getRemainingDeliveryTime(): Long {
        val currentTime = System.currentTimeMillis()
        return maxOf(0, scheduledFor - currentTime)
    }

    companion object {
        /**
         * Maximum content length for a message
         */
        const val MAX_CONTENT_LENGTH = 1000

        /**
         * Minimum delay time in milliseconds (30 seconds)
         */
        const val MIN_DELAY_MS = 30_000L

        /**
         * Maximum delay time in milliseconds (60 seconds)
         */
        const val MAX_DELAY_MS = 60_000L
    }
}