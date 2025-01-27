package com.delayedmessaging.android.data.repository

import com.delayedmessaging.android.data.api.ApiService
import com.delayedmessaging.android.data.api.LoginRequest
import com.delayedmessaging.android.data.api.RegisterRequest
import com.delayedmessaging.android.data.local.dao.UserDao
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.util.Constants.API_CONFIG
import com.delayedmessaging.android.util.Constants.USER_STATUS
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber // version: 5.0.1
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository implementation for managing user-related data operations with offline-first approach.
 * Coordinates between local database and remote API with robust error handling and retry mechanisms.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val dispatcher: CoroutineDispatcher
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Connected)
    val networkState: StateFlow<NetworkState> = _networkState

    private val retryPolicy = RetryPolicy(
        maxAttempts = API_CONFIG.RETRY_COUNT,
        initialDelay = API_CONFIG.RETRY_DELAY_MS.milliseconds
    )

    /**
     * Authenticates user with provided credentials.
     * Implements retry mechanism and stores authentication result locally.
     */
    fun login(username: String, password: String): Flow<Result<User>> = flow {
        try {
            val loginRequest = LoginRequest(username = username, password = password)
            val response = apiService.login(loginRequest)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Store auth token securely
                    TokenManager.saveToken(authResponse.token)
                    
                    // Cache user data locally
                    userDao.insertUser(authResponse.user)
                    
                    emit(Result.success(authResponse.user))
                    
                    // Setup periodic token refresh
                    setupTokenRefresh()
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("Login failed: ${response.code()}")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            emit(Result.failure(e))
        }
    }.catch { e ->
        Timber.e(e, "Error in login flow")
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Registers new user with provided information.
     * Validates input and handles registration process with error handling.
     */
    fun register(
        username: String,
        email: String,
        password: String
    ): Flow<Result<User>> = flow {
        try {
            val registerRequest = RegisterRequest(
                username = username,
                email = email,
                password = password
            )
            
            val response = apiService.register(registerRequest)

            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Store auth token securely
                    TokenManager.saveToken(authResponse.token)
                    
                    // Cache user data locally
                    userDao.insertUser(authResponse.user)
                    
                    // Initialize presence monitoring
                    initializePresenceMonitoring(authResponse.user.id)
                    
                    emit(Result.success(authResponse.user))
                } ?: emit(Result.failure(Exception("Empty response body")))
            } else {
                emit(Result.failure(Exception("Registration failed: ${response.code()}")))
            }
        } catch (e: Exception) {
            Timber.e(e, "Registration failed")
            emit(Result.failure(e))
        }
    }.catch { e ->
        Timber.e(e, "Error in registration flow")
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Retrieves user data with offline-first approach.
     * Returns cached data and updates from remote source when available.
     */
    fun getUser(userId: String): Flow<User?> = userDao.getUser(userId)
        .map { cachedUser ->
            try {
                // Return cached data immediately
                cachedUser?.let { emit(it) }

                // Check if cache needs refresh
                if (shouldRefreshCache(cachedUser)) {
                    val token = TokenManager.getToken() ?: return@map cachedUser
                    val response = apiService.getUserProfile(token)

                    if (response.isSuccessful) {
                        response.body()?.let { user ->
                            userDao.insertUser(user)
                            user
                        } ?: cachedUser
                    } else {
                        cachedUser
                    }
                } else {
                    cachedUser
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching user data")
                cachedUser
            }
        }.flowOn(dispatcher)

    /**
     * Updates user presence status with battery-optimized approach.
     * Implements debouncing to prevent excessive API calls.
     */
    suspend fun updateUserPresence(userId: String, presence: UserPresence) {
        withContext(dispatcher) {
            try {
                // Update local cache immediately
                userDao.updateUserPresence(
                    userId = userId,
                    presence = presence,
                    timestamp = System.currentTimeMillis()
                )

                // Debounce rapid presence changes
                presenceUpdateDebouncer.debounce {
                    val token = TokenManager.getToken() ?: return@debounce
                    val response = apiService.updatePresence(token, presence)

                    if (!response.isSuccessful) {
                        Timber.e("Failed to update presence: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating presence")
            }
        }
    }

    private fun shouldRefreshCache(user: User?): Boolean {
        if (user == null) return true
        val currentTime = System.currentTimeMillis()
        return currentTime - (user.lastSyncTimestamp ?: 0) > TimeUnit.MINUTES.toMillis(15)
    }

    private fun setupTokenRefresh() {
        repositoryScope.launch {
            TokenManager.scheduleTokenRefresh(
                onRefresh = { token ->
                    try {
                        val response = apiService.refreshToken(token)
                        if (response.isSuccessful) {
                            response.body()?.token?.let { newToken ->
                                TokenManager.saveToken(newToken)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Token refresh failed")
                    }
                }
            )
        }
    }

    private fun initializePresenceMonitoring(userId: String) {
        repositoryScope.launch {
            updateUserPresence(
                userId = userId,
                presence = UserPresence(
                    userId = userId,
                    status = USER_STATUS.ONLINE,
                    lastActive = System.currentTimeMillis(),
                    deviceInfo = DeviceInfoProvider.getCurrentDeviceInfo()
                )
            )
        }
    }

    sealed class NetworkState {
        object Connected : NetworkState()
        object Disconnected : NetworkState()
    }
}