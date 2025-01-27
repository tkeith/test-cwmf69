package com.delayedmessaging.android.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.delayedmessaging.android.R
import com.delayedmessaging.android.databinding.ActivityRegisterBinding
import com.delayedmessaging.android.ui.viewmodel.AuthViewModel
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import com.delayedmessaging.android.util.Constants.UI_CONFIG
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Activity handling user registration with comprehensive input validation,
 * accessibility features, and Material Design 3 compliance.
 */
@AndroidEntryPoint
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isFormValid = false

    companion object {
        private val EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )
        private val PASSWORD_PATTERN = Pattern.compile(AUTH_CONFIG.PASSWORD_PATTERN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupValidation()
        observeState()
        setupAccessibility()
    }

    private fun setupUI() {
        with(binding) {
            // Configure TextInputLayouts with Material Design attributes
            usernameInput.apply {
                counterMaxLength = UI_CONFIG.MAX_USERNAME_LENGTH
                isCounterEnabled = true
                setHelperTextColor(ContextCompat.getColorStateList(context, R.color.helper_text))
            }

            passwordInput.apply {
                helperText = getString(R.string.password_requirements)
                endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }

            emailInput.apply {
                helperText = getString(R.string.email_helper)
                setHelperTextColor(ContextCompat.getColorStateList(context, R.color.helper_text))
            }

            // Configure register button
            registerButton.apply {
                isEnabled = false
                setOnClickListener { handleRegistration() }
            }

            // Configure toolbar
            toolbar.setNavigationOnClickListener { onBackPressed() }
        }
    }

    private fun setupValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { validateForm() }
        }

        with(binding) {
            usernameInput.editText?.addTextChangedListener(textWatcher)
            passwordInput.editText?.addTextChangedListener(textWatcher)
            emailInput.editText?.addTextChangedListener(textWatcher)
        }
    }

    private fun validateForm() {
        with(binding) {
            val username = usernameInput.editText?.text.toString()
            val password = passwordInput.editText?.text.toString()
            val email = emailInput.editText?.text.toString()

            // Username validation
            when {
                username.length < UI_CONFIG.MIN_USERNAME_LENGTH -> {
                    usernameInput.error = getString(R.string.username_too_short)
                    isFormValid = false
                }
                username.length > UI_CONFIG.MAX_USERNAME_LENGTH -> {
                    usernameInput.error = getString(R.string.username_too_long)
                    isFormValid = false
                }
                !username.matches(Regex("^[a-zA-Z0-9._-]+$")) -> {
                    usernameInput.error = getString(R.string.username_invalid_chars)
                    isFormValid = false
                }
                else -> {
                    usernameInput.error = null
                }
            }

            // Password validation
            when {
                password.length < AUTH_CONFIG.MIN_PASSWORD_LENGTH -> {
                    passwordInput.error = getString(R.string.password_too_short)
                    isFormValid = false
                }
                !PASSWORD_PATTERN.matcher(password).matches() -> {
                    passwordInput.error = getString(R.string.password_requirements_not_met)
                    isFormValid = false
                }
                else -> {
                    passwordInput.error = null
                    updatePasswordStrengthIndicator(password)
                }
            }

            // Email validation
            when {
                !EMAIL_PATTERN.matcher(email).matches() -> {
                    emailInput.error = getString(R.string.invalid_email)
                    isFormValid = false
                }
                else -> {
                    emailInput.error = null
                }
            }

            // Update form validity
            isFormValid = usernameInput.error == null && 
                         passwordInput.error == null && 
                         emailInput.error == null &&
                         username.isNotBlank() &&
                         password.isNotBlank() &&
                         email.isNotBlank()

            registerButton.isEnabled = isFormValid
        }
    }

    private fun updatePasswordStrengthIndicator(password: String) {
        val strength = calculatePasswordStrength(password)
        with(binding.passwordStrengthIndicator) {
            progress = strength
            setIndicatorColor(
                when {
                    strength < 25 -> ContextCompat.getColor(context, R.color.password_weak)
                    strength < 50 -> ContextCompat.getColor(context, R.color.password_medium)
                    strength < 75 -> ContextCompat.getColor(context, R.color.password_strong)
                    else -> ContextCompat.getColor(context, R.color.password_very_strong)
                }
            )
        }
    }

    private fun calculatePasswordStrength(password: String): Int {
        var score = 0
        if (password.length >= AUTH_CONFIG.MIN_PASSWORD_LENGTH) score += 25
        if (password.contains(Regex("[A-Z]"))) score += 25
        if (password.contains(Regex("[a-z]"))) score += 25
        if (password.contains(Regex("[0-9]"))) score += 12
        if (password.contains(Regex("[^A-Za-z0-9]"))) score += 13
        return score
    }

    private fun handleRegistration() {
        with(binding) {
            val username = usernameInput.editText?.text.toString()
            val password = passwordInput.editText?.text.toString()
            val email = emailInput.editText?.text.toString()

            // Disable UI during registration
            setFormEnabled(false)
            progressIndicator.visibility = View.VISIBLE

            viewModel.register(username, email, password)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is AuthUiState.Success -> {
                            binding.progressIndicator.visibility = View.GONE
                            // Navigate to main activity
                            startMainActivity()
                        }
                        is AuthUiState.Error -> {
                            binding.progressIndicator.visibility = View.GONE
                            setFormEnabled(true)
                            showError(state.message)
                        }
                        is AuthUiState.Loading -> {
                            binding.progressIndicator.visibility = View.VISIBLE
                            setFormEnabled(false)
                        }
                        else -> {
                            binding.progressIndicator.visibility = View.GONE
                            setFormEnabled(true)
                        }
                    }
                }
            }
        }
    }

    private fun setupAccessibility() {
        with(binding) {
            ViewCompat.setAccessibilityDelegate(registerButton,
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.roleDescription = getString(R.string.register_button_description)
                        info.isEnabled = isFormValid
                    }
                }
            )

            // Set content descriptions
            usernameInput.editText?.contentDescription = getString(R.string.username_input_description)
            passwordInput.editText?.contentDescription = getString(R.string.password_input_description)
            emailInput.editText?.contentDescription = getString(R.string.email_input_description)
        }
    }

    private fun setFormEnabled(enabled: Boolean) {
        with(binding) {
            usernameInput.isEnabled = enabled
            passwordInput.isEnabled = enabled
            emailInput.isEnabled = enabled
            registerButton.isEnabled = enabled && isFormValid
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).apply {
            setAction(getString(R.string.dismiss)) { dismiss() }
            show()
        }
    }

    private fun startMainActivity() {
        // Implementation for starting MainActivity
        // This would typically use an Intent to start the main activity
        // and finish the current activity
    }
}