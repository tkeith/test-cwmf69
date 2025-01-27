package com.delayedmessaging.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.recyclerview.widget.DiffUtil // version: 1.3.0
import androidx.recyclerview.widget.RecyclerView // version: 1.3.0
import com.google.android.material.textview.MaterialTextView // version: 1.9.0
import com.delayedmessaging.android.R
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.util.DateTimeUtils

/**
 * RecyclerView adapter for displaying messages with Material Design 3 styling,
 * delivery timers, and accessibility support.
 *
 * @property clickListener Listener for message item click events
 */
class MessageAdapter(
    private val clickListener: MessageClickListener
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<Message> = emptyList()
    private val timerStates = mutableMapOf<String, Long>()

    private val diffCallback = object : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        
        // Set message content with Material styling
        holder.contentView.apply {
            text = message.content
            contentDescription = context.getString(
                R.string.message_content_description,
                message.content
            )
        }

        // Set timestamp with Material styling
        holder.timestampView.apply {
            text = DateTimeUtils.formatMessageTimestamp(message.createdAt)
            contentDescription = context.getString(
                R.string.message_timestamp_description,
                DateTimeUtils.formatMessageTimestamp(message.createdAt)
            )
        }

        // Set status indicator
        holder.statusView.apply {
            text = message.status.getDisplayText()
            setTextColor(context.getColor(getStatusColor(message.status)))
            contentDescription = context.getString(
                R.string.message_status_description,
                message.status.getDisplayText()
            )
        }

        // Handle delivery timer
        if (message.status == MessageStatus.QUEUED) {
            holder.delayTimerView.apply {
                visibility = View.VISIBLE
                startTimer(message.scheduledFor)
                timerStates[message.id] = message.scheduledFor
            }
        } else {
            holder.delayTimerView.apply {
                stopTimer()
                visibility = View.GONE
            }
            timerStates.remove(message.id)
        }

        // Set click listener with accessibility support
        holder.itemView.apply {
            setOnClickListener {
                clickListener.onMessageClick(message)
                sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)
            }
            isClickable = true
            isFocusable = true
        }

        // Set combined accessibility description
        holder.itemView.contentDescription = buildContentDescription(message)
    }

    override fun getItemCount(): Int = messages.size

    override fun onViewRecycled(holder: MessageViewHolder) {
        super.onViewRecycled(holder)
        holder.delayTimerView.stopTimer()
    }

    /**
     * Updates the adapter's message list using DiffUtil for efficient updates.
     *
     * @param newMessages New list of messages to display
     */
    fun updateMessages(newMessages: List<Message>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = messages.size
            override fun getNewListSize() = newMessages.size
            
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return messages[oldPos].id == newMessages[newPos].id
            }

            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                return messages[oldPos] == newMessages[newPos]
            }
        })

        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Saves timer states for configuration changes.
     */
    fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putSerializable("timer_states", HashMap(timerStates))
        }
    }

    /**
     * Restores timer states after configuration changes.
     */
    fun onRestoreInstanceState(state: Bundle) {
        @Suppress("UNCHECKED_CAST")
        (state.getSerializable("timer_states") as? HashMap<String, Long>)?.let {
            timerStates.clear()
            timerStates.putAll(it)
        }
    }

    private fun getStatusColor(status: MessageStatus): Int {
        return when (status) {
            MessageStatus.DELIVERED -> R.color.message_status_delivered
            MessageStatus.FAILED -> R.color.message_status_failed
            MessageStatus.QUEUED -> R.color.message_status_queued
            MessageStatus.SENDING -> R.color.message_status_sending
            else -> R.color.message_status_default
        }
    }

    private fun buildContentDescription(message: Message): String {
        return StringBuilder().apply {
            append(message.content)
            append(". ")
            append("Status: ${message.status.getDisplayText()}")
            if (message.status == MessageStatus.QUEUED) {
                append(". Delivery in: ${message.getRemainingDeliveryTime() / 1000} seconds")
            }
        }.toString()
    }

    /**
     * ViewHolder for message items with Material Design support.
     */
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contentView: MaterialTextView = itemView.findViewById(R.id.message_content)
        val timestampView: MaterialTextView = itemView.findViewById(R.id.message_timestamp)
        val statusView: MaterialTextView = itemView.findViewById(R.id.message_status)
        val delayTimerView: DelayTimerView = itemView.findViewById(R.id.message_timer)
    }
}

/**
 * Interface for handling message item clicks.
 */
interface MessageClickListener {
    /**
     * Called when a message item is clicked.
     *
     * @param message The clicked message
     */
    fun onMessageClick(message: Message)
}