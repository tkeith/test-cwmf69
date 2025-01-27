package com.delayedmessaging.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.domain.model.UserPresence
import kotlinx.coroutines.flow.Flow

/**
 * Room Database Access Object (DAO) interface for User entity operations.
 * Provides reactive data access using Kotlin Flow and coroutines for asynchronous operations.
 * Implements CRUD operations with transaction safety and conflict resolution.
 */
@Dao
interface UserDao {

    /**
     * Retrieves a specific user by their unique identifier with reactive updates.
     *
     * @param userId The unique identifier of the user to retrieve
     * @return Flow emitting the user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): Flow<User?>

    /**
     * Retrieves a user by their username with reactive updates.
     * Used primarily for authentication and user search functionality.
     *
     * @param username The username to search for
     * @return Flow emitting the user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE username = :username")
    fun getUserByUsername(username: String): Flow<User?>

    /**
     * Retrieves a user by their email address with reactive updates.
     * Used primarily for account recovery and verification.
     *
     * @param email The email address to search for
     * @return Flow emitting the user if found, null otherwise
     */
    @Query("SELECT * FROM users WHERE email = :email")
    fun getUserByEmail(email: String): Flow<User?>

    /**
     * Retrieves all users from the database ordered by their last active timestamp.
     * Provides reactive updates for user list changes.
     *
     * @return Flow emitting a list of all users
     */
    @Query("SELECT * FROM users ORDER BY lastActive DESC")
    fun getAllUsers(): Flow<List<User>>

    /**
     * Retrieves all currently online users ordered by their last active timestamp.
     * Used for presence monitoring and active user list display.
     *
     * @return Flow emitting a list of online users
     */
    @Query("""
        SELECT * FROM users 
        WHERE presence = 'ONLINE' 
        ORDER BY lastActive DESC
    """)
    fun getActiveUsers(): Flow<List<User>>

    /**
     * Inserts a new user into the database with conflict resolution.
     * Uses REPLACE strategy to handle existing user conflicts.
     *
     * @param user The user entity to insert
     * @return The row ID of the inserted user
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    /**
     * Updates an existing user's information in the database.
     * Performs a full update of all user fields.
     *
     * @param user The user entity with updated information
     */
    @Update
    suspend fun updateUser(user: User)

    /**
     * Updates a user's presence status and last active timestamp.
     * Used for real-time presence tracking.
     *
     * @param userId The ID of the user to update
     * @param presence The new presence status
     * @param timestamp The current timestamp in milliseconds
     */
    @Query("""
        UPDATE users 
        SET presence = :presence, 
            lastActive = :timestamp 
        WHERE id = :userId
    """)
    suspend fun updateUserPresence(
        userId: String,
        presence: UserPresence,
        timestamp: Long
    )

    /**
     * Deletes a specific user from the database.
     * Used for account deletion and cleanup.
     *
     * @param user The user entity to delete
     */
    @Delete
    suspend fun deleteUser(user: User)

    /**
     * Deletes all users from the database.
     * Used for database cleanup and testing.
     * Should be used with caution in production.
     */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    /**
     * Retrieves users who have been inactive beyond the specified threshold.
     * Used for automatic presence status updates.
     *
     * @param threshold The timestamp threshold in milliseconds
     * @return Flow emitting a list of inactive users
     */
    @Query("""
        SELECT * FROM users 
        WHERE lastActive < :threshold 
        AND presence = 'ONLINE'
    """)
    fun getInactiveUsers(threshold: Long): Flow<List<User>>

    /**
     * Updates presence status for users who have been inactive beyond the threshold.
     * Used for batch presence status updates.
     *
     * @param threshold The timestamp threshold in milliseconds
     * @param newPresence The new presence status to set
     */
    @Transaction
    @Query("""
        UPDATE users 
        SET presence = :newPresence 
        WHERE lastActive < :threshold 
        AND presence = 'ONLINE'
    """)
    suspend fun updateInactiveUsersPresence(
        threshold: Long,
        newPresence: UserPresence
    )
}