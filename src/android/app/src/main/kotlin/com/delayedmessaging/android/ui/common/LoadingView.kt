package com.delayedmessaging.android.ui.common

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.delayedmessaging.android.R
import com.delayedmessaging.android.util.Extensions.setVisibilityGone
import com.delayedmessaging.android.util.Extensions.setVisibilityVisible
import kotlinx.parcelize.Parcelize

/**
 * A Material Design 3 compliant loading indicator view that displays a circular progress
 * indicator with an optional text message. This component is thread-safe and supports
 * RTL layouts and accessibility features.
 *
 * @property loadingSpinner The circular progress indicator
 * @property loadingText The text view displaying the loading message
 * @property loadingMessage Current loading message text
 * @property isShowing Whether the loading view is currently visible
 */
class LoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val loadingSpinner: ProgressBar
    private val loadingText: TextView
    private var loadingMessage: String = ""
    private var isShowing: Boolean = false

    @Parcelize
    private data class SavedState(
        val superState: Parcelable?,
        val message: String,
        val isShowing: Boolean
    ) : Parcelable

    init {
        // Inflate layout with Material Design 3 theming
        LayoutInflater.from(context).inflate(R.layout.view_loading, this, true)

        // Initialize views with proper accessibility support
        loadingSpinner = findViewById<ProgressBar>(R.id.progressBar).apply {
            indeterminateTintList = context.getColorStateList(R.color.material_primary)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        loadingText = findViewById<TextView>(R.id.textViewLoading).apply {
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        // Set up accessibility delegate for the entire view
        ViewCompat.setAccessibilityDelegate(this, object : ViewCompat.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = "Loading indicator"
                info.isContentInvalid = false
                if (isShowing) {
                    info.text = loadingMessage.ifEmpty { "Loading" }
                }
            }
        })

        // Initialize view as hidden
        setVisibilityGone()
    }

    /**
     * Shows the loading indicator with an optional message.
     * Thread-safe implementation for UI state management.
     *
     * @param message Optional message to display below the loading spinner
     */
    @Synchronized
    fun show(message: String? = null) {
        post {
            if (!isShowing) {
                message?.let { setMessage(it) }
                setVisibilityVisible()
                isShowing = true
                announceForAccessibility(
                    loadingMessage.ifEmpty { context.getString(R.string.loading) }
                )
            }
        }
    }

    /**
     * Hides the loading indicator and cleans up resources.
     * Thread-safe implementation for UI state management.
     */
    @Synchronized
    fun hide() {
        post {
            if (isShowing) {
                setVisibilityGone()
                loadingMessage = ""
                loadingText.text = ""
                isShowing = false
                announceForAccessibility(context.getString(R.string.loading_complete))
            }
        }
    }

    /**
     * Updates the loading message text with proper accessibility handling.
     * Thread-safe implementation for text updates.
     *
     * @param message The new message to display
     */
    @Synchronized
    fun setMessage(message: String) {
        post {
            loadingMessage = message
            loadingText.apply {
                text = message
                if (message.isNotEmpty()) {
                    setVisibilityVisible()
                } else {
                    setVisibilityGone()
                }
            }
            announceForAccessibility(message)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(
            superState = super.onSaveInstanceState(),
            message = loadingMessage,
            isShowing = isShowing
        )
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is SavedState -> {
                super.onRestoreInstanceState(state.superState)
                loadingMessage = state.message
                if (state.isShowing) {
                    show(state.message)
                }
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up resources when view is detached
        hide()
    }
}