package com.delayedmessaging.android.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.ActivityLoginBinding
import com.delayedmessaging.android.ui.main.MainActivity
import com.delayedmessaging.android.ui.viewmodel.AuthViewModel
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import com.delayedmessaging.android.util.Constants.UI_CONFIG
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Activity handling user authentication with comprehensive security measures,
 * Material Design 3 compliance, and WCAG 2.1 Level AA accessibility support.
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    
    @Inject
    lateinit var viewModel: AuthViewModel
    
    private var lastAttemptTime: Long = 0
    private var loginAttempts: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAccessibility()
        setupInputValidation()
        setupLoginButton()
        setupUiStateCollection()
        setupThemeAwareness()
    }

    private fun setupAccessibility() {
        with(binding) {
            // Set content descriptions
            usernameInput.contentDescription = getString(R.string.username_input_description)
            passwordInput.contentDescription = getString(R.string.password_input_description)
            loginButton.contentDescription = getString(R.string.login_button_description)

            // Set traversal order
            usernameInput.accessibilityTraversalBefore = R.id.passwordInput
            passwordInput.accessibilityTraversalAfter = R.id.usernameInput
            passwordInput.accessibilityTraversalBefore = R.id.loginButton

            // Enable TalkBack support
            root.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            loginButton.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
    }

    private fun setupInputValidation() {
        with(binding) {
            // Username validation
            usernameLayout.apply {
                isCounterEnabled = true
                counterMaxLength = UI_CONFIG.MAX_USERNAME_LENGTH
                setErrorIconDrawable(R.drawable.ic_error)
            }

            // Password validation
            passwordLayout.apply {
                isPasswordVisibilityToggleEnabled = true
                setErrorIconDrawable(R.drawable.ic_error)
            }

            // Real-time validation
            usernameInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    validateUsername()
                }
            }

            passwordInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    validatePassword()
                }
            }
        }
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            if (checkRateLimiting() && validateInput()) {
                handleLogin()
            }
        }
    }

    private fun setupUiStateCollection() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUiForState(state)
                }
            }
        }
    }

    private fun setupThemeAwareness() {
        // Apply Material Design 3 theming
        binding.root.setBackgroundColor(getColor(R.color.md_theme_background))
        binding.loginButton.setBackgroundColor(getColor(R.color.md_theme_primary))
    }

    private fun handleLogin() {
        val username = binding.usernameInput.text.toString()
        val password = binding.passwordInput.text.toString()

        binding.loginProgress.isVisible = true
        binding.loginButton.isEnabled = false

        lifecycleScope.launch {
            viewModel.login(username, password)
        }
    }

    private fun validateInput(): Boolean {
        return validateUsername() && validatePassword()
    }

    private fun validateUsername(): Boolean {
        val username = binding.usernameInput.text.toString()
        return when {
            username.isBlank() -> {
                showInputError(binding.usernameLayout, R.string.error_username_empty)
                false
            }
            username.length < UI_CONFIG.MIN_USERNAME_LENGTH -> {
                showInputError(binding.usernameLayout, R.string.error_username_too_short)
                false
            }
            username.length > UI_CONFIG.MAX_USERNAME_LENGTH -> {
                showInputError(binding.usernameLayout, R.string.error_username_too_long)
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9._-]+$")) -> {
                showInputError(binding.usernameLayout, R.string.error_username_invalid_chars)
                false
            }
            else -> {
                binding.usernameLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = binding.passwordInput.text.toString()
        return when {
            password.isBlank() -> {
                showInputError(binding.passwordLayout, R.string.error_password_empty)
                false
            }
            password.length < AUTH_CONFIG.MIN_PASSWORD_LENGTH -> {
                showInputError(binding.passwordLayout, R.string.error_password_too_short)
                false
            }
            !password.matches(Regex(AUTH_CONFIG.PASSWORD_PATTERN)) -> {
                showInputError(binding.passwordLayout, R.string.error_password_requirements)
                false
            }
            else -> {
                binding.passwordLayout.error = null
                true
            }
        }
    }

    private fun checkRateLimiting(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastAttempt = currentTime - lastAttemptTime

        return when {
            loginAttempts >= AUTH_CONFIG.MAX_LOGIN_ATTEMPTS && 
            timeSinceLastAttempt < TimeUnit.MINUTES.toMillis(AUTH_CONFIG.LOCKOUT_DURATION_MINUTES) -> {
                val remainingLockout = AUTH_CONFIG.LOCKOUT_DURATION_MINUTES - 
                    TimeUnit.MILLISECONDS.toMinutes(timeSinceLastAttempt)
                showRateLimitError(remainingLockout)
                false
            }
            timeSinceLastAttempt >= TimeUnit.MINUTES.toMillis(AUTH_CONFIG.LOCKOUT_DURATION_MINUTES) -> {
                loginAttempts = 0
                true
            }
            else -> {
                loginAttempts++
                lastAttemptTime = currentTime
                true
            }
        }
    }

    private fun updateUiForState(state: AuthUiState) {
        binding.loginProgress.isVisible = state is AuthUiState.Loading

        when (state) {
            is AuthUiState.Success -> {
                navigateToMain()
            }
            is AuthUiState.Error -> {
                binding.loginButton.isEnabled = true
                showError(state.message)
                announceForAccessibility(state.message)
            }
            is AuthUiState.Loading -> {
                binding.loginButton.isEnabled = false
                announceForAccessibility(getString(R.string.login_progress))
            }
            else -> {
                binding.loginButton.isEnabled = true
            }
        }
    }

    private fun showInputError(layout: com.google.android.material.textfield.TextInputLayout, errorResId: Int) {
        layout.error = getString(errorResId)
        layout.requestFocus()
        announceForAccessibility(getString(errorResId))
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") { }
            .setAnchorView(binding.loginButton)
            .show()
    }

    private fun showRateLimitError(remainingMinutes: Long) {
        val message = getString(R.string.error_rate_limit, remainingMinutes)
        showError(message)
        announceForAccessibility(message)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun announceForAccessibility(message: String) {
        binding.root.announceForAccessibility(message)
        binding.root.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear sensitive data
        binding.passwordInput.text?.clear()
    }
}