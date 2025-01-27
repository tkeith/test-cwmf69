package com.delayedmessaging.android.util

import android.view.View
import android.widget.TextView
import java.util.concurrent.TimeUnit
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.util.Constants.MESSAGE_CONFIG
import com.delayedmessaging.android.util.Constants.USER_STATUS
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat

/**
 * Thread-safe extension function to format message delivery time remaining
 * with precise second accuracy (±1 second as per requirements).
 *
 * @return Localized, accessibility-friendly time string
 */
@Synchronized
fun Message.formatDeliveryTime(): String {
    val remainingMs = getRemainingDeliveryTime()
    if (isDelivered() || remainingMs <= 0) {
        return "Delivered"
    }

    val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs)
    return when {
        seconds < MESSAGE_CONFIG.MIN_DELAY_SECONDS -> {
            "Less than ${MESSAGE_CONFIG.MIN_DELAY_SECONDS} seconds remaining"
        }
        seconds > MESSAGE_CONFIG.MAX_DELAY_SECONDS -> {
            "About ${MESSAGE_CONFIG.MAX_DELAY_SECONDS} seconds remaining"
        }
        else -> "$seconds seconds remaining"
    }.also { formattedTime ->
        // Ensure screen readers announce time updates
        ViewCompat.setAccessibilityDelegate(TextView(null), object : ViewCompat.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.contentDescription = "Message delivery: $formattedTime"
            }
        })
    }
}

/**
 * Thread-safe extension function to format user's last active time
 * with proper time zone handling and localization.
 *
 * @return Localized status string with proper time formatting
 */
@Synchronized
fun UserPresence.formatLastActive(): String {
    if (isOnline()) {
        return "Online"
    }

    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    
    val lastActiveInstant = Instant.ofEpochMilli(lastActive)
    val now = Instant.now()
    val minutesAgo = ChronoUnit.MINUTES.between(lastActiveInstant, now)

    return when {
        minutesAgo < 1 -> "Just now"
        minutesAgo < 60 -> "$minutesAgo minutes ago"
        minutesAgo < 1440 -> "Last seen at ${formatter.format(lastActiveInstant)}"
        else -> "Last seen ${ChronoUnit.DAYS.between(lastActiveInstant, now)} days ago"
    }
}

/**
 * Material Design 3 compliant extension function to hide a view
 * with proper cleanup and accessibility handling.
 */
fun View.setVisibilityGone() {
    animate().cancel()
    visibility = View.GONE
    clearFocus()
    announceForAccessibility("Hidden")
}

/**
 * Material Design 3 compliant extension function to show a view
 * with proper initialization and accessibility handling.
 */
fun View.setVisibilityVisible() {
    visibility = View.VISIBLE
    announceForAccessibility("Visible")
    requestLayout()
}

/**
 * Material Design 3 compliant extension function to set text on a TextView
 * or hide it if the text is null/empty, with proper accessibility handling.
 *
 * @param text The text to display or null to hide the view
 */
fun TextView.setTextOrHide(text: String?) {
    if (text.isNullOrEmpty()) {
        setVisibilityGone()
        return
    }

    setVisibilityVisible()
    this.text = text
    
    // Ensure proper text scaling for accessibility
    setTextIsSelectable(true)
    ViewCompat.setAccessibilityDelegate(this, object : ViewCompat.AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.isSelected = true
            info.text = text
        }
    })
}