package com.delayedmessaging.android.ui.viewmodel

import androidx.lifecycle.ViewModel // version: 2.6.1
import androidx.lifecycle.viewModelScope // version: 2.6.1
import com.delayedmessaging.android.domain.usecase.LoginUseCase
import com.delayedmessaging.android.data.repository.AuthRepository
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import kotlinx.coroutines.flow.MutableStateFlow // version: 1.6.4
import kotlinx.coroutines.flow.StateFlow // version: 1.6.4
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber // version: 5.0.1
import java.util.concurrent.TimeUnit
import javax.inject.Inject // version: 1

/**
 * ViewModel responsible for managing authentication-related UI state and operations.
 * Implements comprehensive security features including rate limiting and input validation.
 *
 * @property loginUseCase Use case for handling login operations
 * @property authRepository Repository for authentication state management
 */
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    private var lastAttemptTimestamp: Long = 0

    companion object {
        private const val LOCKOUT_DURATION = AUTH_CONFIG.LOCKOUT_DURATION_MINUTES
        private const val MAX_ATTEMPTS = AUTH_CONFIG.MAX_LOGIN_ATTEMPTS
        private val USERNAME_REGEX = Regex("^[a-zA-Z0-9._-]+$")
    }

    /**
     * Handles user login with comprehensive security checks and rate limiting.
     *
     * @param username User's username
     * @param password User's password
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                // Check rate limiting
                if (isRateLimited()) {
                    val remainingLockout = calculateRemainingLockout()
                    _uiState.value = AuthUiState.Error(
                        "Too many login attempts. Please try again in $remainingLockout minutes."
                    )
                    Timber.w("Login attempt blocked due to rate limiting for user: $username")
                    return@launch
                }

                // Input validation
                if (!validateCredentials(username, password)) {
                    return@launch
                }

                _uiState.value = AuthUiState.Loading

                // Execute login
                loginUseCase.execute(username, password).collect { result ->
                    result.fold(
                        onSuccess = { user ->
                            handleLoginSuccess(user)
                        },
                        onFailure = { error ->
                            handleLoginFailure(error)
                        }
                    )
                }
            } catch (e: Exception) {
                handleLoginFailure(e)
                Timber.e(e, "Unexpected error during login")
            }
        }
    }

    /**
     * Handles user logout with secure session cleanup.
     */
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.clearAuth()
                _uiState.value = AuthUiState.Initial
                resetFailedAttempts()
                Timber.i("User logged out successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
                _uiState.value = AuthUiState.Error("Logout failed. Please try again.")
            }
        }
    }

    /**
     * Checks current authentication state and session validity.
     *
     * @return true if user is authenticated with valid session
     */
    fun checkAuthState(): Boolean {
        return try {
            val isLoggedIn = authRepository.isLoggedIn()
            if (!isLoggedIn) {
                _uiState.value = AuthUiState.Initial
            }
            isLoggedIn
        } catch (e: Exception) {
            Timber.e(e, "Error checking auth state")
            _uiState.value = AuthUiState.Error("Session verification failed")
            false
        }
    }

    private fun validateCredentials(username: String, password: String): Boolean {
        when {
            username.isBlank() -> {
                _uiState.value = AuthUiState.Error("Username cannot be empty")
                return false
            }
            username.length < AUTH_CONFIG.MIN_USERNAME_LENGTH -> {
                _uiState.value = AuthUiState.Error(
                    "Username must be at least ${AUTH_CONFIG.MIN_USERNAME_LENGTH} characters"
                )
                return false
            }
            username.length > AUTH_CONFIG.MAX_USERNAME_LENGTH -> {
                _uiState.value = AuthUiState.Error(
                    "Username cannot exceed ${AUTH_CONFIG.MAX_USERNAME_LENGTH} characters"
                )
                return false
            }
            !username.matches(USERNAME_REGEX) -> {
                _uiState.value = AuthUiState.Error(
                    "Username can only contain letters, numbers, dots, underscores and hyphens"
                )
                return false
            }
            password.isBlank() -> {
                _uiState.value = AuthUiState.Error("Password cannot be empty")
                return false
            }
        }
        return true
    }

    private fun handleLoginSuccess(user: User) {
        _uiState.value = AuthUiState.Success(user)
        resetFailedAttempts()
        Timber.i("Login successful for user: ${user.id}")
    }

    private fun handleLoginFailure(error: Throwable) {
        incrementFailedAttempts()
        val errorMessage = when {
            _failedAttempts.value >= MAX_ATTEMPTS -> {
                "Account temporarily locked. Please try again later."
            }
            error is SecurityException -> {
                error.message ?: "Security error occurred"
            }
            else -> {
                "Invalid username or password"
            }
        }
        _uiState.value = AuthUiState.Error(errorMessage)
        Timber.w(error, "Login failed. Attempt ${_failedAttempts.value} of $MAX_ATTEMPTS")
    }

    private fun isRateLimited(): Boolean {
        return _failedAttempts.value >= MAX_ATTEMPTS &&
                System.currentTimeMillis() - lastAttemptTimestamp < TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION)
    }

    private fun calculateRemainingLockout(): Long {
        val elapsedTime = System.currentTimeMillis() - lastAttemptTimestamp
        return (TimeUnit.MINUTES.toMillis(LOCKOUT_DURATION) - elapsedTime) / TimeUnit.MINUTES.toMillis(1)
    }

    private fun incrementFailedAttempts() {
        _failedAttempts.value = _failedAttempts.value + 1
        lastAttemptTimestamp = System.currentTimeMillis()
    }

    private fun resetFailedAttempts() {
        _failedAttempts.value = 0
        lastAttemptTimestamp = 0
    }
}

/**
 * Sealed class representing all possible authentication UI states.
 */
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}