package com.delayedmessaging.android.service

import android.os.PowerManager
import android.content.Context
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.delayedmessaging.android.util.NotificationHelper
import com.delayedmessaging.android.util.Constants
import android.util.Log
import javax.inject.Inject
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Firebase Cloud Messaging service implementation for handling push notifications
 * in the Delayed Messaging Android app.
 *
 * Supports message delivery, status updates, and presence notifications with
 * proper wake lock management and retry mechanisms.
 */
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val retryCounter = AtomicInteger(0)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val TAG = "FCMService"
        private const val WAKE_LOCK_TIMEOUT = 60L // seconds
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val TYPE_MESSAGE = "message"
        private const val TYPE_DELIVERY = "delivery"
        private const val TYPE_PRESENCE = "presence"
    }

    override fun onCreate() {
        super.onCreate()
        initializeWakeLock()
    }

    private fun initializeWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "DelayedMessaging:FCMWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    /**
     * Handles new FCM registration token with retry mechanism for server updates
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Received new FCM token")
        
        scope.launch {
            try {
                updateTokenWithRetry(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update FCM token after all retries", e)
            }
        }
    }

    private suspend fun updateTokenWithRetry(token: String) {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                // Store token in SharedPreferences
                getSharedPreferences(Constants.SHARED_PREFS.PREF_DEVICE_TOKEN, Context.MODE_PRIVATE)
                    .edit()
                    .putString(Constants.SHARED_PREFS.PREF_DEVICE_TOKEN, token)
                    .apply()

                // TODO: Update token on backend server
                // Implementation will depend on your API client setup

                return // Success, exit retry loop
            } catch (e: Exception) {
                Log.w(TAG, "Token update attempt ${attempt + 1} failed", e)
                delay((2.0.pow(attempt) * 1000).toLong()) // Exponential backoff
            }
        }
        throw Exception("Failed to update token after $MAX_RETRY_ATTEMPTS attempts")
    }

    /**
     * Processes incoming FCM messages with type-safe handling and wake lock management
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Received FCM message from: ${remoteMessage.from}")

        wakeLock?.acquire(WAKE_LOCK_TIMEOUT.seconds.inWholeMilliseconds)

        try {
            when (remoteMessage.data["type"]) {
                TYPE_MESSAGE -> handleMessageNotification(remoteMessage.data)
                TYPE_DELIVERY -> handleDeliveryNotification(remoteMessage.data)
                TYPE_PRESENCE -> handlePresenceNotification(remoteMessage.data)
                else -> Log.w(TAG, "Unknown notification type received")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing FCM message", e)
        } finally {
            wakeLock?.release()
        }
    }

    /**
     * Processes new message notifications with privacy considerations
     */
    private fun handleMessageNotification(data: Map<String, String>) {
        val messageId = data["message_id"] ?: return
        val sender = data["sender"] ?: return
        val content = data["content"] ?: return
        
        val isPrivate = getSharedPreferences(
            Constants.SHARED_PREFS.PREF_NOTIFICATION_ENABLED,
            Context.MODE_PRIVATE
        ).getBoolean("private_notifications", false)

        notificationHelper.showMessageNotification(
            messageId = messageId,
            sender = sender,
            content = content,
            isPrivate = isPrivate
        )
    }

    /**
     * Processes message delivery status notifications with proper status mapping
     */
    private fun handleDeliveryNotification(data: Map<String, String>) {
        val messageId = data["message_id"] ?: return
        val statusString = data["status"] ?: return

        val status = try {
            Constants.MESSAGE_STATUS.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid message status received: $statusString")
            return
        }

        notificationHelper.showDeliveryNotification(
            messageId = messageId,
            status = status
        )
    }

    /**
     * Processes user presence update notifications with status tracking
     */
    private fun handlePresenceNotification(data: Map<String, String>) {
        val userId = data["user_id"] ?: return
        val username = data["username"] ?: return
        val statusString = data["status"] ?: return

        val status = try {
            Constants.USER_STATUS.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid user status received: $statusString")
            return
        }

        notificationHelper.showPresenceNotification(
            userId = userId,
            username = username,
            status = status
        )
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.release()
        super.onDestroy()
    }
}