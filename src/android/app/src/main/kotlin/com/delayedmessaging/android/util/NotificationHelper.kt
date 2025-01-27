package com.delayedmessaging.android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for managing and displaying notifications in the Delayed Messaging app.
 * Supports Android O+ notification channels, battery optimization, and privacy features.
 * 
 * @property context Application context for accessing system services
 */
@Singleton
class NotificationHelper @Inject constructor(private val context: Context) {

    private val notificationManager: NotificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(Constants.SHARED_PREFS.PREF_NOTIFICATION_ENABLED, Context.MODE_PRIVATE)
    private val workManager: WorkManager = WorkManager.getInstance(context)

    private companion object {
        const val CHANNEL_MESSAGE = "message_notifications"
        const val CHANNEL_DELIVERY = "delivery_notifications"
        const val CHANNEL_PRESENCE = "presence_notifications"
        
        const val NOTIFICATION_GROUP_MESSAGES = "group_messages"
        const val NOTIFICATION_GROUP_DELIVERY = "group_delivery"
        const val NOTIFICATION_GROUP_PRESENCE = "group_presence"
        
        const val AUTO_DISMISS_DELAY = 5L // seconds
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates and configures notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            // Message notifications channel
            NotificationChannel(
                CHANNEL_MESSAGE,
                context.getString(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_messages_description)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes)
                enableVibration(true)
                enableLights(true)
                notificationManager.createNotificationChannel(this)
            }

            // Delivery status channel
            NotificationChannel(
                CHANNEL_DELIVERY,
                context.getString(R.string.channel_delivery_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.channel_delivery_description)
                setSound(null, null)
                enableVibration(false)
                notificationManager.createNotificationChannel(this)
            }

            // Presence updates channel
            NotificationChannel(
                CHANNEL_PRESENCE,
                context.getString(R.string.channel_presence_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.channel_presence_description)
                setSound(null, null)
                enableVibration(false)
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    /**
     * Displays a notification for new incoming messages
     * 
     * @param messageId Unique identifier for the message
     * @param sender Name of the message sender
     * @param content Message content
     * @param isPrivate Whether to hide message content in notification
     */
    fun showMessageNotification(
        messageId: String,
        sender: String,
        content: String,
        isPrivate: Boolean = false
    ) {
        if (!preferences.getBoolean(Constants.SHARED_PREFS.PREF_NOTIFICATION_ENABLED, true)) {
            return
        }

        val viewIntent = PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            // Create intent to open message detail activity
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("message_id", messageId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGE)
            .setSmallIcon(R.drawable.ic_notification_message)
            .setContentTitle(sender)
            .setContentText(if (isPrivate) context.getString(R.string.notification_private_message) else content)
            .setAutoCancel(true)
            .setContentIntent(viewIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(NOTIFICATION_GROUP_MESSAGES)
            .build()

        notificationManager.notify(messageId.hashCode(), notification)
    }

    /**
     * Displays a notification for message delivery status updates
     * 
     * @param messageId Unique identifier for the message
     * @param status Current status of the message
     */
    fun showDeliveryNotification(messageId: String, status: Constants.MESSAGE_STATUS) {
        if (!preferences.getBoolean(Constants.SHARED_PREFS.PREF_NOTIFICATION_ENABLED, true)) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DELIVERY)
            .setSmallIcon(getStatusIcon(status))
            .setContentTitle(context.getString(R.string.notification_delivery_title))
            .setContentText(getStatusMessage(status))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setGroup(NOTIFICATION_GROUP_DELIVERY)
            .build()

        notificationManager.notify(messageId.hashCode(), notification)

        if (status == Constants.MESSAGE_STATUS.DELIVERED) {
            scheduleNotificationDismissal(messageId.hashCode())
        }
    }

    /**
     * Displays a notification for user presence updates
     * 
     * @param userId Unique identifier for the user
     * @param username Name of the user
     * @param status Current presence status of the user
     */
    fun showPresenceNotification(
        userId: String,
        username: String,
        status: Constants.USER_STATUS
    ) {
        if (!preferences.getBoolean(Constants.SHARED_PREFS.PREF_NOTIFICATION_ENABLED, true)) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PRESENCE)
            .setSmallIcon(getPresenceIcon(status))
            .setContentTitle(username)
            .setContentText(getPresenceMessage(status))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(NOTIFICATION_GROUP_PRESENCE)
            .build()

        notificationManager.notify(userId.hashCode(), notification)
    }

    /**
     * Cancels a specific notification and its related resources
     * 
     * @param notificationId ID of the notification to cancel
     */
    fun cancelNotification(notificationId: String) {
        notificationManager.cancel(notificationId.hashCode())
        workManager.cancelAllWorkByTag("notification_$notificationId")
    }

    private fun scheduleNotificationDismissal(notificationId: Int) {
        val dismissWorkRequest = OneTimeWorkRequestBuilder<NotificationDismissWorker>()
            .setInitialDelay(AUTO_DISMISS_DELAY, TimeUnit.SECONDS)
            .setInputData(workDataOf("notification_id" to notificationId))
            .addTag("notification_$notificationId")
            .build()

        workManager.enqueue(dismissWorkRequest)
    }

    private fun getStatusIcon(status: Constants.MESSAGE_STATUS): Int = when (status) {
        Constants.MESSAGE_STATUS.DELIVERED -> R.drawable.ic_status_delivered
        Constants.MESSAGE_STATUS.SEEN -> R.drawable.ic_status_seen
        Constants.MESSAGE_STATUS.FAILED -> R.drawable.ic_status_failed
        else -> R.drawable.ic_status_pending
    }

    private fun getPresenceIcon(status: Constants.USER_STATUS): Int = when (status) {
        Constants.USER_STATUS.ONLINE -> R.drawable.ic_status_online
        Constants.USER_STATUS.AWAY -> R.drawable.ic_status_away
        Constants.USER_STATUS.DO_NOT_DISTURB -> R.drawable.ic_status_dnd
        else -> R.drawable.ic_status_offline
    }

    private fun getStatusMessage(status: Constants.MESSAGE_STATUS): String = when (status) {
        Constants.MESSAGE_STATUS.DELIVERED -> context.getString(R.string.status_delivered)
        Constants.MESSAGE_STATUS.SEEN -> context.getString(R.string.status_seen)
        Constants.MESSAGE_STATUS.FAILED -> context.getString(R.string.status_failed)
        else -> context.getString(R.string.status_pending)
    }

    private fun getPresenceMessage(status: Constants.USER_STATUS): String = when (status) {
        Constants.USER_STATUS.ONLINE -> context.getString(R.string.presence_online)
        Constants.USER_STATUS.AWAY -> context.getString(R.string.presence_away)
        Constants.USER_STATUS.DO_NOT_DISTURB -> context.getString(R.string.presence_dnd)
        else -> context.getString(R.string.presence_offline)
    }
}