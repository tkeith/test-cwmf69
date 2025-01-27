package com.delayedmessaging.android.data.local.dao

import androidx.room.Dao // version: 2.6.1
import androidx.room.Delete // version: 2.6.1
import androidx.room.Insert // version: 2.6.1
import androidx.room.OnConflictStrategy // version: 2.6.1
import androidx.room.Query // version: 2.6.1
import androidx.room.Update // version: 2.6.1
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow // version: 1.7.3

/**
 * Data Access Object (DAO) interface for handling message-related database operations.
 * Provides reactive database access using Kotlin Flow and optimized query performance.
 */
@Dao
interface MessageDao {
    /**
     * Retrieves all messages ordered by creation time in descending order.
     * Uses Flow for reactive updates when the underlying data changes.
     *
     * @return Flow emitting a list of all messages
     */
    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun getAllMessages(): Flow<List<Message>>

    /**
     * Retrieves a specific message by its unique identifier.
     * Returns null if no message is found with the given ID.
     *
     * @param messageId Unique identifier of the message to retrieve
     * @return Flow emitting the requested message or null if not found
     */
    @Query("SELECT * FROM messages WHERE id = :messageId")
    fun getMessageById(messageId: String): Flow<Message?>

    /**
     * Retrieves all messages with a specific status, ordered by creation time.
     * Useful for filtering messages based on their current state in the delivery lifecycle.
     *
     * @param status The message status to filter by
     * @return Flow emitting a list of messages with the specified status
     */
    @Query("SELECT * FROM messages WHERE status = :status ORDER BY createdAt DESC")
    fun getMessagesByStatus(status: MessageStatus): Flow<List<Message>>

    /**
     * Retrieves all pending messages (QUEUED or SENDING) ordered by scheduled delivery time.
     * Used for processing messages that need to be delivered.
     *
     * @return Flow emitting a list of pending messages
     */
    @Query("""
        SELECT * FROM messages 
        WHERE status IN ('QUEUED', 'SENDING') 
        ORDER BY scheduledFor ASC
    """)
    fun getPendingMessages(): Flow<List<Message>>

    /**
     * Inserts a new message into the database.
     * Uses REPLACE strategy to handle conflicts with existing messages.
     *
     * @param message The message to insert
     * @return Row ID of the inserted message
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    /**
     * Updates an existing message in the database.
     *
     * @param message The message to update
     * @return Number of messages updated (should be 1 if successful)
     */
    @Update
    suspend fun updateMessage(message: Message): Int

    /**
     * Updates the status of a message and sets the delivery timestamp if the status is DELIVERED.
     * Optimized query to update only necessary fields based on the status.
     *
     * @param messageId ID of the message to update
     * @param status New status for the message
     * @param timestamp Current timestamp for delivery time (used only when status is DELIVERED)
     * @return Number of messages updated (should be 1 if successful)
     */
    @Query("""
        UPDATE messages 
        SET status = :status, 
            deliveredAt = CASE 
                WHEN :status = 'DELIVERED' THEN :timestamp 
                ELSE deliveredAt 
            END 
        WHERE id = :messageId
    """)
    suspend fun updateMessageStatus(
        messageId: String,
        status: MessageStatus,
        timestamp: Long
    ): Int

    /**
     * Deletes a message from the database.
     *
     * @param message The message to delete
     * @return Number of messages deleted (should be 1 if successful)
     */
    @Delete
    suspend fun deleteMessage(message: Message): Int
}