package com.delayedmessaging.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.delayedmessaging.android.data.repository.UserRepository
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.domain.model.UserSettings
import com.delayedmessaging.android.util.Constants.USER_STATUS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber // version: 5.0.1
import javax.inject.Inject

/**
 * ViewModel managing user profile data and operations with enhanced state management.
 * Implements MVVM pattern with real-time presence tracking and state persistence.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val uiState: StateFlow<UserProfileState> = _uiState

    private val _error = MutableStateFlow<UserError?>(null)
    val error: StateFlow<UserError?> = _error

    init {
        // Restore saved state if available
        savedStateHandle.get<String>(KEY_USER_ID)?.let { userId ->
            loadUserProfile(userId)
        }
    }

    /**
     * Loads user profile data with error handling and state management.
     */
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = UserProfileState.Loading
            
            try {
                userRepository.getUser(userId)
                    .catch { e ->
                        Timber.e(e, "Error loading user profile")
                        _error.value = UserError.LoadProfileError
                        _uiState.value = UserProfileState.Error
                    }
                    .collectLatest { user ->
                        user?.let {
                            _uiState.value = UserProfileState.Success(it)
                            savedStateHandle[KEY_USER_ID] = userId
                        } ?: run {
                            _error.value = UserError.UserNotFound
                            _uiState.value = UserProfileState.Error
                        }
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error in profile load flow")
                _error.value = UserError.UnknownError
                _uiState.value = UserProfileState.Error
            }
        }
    }

    /**
     * Updates user presence status with real-time sync.
     */
    fun updateUserPresence(userId: String, status: USER_STATUS) {
        viewModelScope.launch {
            try {
                val presence = UserPresence(
                    userId = userId,
                    status = status,
                    lastActive = System.currentTimeMillis(),
                    deviceInfo = getCurrentDeviceInfo()
                )
                
                userRepository.updateUserPresence(userId, presence)
            } catch (e: Exception) {
                Timber.e(e, "Error updating presence")
                _error.value = UserError.PresenceUpdateError
            }
        }
    }

    /**
     * Updates user settings with validation.
     */
    fun updateUserSettings(userId: String, settings: UserSettings) {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is UserProfileState.Success) {
                    val updatedUser = currentState.user.copy(settings = settings)
                    userRepository.updateUser(updatedUser)
                    _uiState.value = UserProfileState.Success(updatedUser)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating settings")
                _error.value = UserError.SettingsUpdateError
            }
        }
    }

    /**
     * Clears current error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun getCurrentDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            type = "Android",
            platform = "Android",
            clientVersion = BuildConfig.VERSION_NAME
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
    }
}

/**
 * Sealed class representing possible UI states for user profile.
 */
sealed class UserProfileState {
    object Loading : UserProfileState()
    object Error : UserProfileState()
    data class Success(val user: User) : UserProfileState()
}

/**
 * Sealed class representing possible error states.
 */
sealed class UserError {
    object LoadProfileError : UserError()
    object UserNotFound : UserError()
    object PresenceUpdateError : UserError()
    object SettingsUpdateError : UserError()
    object UnknownError : UserError()
}