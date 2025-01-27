package com.delayedmessaging.android.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.delayedmessaging.android.util.Constants.USER_STATUS

/**
 * Thread-safe, immutable data class representing user presence information
 * for real-time status tracking in the Android application.
 *
 * @property userId Unique identifier of the user
 * @property status Current presence status of the user
 * @property lastActive Timestamp of the user's last activity (Unix timestamp in milliseconds)
 * @property deviceInfo Device-specific information for presence tracking
 */
@Parcelize
data class UserPresence(
    val userId: String,
    val status: USER_STATUS,
    val lastActive: Long,
    val deviceInfo: DeviceInfo
) : Parcelable {

    /**
     * Thread-safe function to check if user is currently online.
     *
     * @return true if user status is ONLINE, false otherwise
     */
    fun isOnline(): Boolean = status == USER_STATUS.ONLINE

    /**
     * Thread-safe function to check if user is in away state.
     *
     * @return true if user status is AWAY, false otherwise
     */
    fun isAway(): Boolean = status == USER_STATUS.AWAY

    /**
     * Thread-safe function to check if user is in do not disturb state.
     *
     * @return true if user status is DO_NOT_DISTURB, false otherwise
     */
    fun isDoNotDisturb(): Boolean = status == USER_STATUS.DO_NOT_DISTURB
}

/**
 * Thread-safe, immutable data class containing minimal required device-specific
 * information for presence tracking.
 *
 * @property type Device type identifier (e.g., "smartphone", "tablet")
 * @property platform Platform identifier (e.g., "Android")
 * @property clientVersion Version of the client application
 */
@Parcelize
data class DeviceInfo(
    val type: String,
    val platform: String,
    val clientVersion: String
) : Parcelable