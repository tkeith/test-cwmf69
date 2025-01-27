package com.delayedmessaging.android.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.util.Constants.USER_STATUS

/**
 * Thread-safe, immutable data class representing a user entity in the Delayed Messaging system.
 * Implements Parcelable for efficient data transfer between Android components.
 *
 * @property id Unique identifier for the user
 * @property username User's chosen display name
 * @property email User's email address for account management
 * @property presence Current presence information including online status
 * @property settings User preferences including theme and notification settings
 * @property createdAt Account creation timestamp (Unix timestamp in milliseconds)
 * @property updatedAt Last profile update timestamp (Unix timestamp in milliseconds)
 */
@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String,
    val presence: UserPresence,
    val settings: UserSettings,
    val createdAt: Long,
    val updatedAt: Long
) : Parcelable {

    /**
     * Thread-safe check for user's online status.
     * Delegates to the UserPresence component for status verification.
     *
     * @return true if the user is currently online
     */
    fun isOnline(): Boolean = presence.isOnline()

    /**
     * Retrieves the user's display name with null safety.
     * Ensures a non-null string is always returned for UI display.
     *
     * @return formatted username for display
     */
    fun getDisplayName(): String = username.takeIf { it.isNotBlank() } ?: "User-${id.take(8)}"
}

/**
 * Thread-safe, immutable data class representing user preferences and settings.
 * Implements Parcelable for Android component integration.
 *
 * @property theme User's selected theme preference ("light", "dark", "system")
 * @property notifications User's notification preferences
 */
@Parcelize
data class UserSettings(
    val theme: String,
    val notifications: NotificationSettings
) : Parcelable

/**
 * Thread-safe, immutable data class representing user notification preferences.
 * Implements Parcelable for Android component integration.
 *
 * @property messageDelivery Enable/disable message delivery notifications
 * @property newMessages Enable/disable new message notifications
 * @property statusUpdates Enable/disable contact status update notifications
 */
@Parcelize
data class NotificationSettings(
    val messageDelivery: Boolean,
    val newMessages: Boolean,
    val statusUpdates: Boolean
) : Parcelable {

    companion object {
        /**
         * Default notification settings with all notifications enabled
         */
        fun getDefault() = NotificationSettings(
            messageDelivery = true,
            newMessages = true,
            statusUpdates = true
        )
    }
}