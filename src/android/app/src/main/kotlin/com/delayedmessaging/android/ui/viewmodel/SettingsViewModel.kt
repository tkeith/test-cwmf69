package com.delayedmessaging.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delayedmessaging.android.util.Constants.USER_STATUS
import com.delayedmessaging.android.util.Logger
import com.delayedmessaging.android.util.NotificationHelper
import com.delayedmessaging.android.util.PreferenceManager
import com.delayedmessaging.android.util.ThemeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing user settings and preferences in the Delayed Messaging app.
 * Handles theme mode, notifications, user presence, and notification type preferences.
 *
 * @property themeManager Manages application theme settings
 * @property preferenceManager Manages user preferences storage
 * @property notificationHelper Handles notification channel management
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val preferenceManager: PreferenceManager,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val TAG = "SettingsViewModel"

    // State flows for reactive UI updates
    private val _isDarkMode = MutableStateFlow(false)
    private val _notificationsEnabled = MutableStateFlow(true)
    private val _userPresenceStatus = MutableStateFlow(USER_STATUS.ONLINE)
    private val _notificationTypes = MutableStateFlow(setOf(
        NotificationType.MESSAGE_DELIVERY,
        NotificationType.NEW_MESSAGES,
        NotificationType.STATUS_UPDATES
    ))

    // Public immutable state flows
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    val userPresenceStatus: StateFlow<USER_STATUS> = _userPresenceStatus.asStateFlow()
    val notificationTypes: StateFlow<Set<NotificationType>> = _notificationTypes.asStateFlow()

    init {
        Logger.debug(TAG, "Initializing SettingsViewModel")
        initializeSettings()
    }

    /**
     * Initializes settings state from persistent storage
     */
    private fun initializeSettings() {
        viewModelScope.launch {
            try {
                // Initialize theme mode
                _isDarkMode.value = themeManager.isDarkMode()

                // Initialize notifications state
                _notificationsEnabled.value = preferenceManager.getNotificationPreference()

                // Initialize user presence
                _userPresenceStatus.value = preferenceManager.getUserStatus()

                // Initialize notification types
                _notificationTypes.value = preferenceManager.getNotificationTypes()

                Logger.debug(TAG, "Settings initialized successfully")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to initialize settings", e)
            }
        }
    }

    /**
     * Updates the application theme mode
     *
     * @param enabled True for dark mode, false for light mode
     */
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                themeManager.setThemeMode(enabled)
                _isDarkMode.value = enabled
                Logger.debug(TAG, "Theme mode updated: isDarkMode=$enabled")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to update theme mode", e)
            }
        }
    }

    /**
     * Updates notification settings
     *
     * @param enabled True to enable notifications, false to disable
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferenceManager.saveNotificationPreference(enabled)
                _notificationsEnabled.value = enabled
                
                if (enabled) {
                    notificationHelper.createNotificationChannels()
                }
                
                Logger.debug(TAG, "Notifications ${if (enabled) "enabled" else "disabled"}")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to update notification settings", e)
            }
        }
    }

    /**
     * Updates user presence status
     *
     * @param status New user status
     */
    fun setUserPresence(status: USER_STATUS) {
        viewModelScope.launch {
            try {
                // Validate status transition
                val currentStatus = _userPresenceStatus.value
                if (!isValidStatusTransition(currentStatus, status)) {
                    Logger.warn(TAG, "Invalid status transition from $currentStatus to $status")
                    return@launch
                }

                preferenceManager.saveUserStatus(status)
                _userPresenceStatus.value = status
                Logger.debug(TAG, "User presence updated: $status")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to update user presence", e)
            }
        }
    }

    /**
     * Toggles specific notification type preference
     *
     * @param type Notification type to toggle
     */
    fun toggleNotificationType(type: NotificationType) {
        viewModelScope.launch {
            try {
                val currentTypes = _notificationTypes.value.toMutableSet()
                if (currentTypes.contains(type)) {
                    currentTypes.remove(type)
                } else {
                    currentTypes.add(type)
                }
                
                preferenceManager.setNotificationTypes(currentTypes)
                _notificationTypes.value = currentTypes
                
                // Update notification channels based on new preferences
                notificationHelper.updateNotificationChannels()
                
                Logger.debug(TAG, "Notification type ${type.name} toggled")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to toggle notification type", e)
            }
        }
    }

    /**
     * Validates whether a status transition is allowed
     *
     * @param currentStatus Current user status
     * @param newStatus Requested new status
     * @return True if transition is valid, false otherwise
     */
    private fun isValidStatusTransition(currentStatus: USER_STATUS, newStatus: USER_STATUS): Boolean {
        return when (currentStatus) {
            USER_STATUS.OFFLINE -> newStatus == USER_STATUS.ONLINE
            USER_STATUS.ONLINE -> true // Can transition to any state from ONLINE
            USER_STATUS.AWAY -> newStatus != USER_STATUS.OFFLINE
            USER_STATUS.DO_NOT_DISTURB -> newStatus != USER_STATUS.OFFLINE
            else -> false
        }
    }

    /**
     * Enum defining supported notification types
     */
    enum class NotificationType {
        MESSAGE_DELIVERY,
        NEW_MESSAGES,
        STATUS_UPDATES
    }

    override fun onCleared() {
        super.onCleared()
        Logger.debug(TAG, "SettingsViewModel cleared")
    }
}