package com.delayedmessaging.android.data.api

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.socket.client.IO // version: 2.1.0
import io.socket.client.Socket // version: 2.1.0
import io.socket.emitter.Emitter // version: 2.1.0
import kotlinx.coroutines.* // version: 1.7.0
import org.json.JSONObject
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.util.Constants.API_CONFIG
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import com.delayedmessaging.android.util.Constants.USER_STATUS
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages WebSocket connections and real-time communication for the Android client.
 * Implements automatic reconnection, presence monitoring, and message delivery tracking.
 */
class WebSocketManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val retryPolicy: RetryPolicy
) : DefaultLifecycleObserver {

    private var socket: Socket? = null
    private val listeners = CopyOnWriteArrayList<WebSocketListener>()
    private val isConnected = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private var authToken: String? = null
    private val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val wakeLock: PowerManager.WakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "DelayedMessaging:WebSocketManager"
    )

    /**
     * Interface for WebSocket event listeners
     */
    interface WebSocketListener {
        fun onConnected()
        fun onDisconnected()
        fun onMessageReceived(message: Message)
        fun onMessageStatusChanged(messageId: String, status: MessageStatus)
        fun onPresenceUpdated(presence: UserPresence)
        fun onError(error: WebSocketError)
    }

    /**
     * Establishes WebSocket connection with authentication
     */
    suspend fun connect(token: String) = withContext(Dispatchers.IO) {
        try {
            authToken = token
            val options = IO.Options().apply {
                forceNew = true
                timeout = API_CONFIG.CONNECT_TIMEOUT * 1000
                reconnection = true
                reconnectionAttempts = retryPolicy.maxAttempts
                reconnectionDelay = retryPolicy.initialDelayMs
                reconnectionDelayMax = retryPolicy.maxDelayMs
                query = "auth_token=$token"
            }

            socket = IO.socket(API_CONFIG.SOCKET_URL, options).apply {
                on(Socket.EVENT_CONNECT) { onSocketConnected() }
                on(Socket.EVENT_DISCONNECT) { onSocketDisconnected() }
                on(Socket.EVENT_ERROR) { error -> onSocketError(error[0]) }
                on("message:received") { args -> onMessageReceived(args[0] as JSONObject) }
                on("message:status") { args -> onMessageStatusUpdate(args[0] as JSONObject) }
                on("presence:update") { args -> onPresenceUpdate(args[0] as JSONObject) }
                connect()
            }

            startHeartbeat()
        } catch (e: Exception) {
            handleConnectionError(e)
        }
    }

    /**
     * Sends a message through WebSocket with delivery tracking
     */
    suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            throw WebSocketError.NotConnected
        }

        try {
            val messageJson = JSONObject().apply {
                put("id", message.id)
                put("content", message.content)
                put("recipientId", message.recipientId)
                put("scheduledFor", message.scheduledFor)
            }

            socket?.emit("message:send", messageJson) { ack ->
                handleMessageAcknowledgment(message.id, ack)
            }

            startDeliveryTracking(message.id)
        } catch (e: Exception) {
            handleMessageError(message.id, e)
        }
    }

    /**
     * Updates user presence status
     */
    suspend fun updatePresence(status: USER_STATUS) = withContext(Dispatchers.IO) {
        val presenceJson = JSONObject().apply {
            put("status", status.name)
            put("lastActive", System.currentTimeMillis())
        }
        socket?.emit("presence:update", presenceJson)
    }

    /**
     * Disconnects WebSocket connection
     */
    fun disconnect() {
        socket?.disconnect()
        wakeLock.release()
        scope.cancel()
    }

    /**
     * Adds WebSocket event listener
     */
    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    /**
     * Removes WebSocket event listener
     */
    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    private fun onSocketConnected() {
        isConnected.set(true)
        reconnectAttempts.set(0)
        listeners.forEach { it.onConnected() }
    }

    private fun onSocketDisconnected() {
        isConnected.set(false)
        listeners.forEach { it.onDisconnected() }
        if (reconnectAttempts.incrementAndGet() <= retryPolicy.maxAttempts) {
            scope.launch {
                delay(retryPolicy.getNextDelay(reconnectAttempts.get()))
                connect(authToken ?: return@launch)
            }
        }
    }

    private fun onSocketError(error: Any) {
        val webSocketError = when (error) {
            is String -> WebSocketError.ConnectionError(error)
            else -> WebSocketError.Unknown
        }
        listeners.forEach { it.onError(webSocketError) }
    }

    private fun onMessageReceived(messageJson: JSONObject) {
        try {
            val message = Message(
                id = messageJson.getString("id"),
                content = messageJson.getString("content"),
                senderId = messageJson.getString("senderId"),
                recipientId = messageJson.getString("recipientId"),
                status = MessageStatus.valueOf(messageJson.getString("status")),
                createdAt = messageJson.getLong("createdAt"),
                scheduledFor = messageJson.getLong("scheduledFor"),
                deliveredAt = messageJson.optLong("deliveredAt")
            )
            listeners.forEach { it.onMessageReceived(message) }
        } catch (e: Exception) {
            listeners.forEach { it.onError(WebSocketError.MessageParsingError(e.message)) }
        }
    }

    private fun onMessageStatusUpdate(statusJson: JSONObject) {
        try {
            val messageId = statusJson.getString("messageId")
            val status = MessageStatus.valueOf(statusJson.getString("status"))
            listeners.forEach { it.onMessageStatusChanged(messageId, status) }
        } catch (e: Exception) {
            listeners.forEach { it.onError(WebSocketError.StatusUpdateError(e.message)) }
        }
    }

    private fun onPresenceUpdate(presenceJson: JSONObject) {
        try {
            val presence = UserPresence(
                userId = presenceJson.getString("userId"),
                status = USER_STATUS.valueOf(presenceJson.getString("status")),
                lastActive = presenceJson.getLong("lastActive"),
                deviceInfo = parseDeviceInfo(presenceJson.getJSONObject("deviceInfo"))
            )
            listeners.forEach { it.onPresenceUpdated(presence) }
        } catch (e: Exception) {
            listeners.forEach { it.onError(WebSocketError.PresenceUpdateError(e.message)) }
        }
    }

    private fun startHeartbeat() {
        scope.launch {
            while (isActive) {
                if (isConnected.get()) {
                    socket?.emit("heartbeat")
                }
                delay(MESSAGE_CONFIG.STATUS_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun startDeliveryTracking(messageId: String) {
        scope.launch {
            delay(MESSAGE_CONFIG.DELIVERY_TIMEOUT_SECONDS * 1000)
            if (isActive) {
                listeners.forEach { 
                    it.onMessageStatusChanged(messageId, MessageStatus.FAILED)
                }
            }
        }
    }

    private fun handleMessageAcknowledgment(messageId: String, ack: Array<Any>) {
        val success = ack.firstOrNull() as? Boolean ?: false
        if (!success) {
            listeners.forEach { 
                it.onError(WebSocketError.MessageNotAcknowledged(messageId))
            }
        }
    }

    private fun handleConnectionError(error: Exception) {
        listeners.forEach { 
            it.onError(WebSocketError.ConnectionError(error.message))
        }
    }

    private fun handleMessageError(messageId: String, error: Exception) {
        listeners.forEach { 
            it.onError(WebSocketError.MessageSendError(messageId, error.message))
        }
    }

    sealed class WebSocketError : Exception() {
        object NotConnected : WebSocketError()
        object Unknown : WebSocketError()
        data class ConnectionError(val reason: String?) : WebSocketError()
        data class MessageSendError(val messageId: String, val reason: String?) : WebSocketError()
        data class MessageNotAcknowledged(val messageId: String) : WebSocketError()
        data class MessageParsingError(val reason: String?) : WebSocketError()
        data class StatusUpdateError(val reason: String?) : WebSocketError()
        data class PresenceUpdateError(val reason: String?) : WebSocketError()
    }

    data class RetryPolicy(
        val maxAttempts: Int = 5,
        val initialDelayMs: Long = 1000,
        val maxDelayMs: Long = 30000,
        val multiplier: Float = 1.5f
    ) {
        fun getNextDelay(attempt: Int): Long {
            val delay = (initialDelayMs * multiplier.pow(attempt - 1)).toLong()
            return delay.coerceAtMost(maxDelayMs)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        updatePresence(USER_STATUS.AWAY)
    }

    override fun onResume(owner: LifecycleOwner) {
        updatePresence(USER_STATUS.ONLINE)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disconnect()
    }
}