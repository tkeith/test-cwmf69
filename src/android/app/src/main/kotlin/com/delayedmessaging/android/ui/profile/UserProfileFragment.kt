package com.delayedmessaging.android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.FragmentUserProfileBinding
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.ui.viewmodel.UserProfileViewModel
import com.delayedmessaging.android.util.Constants.USER_STATUS
import com.google.android.material.card.MaterialCardView // version: 1.9.0
import com.google.android.material.progressindicator.CircularProgressIndicator // version: 1.9.0
import com.google.android.material.snackbar.Snackbar // version: 1.9.0
import com.bumptech.glide.Glide // version: 4.15.1
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment displaying user profile information with Material Design 3 components.
 * Implements real-time presence status updates and profile management.
 */
@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: UserProfileViewModel.Factory
    private val viewModel: UserProfileViewModel by viewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupObservers()
        setupPresenceControls()
        viewModel.loadUserProfile()
    }

    private fun setupUI() {
        with(binding) {
            // Configure Material Card elevation and shape
            profileCard.apply {
                elevation = resources.getDimension(R.dimen.card_elevation)
                radius = resources.getDimension(R.dimen.card_corner_radius)
            }

            // Setup presence status radio group
            presenceGroup.setOnCheckedChangeListener { _, checkedId ->
                val newStatus = when (checkedId) {
                    R.id.radioOnline -> USER_STATUS.ONLINE
                    R.id.radioAway -> USER_STATUS.AWAY
                    R.id.radioDoNotDisturb -> USER_STATUS.DO_NOT_DISTURB
                    else -> USER_STATUS.OFFLINE
                }
                viewModel.updatePresenceStatus(newStatus)
            }

            // Configure error retry button
            retryButton.setOnClickListener {
                viewModel.loadUserProfile()
            }

            // Setup accessibility
            profileCard.contentDescription = getString(R.string.profile_card_content_description)
            presenceGroup.contentDescription = getString(R.string.presence_group_content_description)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe user profile data
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UserProfileState.Success -> updateUI(state.user)
                            is UserProfileState.Loading -> showLoading(true)
                            is UserProfileState.Error -> showError()
                        }
                    }
                }

                // Observe error states
                launch {
                    viewModel.error.collect { error ->
                        error?.let { showError(it) }
                    }
                }
            }
        }
    }

    private fun updateUI(user: User) {
        with(binding) {
            showLoading(false)
            
            // Update profile information
            username.text = user.username
            email.text = user.email

            // Update presence status indicator
            val statusColor = when (user.presence.status) {
                USER_STATUS.ONLINE -> R.color.status_online
                USER_STATUS.AWAY -> R.color.status_away
                USER_STATUS.DO_NOT_DISTURB -> R.color.status_dnd
                else -> R.color.status_offline
            }
            presenceIndicator.setColorFilter(
                resources.getColor(statusColor, requireContext().theme)
            )

            // Load avatar with Glide
            user.avatarUrl?.let { url ->
                Glide.with(this@UserProfileFragment)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_error)
                    .into(avatarImage)
            }

            // Update presence radio selection
            presenceGroup.check(
                when (user.presence.status) {
                    USER_STATUS.ONLINE -> R.id.radioOnline
                    USER_STATUS.AWAY -> R.id.radioAway
                    USER_STATUS.DO_NOT_DISTURB -> R.id.radioDoNotDisturb
                    else -> R.id.radioOffline
                }
            )

            // Update content descriptions for accessibility
            avatarImage.contentDescription = 
                getString(R.string.avatar_content_description, user.username)
            presenceIndicator.contentDescription = 
                getString(R.string.presence_indicator_description, user.presence.status.name)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        with(binding) {
            progressIndicator.isVisible = isLoading
            contentGroup.isVisible = !isLoading
            errorGroup.isVisible = false
        }
    }

    private fun showError(error: UserError = UserError.UnknownError) {
        with(binding) {
            progressIndicator.isVisible = false
            contentGroup.isVisible = false
            errorGroup.isVisible = true
            
            errorText.text = when (error) {
                is UserError.LoadProfileError -> getString(R.string.error_loading_profile)
                is UserError.PresenceUpdateError -> getString(R.string.error_updating_presence)
                else -> getString(R.string.error_unknown)
            }
        }

        Snackbar.make(
            binding.root,
            getString(R.string.error_message),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.retry)) {
            viewModel.loadUserProfile()
        }.show()
    }

    private fun setupPresenceControls() {
        with(binding) {
            // Configure presence radio buttons with Material Design styles
            radioOnline.apply {
                setTextColor(resources.getColorStateList(R.color.presence_text_selector, null))
                buttonTintList = resources.getColorStateList(R.color.presence_radio_selector, null)
            }
            radioAway.apply {
                setTextColor(resources.getColorStateList(R.color.presence_text_selector, null))
                buttonTintList = resources.getColorStateList(R.color.presence_radio_selector, null)
            }
            radioDoNotDisturb.apply {
                setTextColor(resources.getColorStateList(R.color.presence_text_selector, null))
                buttonTintList = resources.getColorStateList(R.color.presence_radio_selector, null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up resources
        _binding = null
        Glide.with(this).clear(binding.avatarImage)
    }

    companion object {
        fun newInstance() = UserProfileFragment()
    }
}