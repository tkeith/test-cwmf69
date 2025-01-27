package com.delayedmessaging.android.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.FragmentMessageListBinding
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.ui.common.MessageAdapter
import com.delayedmessaging.android.ui.common.MessageClickListener
import com.delayedmessaging.android.ui.viewmodel.MessageListViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment responsible for displaying the message list with real-time updates
 * and Material Design 3 compliance.
 */
class MessageListFragment : Fragment(), MessageClickListener {

    private var _binding: FragmentMessageListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MessageListViewModel by viewModels()
    private var messageAdapter: MessageAdapter? = null
    private var messageCollectionJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView(savedInstanceState)
        setupSwipeRefresh()
        setupErrorHandling()
        observeViewModelState()

        // Restore saved state if available
        savedInstanceState?.let { state ->
            messageAdapter?.onRestoreInstanceState(state)
            viewModel.restoreState(state)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        messageAdapter?.onSaveInstanceState()?.let { adapterState ->
            outState.putAll(adapterState)
        }
        viewModel.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messageCollectionJob?.cancel()
        messageAdapter = null
        _binding = null
    }

    private fun setupRecyclerView(savedInstanceState: Bundle?) {
        binding.messageList.apply {
            layoutManager = LinearLayoutManager(context).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for better performance
            
            messageAdapter = MessageAdapter(this@MessageListFragment).also { adapter ->
                this.adapter = adapter
                savedInstanceState?.let { state ->
                    adapter.onRestoreInstanceState(state)
                }
            }

            // Optimize RecyclerView performance
            setItemViewCacheSize(20)
            recycledViewPool.setMaxRecycledViews(0, 20)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeResources(R.color.material_primary)
            setProgressBackgroundColorSchemeResource(R.color.material_surface)
            setOnRefreshListener {
                viewModel.refreshMessages()
            }
        }
    }

    private fun setupErrorHandling() {
        binding.retryButton.setOnClickListener {
            viewModel.retryLastOperation()
        }
    }

    private fun observeViewModelState() {
        // Cancel existing job before starting new collection
        messageCollectionJob?.cancel()
        
        messageCollectionJob = viewLifecycleOwner.lifecycleScope.launch {
            // Collect messages
            launch {
                viewModel.messages.collectLatest { messages ->
                    messageAdapter?.updateMessages(messages)
                    binding.emptyState.visibility = 
                        if (messages.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            // Collect loading state
            launch {
                viewModel.isLoading.collectLatest { isLoading ->
                    binding.swipeRefresh.isRefreshing = isLoading
                    binding.progressBar.visibility = 
                        if (isLoading && messageAdapter?.itemCount == 0) View.VISIBLE else View.GONE
                }
            }

            // Collect error state
            launch {
                viewModel.error.collectLatest { error ->
                    error?.let { errorMessage ->
                        showError(errorMessage)
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            setAction(R.string.retry) {
                viewModel.retryLastOperation()
            }
            setAnchorView(R.id.fab_compose)
            show()
        }
    }

    override fun onMessageClick(message: Message) {
        viewModel.selectMessage(message.id)
        // Navigation would be handled here
    }

    companion object {
        fun newInstance() = MessageListFragment()
    }
}