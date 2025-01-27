package com.delayedmessaging.android.util

/**
 * Global constants used throughout the Delayed Messaging Android application.
 * Contains configurations for API endpoints, message handling, authentication,
 * and UI-related settings.
 */
object Constants {

    /**
     * API and networking configuration constants
     */
    object API_CONFIG {
        const val BASE_URL = "https://api.delayedmessaging.com"
        const val API_VERSION = "v1"
        const val SOCKET_URL = "wss://ws.delayedmessaging.com"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
        const val RETRY_COUNT = 3
        const val RETRY_DELAY_MS = 5000L
    }

    /**
     * Message handling and delivery configuration constants
     */
    object MESSAGE_CONFIG {
        const val MIN_DELAY_SECONDS = 30L
        const val MAX_DELAY_SECONDS = 60L
        const val MAX_MESSAGE_LENGTH = 1000
        const val MESSAGE_RETRY_ATTEMPTS = 3
        const val QUEUE_TIMEOUT_SECONDS = 300L
        const val DELIVERY_TIMEOUT_SECONDS = 120L
        const val STATUS_CHECK_INTERVAL_MS = 1000L
        const val BATCH_SIZE = 50
    }

    /**
     * Authentication and security configuration constants
     */
    object AUTH_CONFIG {
        const val TOKEN_EXPIRY_HOURS = 24L
        const val REFRESH_TOKEN_EXPIRY_DAYS = 30L
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 64
        const val PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
        const val AUTH_HEADER = "Authorization"
        const val TOKEN_PREFIX = "Bearer "
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }

    /**
     * Enum defining all possible message states throughout the message lifecycle
     */
    enum class MESSAGE_STATUS(val description: String) {
        DRAFT("Initial message state before sending"),
        VALIDATING("Message under validation checks"),
        QUEUED("Message queued for delayed delivery"),
        SCHEDULED("Message scheduled for delivery"),
        SENDING("Message in delivery process"),
        DELIVERED("Message successfully delivered"),
        SEEN("Message viewed by recipient"),
        FAILED("Message delivery failed"),
        EXPIRED("Message expired before delivery")
    }

    /**
     * Enum defining user presence states
     */
    enum class USER_STATUS(val description: String) {
        ONLINE("User is active and can receive messages"),
        AWAY("User is temporarily inactive"),
        DO_NOT_DISTURB("User has disabled notifications"),
        OFFLINE("User is disconnected"),
        TYPING("User is composing a message"),
        INACTIVE("User has been inactive for threshold period")
    }

    /**
     * UI-related configuration constants
     */
    object UI_CONFIG {
        const val ANIMATION_DURATION_MS = 300L
        const val TOAST_DURATION_SHORT_MS = 2000L
        const val TOAST_DURATION_LONG_MS = 3500L
        const val DELAY_TIMER_UPDATE_INTERVAL_MS = 1000L
        const val MAX_USERNAME_LENGTH = 30
        const val MIN_USERNAME_LENGTH = 3
        const val TYPING_INDICATOR_TIMEOUT_MS = 5000L
        const val PRESENCE_UPDATE_INTERVAL_MS = 60000L
        const val LIST_PAGE_SIZE = 20
        const val MAX_RETRY_BUTTON_CLICKS = 3
    }

    /**
     * SharedPreferences keys for persistent storage
     */
    object SHARED_PREFS {
        const val PREF_AUTH_TOKEN = "auth_token"
        const val PREF_REFRESH_TOKEN = "refresh_token"
        const val PREF_USER_ID = "user_id"
        const val PREF_USER_STATUS = "user_status"
        const val PREF_THEME_MODE = "theme_mode"
        const val PREF_NOTIFICATION_ENABLED = "notifications_enabled"
        const val PREF_LAST_SYNC_TIME = "last_sync_timestamp"
        const val PREF_DEVICE_TOKEN = "device_token"
        const val PREF_APP_VERSION = "app_version"
        const val PREF_FIRST_LAUNCH = "is_first_launch"
    }
}