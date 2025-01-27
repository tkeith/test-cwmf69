package com.delayedmessaging.android.ui.messages

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment // version: 1.6.1
import androidx.lifecycle.lifecycleScope // version: 2.6.2
import androidx.lifecycle.repeatOnLifecycle // version: 2.6.2
import com.delayedmessaging.android.R
import com.delayedmessaging.android.ui.common.DelayTimerView
import com.delayedmessaging.android.ui.viewmodel.ComposeMessageViewModel
import com.google.android.material.button.MaterialButton // version: 1.9.0
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject // version: 1
import kotlin.time.Duration.Companion.milliseconds

/**
 * Fragment responsible for message composition with enforced delivery delay.
 * Implements Material Design 3 components and accessibility support.
 */
@AndroidEntryPoint
class ComposeMessageFragment : Fragment() {

    @Inject
    lateinit var viewModel: ComposeMessageViewModel

    private lateinit var messageInputLayout: TextInputLayout
    private lateinit var messageInput: TextInputEditText
    private lateinit var recipientInputLayout: TextInputLayout
    private lateinit var recipientInput: TextInputEditText
    private lateinit var characterCount: TextInputLayout
    private lateinit var sendButton: MaterialButton
    private lateinit var delayTimerView: DelayTimerView
    private lateinit var loadingIndicator: CircularProgressIndicator

    companion object {
        private const val DEBOUNCE_TIME_MS = 300L
        private const val MAX_CHARS = 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_compose_message, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupAccessibility()
        setupMessageInputWatcher()
        setupRecipientInputWatcher()
        setupSendButton()
        setupViewModelObservers()
    }

    private fun initializeViews(view: View) {
        messageInputLayout = view.findViewById(R.id.messageInputLayout)
        messageInput = view.findViewById(R.id.messageInput)
        recipientInputLayout = view.findViewById(R.id.recipientInputLayout)
        recipientInput = view.findViewById(R.id.recipientInput)
        characterCount = view.findViewById(R.id.characterCount)
        sendButton = view.findViewById(R.id.sendButton)
        delayTimerView = view.findViewById(R.id.delayTimerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        // Set initial states
        sendButton.isEnabled = false
        delayTimerView.visibility = View.GONE
        loadingIndicator.visibility = View.GONE
    }

    private fun setupAccessibility() {
        messageInput.contentDescription = getString(R.string.message_input_description)
        recipientInput.contentDescription = getString(R.string.recipient_input_description)
        sendButton.contentDescription = getString(R.string.send_button_description)
    }

    @OptIn(FlowPreview::class)
    private fun setupMessageInputWatcher() {
        messageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharacterCount(s?.length ?: 0)
            }

            override fun afterTextChanged(editable: Editable?) {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.updateMessageContent(editable?.toString() ?: "")
                }
            }
        })
    }

    private fun setupRecipientInputWatcher() {
        recipientInput.addTextChangedListener { text ->
            viewLifecycleOwner.lifecycleScope.launch {
                text?.toString()?.let { recipient ->
                    if (recipient.isNotBlank()) {
                        viewModel.setRecipient(recipient)
                    }
                }
            }
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                sendButton.isEnabled = false
                loadingIndicator.visibility = View.VISIBLE
                viewModel.sendMessage()
            }
        }
    }

    private fun setupViewModelObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    viewModel.messageContent.collect { content ->
                        messageInput.setText(content)
                        updateSendButtonState()
                    }
                }

                launch {
                    viewModel.isSending.collect { isSending ->
                        loadingIndicator.visibility = if (isSending) View.VISIBLE else View.GONE
                        messageInput.isEnabled = !isSending
                        recipientInput.isEnabled = !isSending
                    }
                }

                launch {
                    viewModel.remainingDeliveryTime.collect { remainingTime ->
                        if (remainingTime > 0) {
                            delayTimerView.visibility = View.VISIBLE
                            delayTimerView.startTimer(System.currentTimeMillis() + remainingTime)
                        } else {
                            delayTimerView.stopTimer()
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let { showError(it) }
                    }
                }
            }
        }
    }

    private fun updateCharacterCount(length: Int) {
        val remaining = MAX_CHARS - length
        characterCount.helperText = "$remaining/${MAX_CHARS}"
        
        if (length > MAX_CHARS) {
            characterCount.error = getString(R.string.character_limit_exceeded)
        } else {
            characterCount.error = null
        }

        // Announce character count for accessibility
        characterCount.announceForAccessibility(
            getString(R.string.characters_remaining, remaining)
        )
    }

    private fun updateSendButtonState() {
        val isValidMessage = messageInput.text?.isNotBlank() == true &&
                (messageInput.text?.length ?: 0) <= MAX_CHARS &&
                recipientInput.text?.isNotBlank() == true

        sendButton.isEnabled = isValidMessage
    }

    private fun showError(error: String) {
        Snackbar.make(
            requireView(),
            error,
            Snackbar.LENGTH_LONG
        ).apply {
            setAction(R.string.dismiss) { dismiss() }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        delayTimerView.stopTimer()
    }
}