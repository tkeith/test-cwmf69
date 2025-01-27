package com.delayedmessaging.android.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.FragmentMessageDetailBinding
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.ui.common.DelayTimerView
import com.delayedmessaging.android.util.DateTimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment responsible for displaying detailed message information including
 * content, status timeline, and delivery countdown.
 */
@AndroidEntryPoint
class MessageDetailFragment : Fragment() {

    private var _binding: FragmentMessageDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageDetailViewModel by viewModels()
    private var messageId: String? = null

    @Inject
    lateinit var dateTimeUtils: DateTimeUtils

    companion object {
        private const val ARG_MESSAGE_ID = "message_id"
        private const val DATE_FORMAT = "HH:mm:ss"

        /**
         * Creates a new instance of MessageDetailFragment with the given message ID
         */
        fun newInstance(messageId: String) = MessageDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MESSAGE_ID, messageId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageId = arguments?.getString(ARG_MESSAGE_ID)
            ?: throw IllegalArgumentException("Message ID is required")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupMessageObserver()
        setupRefreshLayout()
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            setNavigationOnClickListener { requireActivity().onBackPressed() }
            title = getString(R.string.message_details_title)
        }
    }

    private fun setupMessageObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getMessage(messageId!!).collectLatest { message ->
                updateMessageContent(message)
                updateStatusTimeline(message)
                updateDeliveryTimer(message)
            }
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefreshLayout.apply {
            setOnRefreshListener {
                viewModel.refreshMessage(messageId!!)
                isRefreshing = false
            }
        }
    }

    private fun updateMessageContent(message: Message) {
        binding.apply {
            messageContent.text = message.content
            messageTimestamp.text = formatTimestamp(message.createdAt)
            recipientInfo.text = getString(R.string.message_recipient_format, message.recipientId)
        }
    }

    private fun updateStatusTimeline(message: Message) {
        binding.statusTimeline.apply {
            // Update status indicators based on current message status
            val statusOrder = listOf(
                MessageStatus.DRAFT,
                MessageStatus.QUEUED,
                MessageStatus.SENDING,
                MessageStatus.DELIVERED
            )

            statusOrder.forEach { status ->
                val indicator = when (status) {
                    MessageStatus.DRAFT -> binding.statusDraft
                    MessageStatus.QUEUED -> binding.statusQueued
                    MessageStatus.SENDING -> binding.statusSending
                    MessageStatus.DELIVERED -> binding.statusDelivered
                    else -> null
                }

                indicator?.apply {
                    val isCompleted = statusOrder.indexOf(status) <= 
                        statusOrder.indexOf(message.status)
                    setCompletedState(isCompleted)
                    
                    val isActive = status == message.status
                    setActiveState(isActive)
                }
            }

            // Update status timestamps
            binding.statusTimestamps.apply {
                draftTime.text = formatTimestamp(message.createdAt)
                queuedTime.text = formatTimestamp(message.createdAt)
                sendingTime.text = if (message.status.ordinal >= 
                    MessageStatus.SENDING.ordinal) {
                    formatTimestamp(message.scheduledFor)
                } else ""
                deliveredTime.text = message.deliveredAt?.let { 
                    formatTimestamp(it) 
                } ?: ""
            }
        }
    }

    private fun updateDeliveryTimer(message: Message) {
        if (message.status == MessageStatus.QUEUED) {
            binding.delayTimer.apply {
                visibility = View.VISIBLE
                val remainingTime = message.getRemainingDeliveryTime()
                if (remainingTime > 0) {
                    startTimer(message.scheduledFor)
                } else {
                    stopTimer()
                }
            }
        } else {
            binding.delayTimer.apply {
                stopTimer()
                visibility = View.GONE
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            .format(timestamp)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.delayTimer.stopTimer()
        _binding = null
    }
}