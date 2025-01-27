package com.delayedmessaging.android.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.delayedmessaging.android.R
import com.delayedmessaging.android.util.setTextOrHide
import com.google.android.material.textview.MaterialTextView
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

/**
 * A Material Design 3 compliant custom view that displays an empty state with
 * an illustration and configurable message. Supports animations, state restoration,
 * and custom styling with full accessibility support.
 */
@Parcelize
class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Parcelable {

    @IgnoredOnParcel
    private lateinit var emptyStateImage: AppCompatImageView

    @IgnoredOnParcel
    private lateinit var emptyStateText: MaterialTextView

    @IgnoredOnParcel
    private var stateChangeAnimator: AnimatorSet = AnimatorSet()

    @IgnoredOnParcel
    private var customTextAppearance: Int = com.google.android.material.R.style.TextAppearance_Material3_BodyLarge

    @IgnoredOnParcel
    private var imageTintList: ColorStateList? = null

    @IgnoredOnParcel
    private var customLayoutResId: Int = R.layout.view_empty_state

    init {
        // Extract custom attributes
        context.withStyledAttributes(attrs, R.styleable.EmptyStateView) {
            customTextAppearance = getResourceId(
                R.styleable.EmptyStateView_emptyStateTextAppearance,
                customTextAppearance
            )
            imageTintList = getColorStateList(R.styleable.EmptyStateView_emptyStateImageTint)
            customLayoutResId = getResourceId(
                R.styleable.EmptyStateView_emptyStateLayout,
                customLayoutResId
            )
        }

        // Initialize view
        initializeView()
        setupAccessibility()
    }

    private fun initializeView() {
        // Inflate layout
        LayoutInflater.from(context).inflate(customLayoutResId, this, true)

        // Initialize view references
        emptyStateImage = findViewById(R.id.emptyStateImage)
        emptyStateText = findViewById(R.id.emptyStateText)

        // Apply custom styling
        emptyStateText.setTextAppearance(customTextAppearance)
        imageTintList?.let { tintList ->
            emptyStateImage.imageTintList = tintList
        }

        // Initialize entrance animation
        stateChangeAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(emptyStateImage, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(emptyStateText, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(emptyStateImage, View.TRANSLATION_Y, 50f, 0f),
                ObjectAnimator.ofFloat(emptyStateText, View.TRANSLATION_Y, 50f, 0f)
            )
            duration = Constants.UI_CONFIG.ANIMATION_DURATION_MS
        }
    }

    private fun setupAccessibility() {
        ViewCompat.setAccessibilityDelegate(this, object : ViewCompat.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.roleDescription = "Empty state indicator"
                info.isHeading = true
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK)
            }
        })
    }

    /**
     * Sets the empty state message with proper accessibility handling
     * and entrance animation if needed.
     *
     * @param text The message to display
     */
    fun setEmptyStateText(text: String?) {
        emptyStateText.setTextOrHide(text)
        text?.let {
            ViewCompat.setAccessibilityLiveRegion(
                emptyStateText,
                ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
            )
            if (!stateChangeAnimator.isRunning) {
                stateChangeAnimator.start()
            }
        }
    }

    /**
     * Sets the empty state illustration with proper tinting and
     * accessibility support.
     *
     * @param resourceId The drawable resource ID for the illustration
     */
    fun setEmptyStateImage(resourceId: Int) {
        emptyStateImage.setImageResource(resourceId)
        imageTintList?.let { tintList ->
            emptyStateImage.imageTintList = tintList
        }
        emptyStateImage.contentDescription = context.getString(R.string.empty_state_image_description)
        
        if (!stateChangeAnimator.isRunning) {
            stateChangeAnimator.start()
        }
    }

    /**
     * Saves the current state for configuration changes and process death.
     */
    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return EmptyStateViewState(
            superState = superState,
            text = emptyStateText.text?.toString(),
            imageResId = emptyStateImage.tag as? Int,
            textAppearance = customTextAppearance,
            imageTintList = imageTintList,
            layoutResId = customLayoutResId
        )
    }

    /**
     * Restores the saved state after configuration changes or process death.
     */
    override fun onRestoreInstanceState(state: Parcelable?) {
        when (state) {
            is EmptyStateViewState -> {
                super.onRestoreInstanceState(state.superState)
                customTextAppearance = state.textAppearance
                imageTintList = state.imageTintList
                customLayoutResId = state.layoutResId
                state.text?.let { setEmptyStateText(it) }
                state.imageResId?.let { setEmptyStateImage(it) }
            }
            else -> super.onRestoreInstanceState(state)
        }
    }

    /**
     * Parcelable state class for saving view state.
     */
    @Parcelize
    private data class EmptyStateViewState(
        val superState: Parcelable?,
        val text: String?,
        val imageResId: Int?,
        val textAppearance: Int,
        val imageTintList: ColorStateList?,
        val layoutResId: Int
    ) : Parcelable
}