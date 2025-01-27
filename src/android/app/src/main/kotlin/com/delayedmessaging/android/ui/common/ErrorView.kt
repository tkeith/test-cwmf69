package com.delayedmessaging.android.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.delayedmessaging.android.util.Logger

/**
 * A custom view component that displays error states with Material Design 3 styling.
 * Provides configurable error messages and retry functionality with comprehensive accessibility support.
 *
 * @property context The view's context
 * @property attrs Optional view attributes
 * @property defStyleAttr Default style attributes
 */
class ErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : MaterialCardView(context, attrs, defStyleAttr) {

    private companion object {
        private const val TAG = "ErrorView"
        private const val ANIMATION_DURATION = 300L
        private const val MIN_TOUCH_TARGET_SIZE_DP = 48
    }

    private val errorMessageView: TextView
    private val retryButton: MaterialButton
    private val errorContainer: MaterialCardView

    init {
        Logger.debug(TAG, "Initializing ErrorView")
        
        // Inflate the error view layout
        LayoutInflater.from(context).inflate(
            com.delayedmessaging.android.R.layout.view_error,
            this,
            true
        )

        // Initialize view references
        errorMessageView = findViewById(com.delayedmessaging.android.R.id.textViewError)
        retryButton = findViewById(com.delayedmessaging.android.R.id.buttonRetry)
        errorContainer = findViewById(com.delayedmessaging.android.R.id.cardViewError)

        // Apply Material Design 3 styling
        setupMaterialStyling()
        
        // Configure accessibility
        setupAccessibility()

        // Set initial visibility
        visibility = View.GONE
    }

    /**
     * Sets the error message to be displayed.
     *
     * @param message The error message text
     */
    fun setErrorMessage(message: String) {
        Logger.debug(TAG, "Setting error message: $message")
        
        errorMessageView.text = message
        
        // Update accessibility
        ViewCompat.setAccessibilityDelegate(errorMessageView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = "Error message"
                info.text = message
            }
        })
    }

    /**
     * Sets the click listener for the retry button.
     *
     * @param listener The OnClickListener to be called when retry is clicked
     */
    fun setOnRetryClickListener(listener: OnClickListener) {
        Logger.debug(TAG, "Setting retry button click listener")
        
        retryButton.setOnClickListener { view ->
            view.isEnabled = false // Prevent double clicks
            listener.onClick(view)
            view.postDelayed({ view.isEnabled = true }, 1000)
        }
    }

    /**
     * Shows the error view with animation.
     */
    fun show() {
        Logger.debug(TAG, "Showing error view")
        
        if (visibility == View.VISIBLE) return

        alpha = 0f
        visibility = View.VISIBLE
        
        animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .withStartAction {
                announceForAccessibility(context.getString(
                    com.delayedmessaging.android.R.string.error_view_shown
                ))
            }
            .start()
    }

    /**
     * Hides the error view with animation.
     */
    fun hide() {
        Logger.debug(TAG, "Hiding error view")
        
        if (visibility == View.GONE) return

        animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .withEndAction {
                visibility = View.GONE
                announceForAccessibility(context.getString(
                    com.delayedmessaging.android.R.string.error_view_hidden
                ))
            }
            .start()
    }

    private fun setupMaterialStyling() {
        // Apply Material Design 3 elevation and shape
        elevation = resources.getDimension(
            com.google.android.material.R.dimen.material_emphasis_medium
        )
        
        // Configure retry button styling
        retryButton.apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
            setBackgroundColor(context.getColor(com.google.android.material.R.color.material_on_surface_emphasis_medium))
            rippleColor = context.getColorStateList(com.google.android.material.R.color.material_on_surface_emphasis_high_type)
        }

        // Configure error message styling
        errorMessageView.setTextAppearance(
            com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
        )
    }

    private fun setupAccessibility() {
        // Set minimum touch target size for accessibility
        val minTouchTarget = (MIN_TOUCH_TARGET_SIZE_DP * resources.displayMetrics.density).toInt()
        retryButton.minimumHeight = minTouchTarget
        retryButton.minimumWidth = minTouchTarget

        // Configure accessibility properties
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = context.getString(
            com.delayedmessaging.android.R.string.error_view_description
        )

        // Set up retry button accessibility
        retryButton.apply {
            contentDescription = context.getString(
                com.delayedmessaging.android.R.string.error_view_retry_button_description
            )
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

        // Configure error container accessibility
        errorContainer.apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }

        // Set up custom accessibility actions
        ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                super.onInitializeAccessibilityEvent(host, event)
                event.className = ErrorView::class.java.name
            }

            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = ErrorView::class.java.name
                info.roleDescription = "Error view"
                info.isClickable = true
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            }
        })
    }
}