package com.delayedmessaging.android.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.FragmentSettingsBinding
import com.delayedmessaging.android.ui.viewmodel.SettingsViewModel
import com.delayedmessaging.android.util.Constants.USER_STATUS
import com.delayedmessaging.android.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment responsible for managing user settings including theme mode and notification preferences.
 * Implements Material Design 3 components and follows Android best practices.
 */
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val TAG = "SettingsFragment"
    private val viewModel: SettingsViewModel by viewModels()
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Theme radio buttons
    private lateinit var lightThemeRadio: MaterialRadioButton
    private lateinit var darkThemeRadio: MaterialRadioButton
    private lateinit var systemThemeRadio: MaterialRadioButton

    // Notification switches
    private lateinit var messageDeliverySwitch: SwitchMaterial
    private lateinit var newMessagesSwitch: SwitchMaterial
    private lateinit var statusUpdatesSwitch: SwitchMaterial

    // Coroutine jobs for cleanup
    private var themeJob: Job? = null
    private var notificationJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Logger.debug(TAG, "Creating settings view")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.debug(TAG, "Setting up settings view")

        initializeViews()
        setupThemeControls()
        setupNotificationControls()
        observeViewModelState()
    }

    private fun initializeViews() {
        with(binding) {
            // Initialize theme controls
            lightThemeRadio = themeRadioGroup.lightThemeRadio
            darkThemeRadio = themeRadioGroup.darkThemeRadio
            systemThemeRadio = themeRadioGroup.systemThemeRadio

            // Initialize notification controls
            messageDeliverySwitch = notificationGroup.messageDeliverySwitch
            newMessagesSwitch = notificationGroup.newMessagesSwitch
            statusUpdatesSwitch = notificationGroup.statusUpdatesSwitch
        }
    }

    private fun setupThemeControls() {
        Logger.debug(TAG, "Setting up theme controls")

        // Light theme selection
        lightThemeRadio.setOnClickListener {
            viewModel.setDarkMode(false)
            viewModel.setSystemTheme(false)
        }

        // Dark theme selection
        darkThemeRadio.setOnClickListener {
            viewModel.setDarkMode(true)
            viewModel.setSystemTheme(false)
        }

        // System theme selection
        systemThemeRadio.setOnClickListener {
            viewModel.setSystemTheme(true)
        }
    }

    private fun setupNotificationControls() {
        Logger.debug(TAG, "Setting up notification controls")

        // Message delivery notifications
        messageDeliverySwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotificationType(SettingsViewModel.NotificationType.MESSAGE_DELIVERY)
        }

        // New message notifications
        newMessagesSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotificationType(SettingsViewModel.NotificationType.NEW_MESSAGES)
        }

        // Status update notifications
        statusUpdatesSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotificationType(SettingsViewModel.NotificationType.STATUS_UPDATES)
        }
    }

    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe theme state
            themeJob = launch {
                viewModel.isDarkMode.collectLatest { isDarkMode ->
                    Logger.debug(TAG, "Theme state updated: isDarkMode=$isDarkMode")
                    updateThemeUI(isDarkMode)
                }
            }

            // Observe notification state
            notificationJob = launch {
                viewModel.notificationTypes.collectLatest { types ->
                    Logger.debug(TAG, "Notification types updated: $types")
                    updateNotificationUI(types)
                }
            }
        }
    }

    private fun updateThemeUI(isDarkMode: Boolean) {
        darkThemeRadio.isChecked = isDarkMode
        lightThemeRadio.isChecked = !isDarkMode
        systemThemeRadio.isChecked = false
    }

    private fun updateNotificationUI(types: Set<SettingsViewModel.NotificationType>) {
        messageDeliverySwitch.isChecked = types.contains(SettingsViewModel.NotificationType.MESSAGE_DELIVERY)
        newMessagesSwitch.isChecked = types.contains(SettingsViewModel.NotificationType.NEW_MESSAGES)
        statusUpdatesSwitch.isChecked = types.contains(SettingsViewModel.NotificationType.STATUS_UPDATES)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Logger.debug(TAG, "Cleaning up settings view")
        
        // Cancel coroutine jobs
        themeJob?.cancel()
        notificationJob?.cancel()

        // Clear view binding
        _binding = null
    }

    companion object {
        /**
         * Creates a new instance of SettingsFragment
         * @return A new instance of SettingsFragment
         */
        fun newInstance() = SettingsFragment()
    }
}