package com.delayedmessaging.android.data.repository

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences // version: 1.1.0-alpha06
import com.delayedmessaging.android.data.api.ApiService
import com.delayedmessaging.android.data.api.LoginRequest
import com.delayedmessaging.android.data.api.RegisterRequest
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import com.delayedmessaging.android.util.Constants.SHARED_PREFS
import kotlinx.coroutines.flow.Flow // version: 1.6.4
import kotlinx.coroutines.flow.flow
import java.util.regex.Pattern
import javax.inject.Inject // version: 1
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Repository implementation for secure authentication operations in the Delayed Messaging Android application.
 * Handles user authentication, token management, and session persistence with comprehensive security measures.
 *
 * @property apiService API service for authentication endpoints
 * @property encryptedPreferences Encrypted storage for sensitive authentication data
 */
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val encryptedPreferences: EncryptedSharedPreferences
) {
    private companion object {
        const val AUTH_TOKEN_KEY = SHARED_PREFS.PREF_AUTH_TOKEN
        const val REFRESH_TOKEN_KEY = SHARED_PREFS.PREF_REFRESH_TOKEN
        const val TOKEN_EXPIRY_TIME = TimeUnit.HOURS.toMillis(AUTH_CONFIG.TOKEN_EXPIRY_HOURS)
        
        // Rate limiting configuration
        private const val MAX_LOGIN_ATTEMPTS = AUTH_CONFIG.MAX_LOGIN_ATTEMPTS
        private const val LOCKOUT_DURATION = TimeUnit.MINUTES.toMillis(AUTH_CONFIG.LOCKOUT_DURATION_MINUTES)
    }

    // Thread-safe rate limiting tracking
    private val loginAttempts = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Authenticates user with credentials and manages token storage.
     * Implements rate limiting and security validations.
     *
     * @param username User's username
     * @param password User's password
     * @return Flow emitting Result containing User on success or error details
     */
    fun login(username: String, password: String): Flow<Result<User>> = flow {
        try {
            // Validate input parameters
            if (username.isBlank() || password.isBlank()) {
                emit(Result.failure(IllegalArgumentException("Username and password cannot be empty")))
                return@flow
            }

            // Check rate limiting
            if (isUserLocked(username)) {
                emit(Result.failure(SecurityException("Too many login attempts. Please try again later.")))
                return@flow
            }

            // Execute login request
            val response = apiService.login(LoginRequest(username, password))
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Store tokens securely
                    encryptedPreferences.edit().apply {
                        putString(AUTH_TOKEN_KEY, authResponse.token)
                        putString(REFRESH_TOKEN_KEY, authResponse.token)
                        putLong("${AUTH_TOKEN_KEY}_expiry", System.currentTimeMillis() + TOKEN_EXPIRY_TIME)
                        apply()
                    }
                    
                    // Clear login attempts on successful login
                    loginAttempts.remove(username)
                    
                    emit(Result.success(authResponse.user))
                } ?: emit(Result.failure(Exception("Invalid response from server")))
            } else {
                // Track failed login attempt
                recordLoginAttempt(username)
                emit(Result.failure(Exception("Login failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Registers new user with comprehensive input validation.
     * Enforces password policy and secure token storage.
     *
     * @param username Desired username
     * @param email User's email address
     * @param password User's password
     * @return Flow emitting Result containing User on success or validation error
     */
    fun register(username: String, email: String, password: String): Flow<Result<User>> = flow {
        try {
            // Validate input parameters
            if (!isValidEmail(email)) {
                emit(Result.failure(IllegalArgumentException("Invalid email format")))
                return@flow
            }

            if (!isValidPassword(password)) {
                emit(Result.failure(IllegalArgumentException(
                    "Password must be ${AUTH_CONFIG.MIN_PASSWORD_LENGTH} characters long and contain uppercase, lowercase, number and special character"
                )))
                return@flow
            }

            // Execute registration request
            val response = apiService.register(RegisterRequest(username, email, password))
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Store tokens securely
                    encryptedPreferences.edit().apply {
                        putString(AUTH_TOKEN_KEY, authResponse.token)
                        putString(REFRESH_TOKEN_KEY, authResponse.token)
                        putLong("${AUTH_TOKEN_KEY}_expiry", System.currentTimeMillis() + TOKEN_EXPIRY_TIME)
                        apply()
                    }
                    emit(Result.success(authResponse.user))
                } ?: emit(Result.failure(Exception("Invalid response from server")))
            } else {
                emit(Result.failure(Exception("Registration failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Refreshes authentication token before expiry.
     * Implements secure token rotation.
     *
     * @return Flow emitting Result containing new auth token or error
     */
    fun refreshAuthToken(): Flow<Result<String>> = flow {
        try {
            val refreshToken = encryptedPreferences.getString(REFRESH_TOKEN_KEY, null)
                ?: throw SecurityException("No refresh token available")

            val response = apiService.refreshToken(refreshToken)
            
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Update stored tokens
                    encryptedPreferences.edit().apply {
                        putString(AUTH_TOKEN_KEY, authResponse.token)
                        putString(REFRESH_TOKEN_KEY, authResponse.token)
                        putLong("${AUTH_TOKEN_KEY}_expiry", System.currentTimeMillis() + TOKEN_EXPIRY_TIME)
                        apply()
                    }
                    emit(Result.success(authResponse.token))
                } ?: emit(Result.failure(Exception("Invalid response from server")))
            } else {
                emit(Result.failure(Exception("Token refresh failed: ${response.message()}")))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Retrieves stored authentication token if valid.
     * Handles token expiry and refresh.
     *
     * @return Current valid auth token or null if expired/invalid
     */
    fun getStoredToken(): String? {
        val token = encryptedPreferences.getString(AUTH_TOKEN_KEY, null)
        val expiry = encryptedPreferences.getLong("${AUTH_TOKEN_KEY}_expiry", 0)
        
        return when {
            token == null -> null
            System.currentTimeMillis() >= expiry -> null
            else -> token
        }
    }

    /**
     * Securely clears all authentication data.
     * Implements secure data removal.
     */
    fun clearAuth() {
        encryptedPreferences.edit().apply {
            remove(AUTH_TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            remove("${AUTH_TOKEN_KEY}_expiry")
            apply()
        }
    }

    /**
     * Checks if user is currently authenticated.
     *
     * @return true if valid auth token exists
     */
    fun isLoggedIn(): Boolean {
        return getStoredToken() != null
    }

    /**
     * Validates email format.
     *
     * @param email Email to validate
     * @return true if email format is valid
     */
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
        )
        return emailPattern.matcher(email).matches()
    }

    /**
     * Validates password against security policy.
     *
     * @param password Password to validate
     * @return true if password meets security requirements
     */
    private fun isValidPassword(password: String): Boolean {
        val passwordPattern = Pattern.compile(AUTH_CONFIG.PASSWORD_PATTERN)
        return password.length >= AUTH_CONFIG.MIN_PASSWORD_LENGTH &&
               password.length <= AUTH_CONFIG.MAX_PASSWORD_LENGTH &&
               passwordPattern.matcher(password).matches()
    }

    /**
     * Records failed login attempt for rate limiting.
     *
     * @param username Username attempting login
     */
    private fun recordLoginAttempt(username: String) {
        loginAttempts.compute(username) { _, attempts ->
            (attempts ?: mutableListOf()).apply {
                add(System.currentTimeMillis())
                removeAll { it < System.currentTimeMillis() - LOCKOUT_DURATION }
            }
        }
    }

    /**
     * Checks if user is locked out due to too many failed attempts.
     *
     * @param username Username to check
     * @return true if user is currently locked out
     */
    private fun isUserLocked(username: String): Boolean {
        return loginAttempts[username]?.size ?: 0 >= MAX_LOGIN_ATTEMPTS
    }
}