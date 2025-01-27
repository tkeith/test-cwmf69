package com.delayedmessaging.android.ui.common

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.delayedmessaging.android.util.DateTimeUtils
import com.delayedmessaging.android.R

/**
 * A custom view component that displays a countdown timer with Material Design 3 styling
 * for the message delivery delay feature. Implements precise timing and accessibility support.
 *
 * @property context The view context
 * @property attrs Optional view attributes
 * @property defStyleAttr Default style attribute
 */
class DelayTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val progressBar: LinearProgressIndicator
    private val timerText: TextView
    private val updateHandler: Handler = Handler(Looper.getMainLooper())
    private var deliveryTimeMillis: Long = 0L
    private var isRunning: Boolean = false

    companion object {
        private const val UPDATE_INTERVAL_MS = 100L // Update every 100ms for smooth animation
        private const val MAX_PROGRESS = 100
        private const val KEY_DELIVERY_TIME = "delivery_time"
        private const val KEY_IS_RUNNING = "is_running"
        private const val PROGRESS_ANIMATION_DURATION = 100L
    }

    init {
        orientation = VERTICAL

        // Inflate layout with Material Design 3 components
        LayoutInflater.from(context).inflate(R.layout.view_delay_timer, this, true)

        // Initialize progress bar with Material Design 3 styling
        progressBar = findViewById<LinearProgressIndicator>(R.id.progress_bar).apply {
            max = MAX_PROGRESS
            progress = 0
            isIndeterminate = false
            setIndicatorColor(context.getColor(R.color.material_primary))
            trackColor = context.getColor(R.color.material_surface_variant)
            trackThickness = resources.getDimensionPixelSize(R.dimen.progress_track_thickness)
            trackCornerRadius = resources.getDimensionPixelSize(R.dimen.progress_corner_radius)
            setAnimationDuration(PROGRESS_ANIMATION_DURATION)
        }

        // Initialize timer text with Material Design 3 typography
        timerText = findViewById<TextView>(R.id.timer_text).apply {
            setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
        }

        // Configure accessibility
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(R.string.delay_timer_description)
    }

    /**
     * Starts the countdown timer for message delivery.
     *
     * @param deliveryTime The scheduled message delivery time in milliseconds
     */
    fun startTimer(deliveryTime: Long) {
        deliveryTimeMillis = deliveryTime
        isRunning = true

        // Reset and show progress
        progressBar.progress = 0
        visibility = View.VISIBLE

        // Start periodic updates
        updateProgress()

        // Announce timer start for accessibility
        announceForAccessibility(context.getString(R.string.delay_timer_started))
    }

    /**
     * Stops the countdown timer and cleans up resources.
     */
    fun stopTimer() {
        isRunning = false
        updateHandler.removeCallbacksAndMessages(null)
        progressBar.progress = 0
        visibility = View.GONE

        // Announce timer stop for accessibility
        announceForAccessibility(context.getString(R.string.delay_timer_stopped))
    }

    /**
     * Updates the progress bar and timer text with precise timing.
     */
    private fun updateProgress() {
        if (!isRunning) return

        val remainingTime = DateTimeUtils.getRemainingDeliveryTime(deliveryTimeMillis)
        
        if (remainingTime <= 0) {
            stopTimer()
            return
        }

        // Calculate progress percentage
        val totalDelay = 60_000L // 60 seconds in milliseconds
        val progress = ((totalDelay - remainingTime) * MAX_PROGRESS / totalDelay).toInt()
        
        // Update UI with smooth animation
        progressBar.setProgressCompat(progress, true)
        
        // Update timer text
        val remainingSeconds = (remainingTime / 1000L) + 1
        val timerString = context.getString(R.string.delay_timer_seconds, remainingSeconds)
        timerText.text = timerString

        // Schedule next update
        updateHandler.postDelayed({ updateProgress() }, UPDATE_INTERVAL_MS)

        // Update accessibility if seconds changed
        if (remainingTime % 1000L < UPDATE_INTERVAL_MS) {
            announceForAccessibility(timerString)
        }
    }

    /**
     * Save instance state for configuration changes.
     */
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return Bundle().apply {
            putParcelable("super_state", superState)
            putLong(KEY_DELIVERY_TIME, deliveryTimeMillis)
            putBoolean(KEY_IS_RUNNING, isRunning)
        }
    }

    /**
     * Restore instance state after configuration changes.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val superState = state.getParcelable<Parcelable>("super_state")
            super.onRestoreInstanceState(superState)
            
            val savedDeliveryTime = state.getLong(KEY_DELIVERY_TIME)
            val wasRunning = state.getBoolean(KEY_IS_RUNNING)
            
            if (wasRunning && savedDeliveryTime > 0) {
                startTimer(savedDeliveryTime)
            }
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    /**
     * Clean up resources when view is detached.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTimer()
    }
}