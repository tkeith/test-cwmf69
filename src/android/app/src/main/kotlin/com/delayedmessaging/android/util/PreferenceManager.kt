package com.delayedmessaging.android.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.delayedmessaging.android.util.Constants.SHARED_PREFS
import com.delayedmessaging.android.util.Constants.USER_STATUS
import com.delayedmessaging.android.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for application preferences with encryption support.
 * Handles secure storage of authentication tokens, user preferences, and app settings.
 *
 * @property context Application context for SharedPreferences access
 */
class PreferenceManager(private val context: Context) {

    private val mutex = Mutex()
    private val TAG = "PreferenceManager"
    private val cache = ConcurrentHashMap<String, Any?>()

    // Regular SharedPreferences for non-sensitive data
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "delayed_messaging_prefs",
        Context.MODE_PRIVATE
    )

    // Encrypted SharedPreferences for sensitive data
    private val securePrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        securePrefs = EncryptedSharedPreferences.create(
            context,
            "delayed_messaging_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        Logger.debug(TAG, "PreferenceManager initialized successfully")
    }

    /**
     * Securely stores the authentication token with encryption.
     *
     * @param token JWT token to be stored
     */
    suspend fun saveAuthToken(token: String) = mutex.withLock {
        try {
            securePrefs.edit().apply {
                putString(SHARED_PREFS.PREF_AUTH_TOKEN, token)
                apply()
            }
            cache[SHARED_PREFS.PREF_AUTH_TOKEN] = token
            Logger.debug(TAG, "Auth token saved successfully")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to save auth token", e)
            throw e
        }
    }

    /**
     * Retrieves the stored authentication token.
     *
     * @return Decrypted authentication token or null if not found
     */
    suspend fun getAuthToken(): String? = mutex.withLock {
        try {
            return cache.getOrPut(SHARED_PREFS.PREF_AUTH_TOKEN) {
                securePrefs.getString(SHARED_PREFS.PREF_AUTH_TOKEN, null)
            } as String?
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to retrieve auth token", e)
            return null
        }
    }

    /**
     * Saves user presence status.
     *
     * @param status Current user status
     */
    suspend fun saveUserStatus(status: USER_STATUS) = mutex.withLock {
        try {
            prefs.edit().apply {
                putString(SHARED_PREFS.PREF_USER_STATUS, status.name)
                apply()
            }
            cache[SHARED_PREFS.PREF_USER_STATUS] = status
            Logger.debug(TAG, "User status saved: $status")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to save user status", e)
            throw e
        }
    }

    /**
     * Retrieves current user status.
     *
     * @return Current USER_STATUS or OFFLINE if not found
     */
    suspend fun getUserStatus(): USER_STATUS = mutex.withLock {
        try {
            val cached = cache[SHARED_PREFS.PREF_USER_STATUS] as USER_STATUS?
            if (cached != null) return cached

            val statusString = prefs.getString(SHARED_PREFS.PREF_USER_STATUS, USER_STATUS.OFFLINE.name)
            val status = USER_STATUS.valueOf(statusString!!)
            cache[SHARED_PREFS.PREF_USER_STATUS] = status
            return status
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to retrieve user status", e)
            return USER_STATUS.OFFLINE
        }
    }

    /**
     * Saves user notification preferences.
     *
     * @param enabled Whether notifications are enabled
     */
    suspend fun saveNotificationPreference(enabled: Boolean) = mutex.withLock {
        try {
            prefs.edit().apply {
                putBoolean(SHARED_PREFS.PREF_NOTIFICATION_ENABLED, enabled)
                apply()
            }
            cache[SHARED_PREFS.PREF_NOTIFICATION_ENABLED] = enabled
            Logger.debug(TAG, "Notification preference saved: $enabled")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to save notification preference", e)
            throw e
        }
    }

    /**
     * Retrieves notification preferences.
     *
     * @return Whether notifications are enabled (default: true)
     */
    suspend fun getNotificationPreference(): Boolean = mutex.withLock {
        try {
            return cache.getOrPut(SHARED_PREFS.PREF_NOTIFICATION_ENABLED) {
                prefs.getBoolean(SHARED_PREFS.PREF_NOTIFICATION_ENABLED, true)
            } as Boolean
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to retrieve notification preference", e)
            return true
        }
    }

    /**
     * Saves theme mode preference.
     *
     * @param isDarkMode Whether dark mode is enabled
     */
    suspend fun saveThemeMode(isDarkMode: Boolean) = mutex.withLock {
        try {
            prefs.edit().apply {
                putBoolean(SHARED_PREFS.PREF_THEME_MODE, isDarkMode)
                apply()
            }
            cache[SHARED_PREFS.PREF_THEME_MODE] = isDarkMode
            Logger.debug(TAG, "Theme mode saved: $isDarkMode")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to save theme mode", e)
            throw e
        }
    }

    /**
     * Retrieves theme mode preference.
     *
     * @return Whether dark mode is enabled (default: system default)
     */
    suspend fun getThemeMode(): Boolean = mutex.withLock {
        try {
            return cache.getOrPut(SHARED_PREFS.PREF_THEME_MODE) {
                prefs.getBoolean(SHARED_PREFS.PREF_THEME_MODE, false)
            } as Boolean
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to retrieve theme mode", e)
            return false
        }
    }

    /**
     * Clears all stored preferences and cache.
     * Used during logout or app data clear.
     */
    suspend fun clearAll() = mutex.withLock {
        try {
            prefs.edit().clear().apply()
            securePrefs.edit().clear().apply()
            cache.clear()
            Logger.debug(TAG, "All preferences cleared successfully")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to clear preferences", e)
            throw e
        }
    }

    /**
     * Updates the last sync timestamp.
     *
     * @param timestamp Last successful sync timestamp
     */
    suspend fun updateLastSyncTime(timestamp: Long) = mutex.withLock {
        try {
            prefs.edit().apply {
                putLong(SHARED_PREFS.PREF_LAST_SYNC_TIME, timestamp)
                apply()
            }
            cache[SHARED_PREFS.PREF_LAST_SYNC_TIME] = timestamp
            Logger.debug(TAG, "Last sync time updated: $timestamp")
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to update last sync time", e)
            throw e
        }
    }

    /**
     * Retrieves the last sync timestamp.
     *
     * @return Last sync timestamp or 0 if never synced
     */
    suspend fun getLastSyncTime(): Long = mutex.withLock {
        try {
            return cache.getOrPut(SHARED_PREFS.PREF_LAST_SYNC_TIME) {
                prefs.getLong(SHARED_PREFS.PREF_LAST_SYNC_TIME, 0L)
            } as Long
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to retrieve last sync time", e)
            return 0L
        }
    }

    companion object {
        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context).also { instance = it }
            }
        }
    }
}