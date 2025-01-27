package com.delayedmessaging.android.domain.usecase

import com.delayedmessaging.android.data.repository.UserRepository
import com.delayedmessaging.android.domain.model.UserPresence
import com.delayedmessaging.android.util.Constants.USER_STATUS
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber // version: 5.0.1

/**
 * Use case implementing battery-efficient presence management with privacy controls.
 * Handles debouncing of presence updates and network-aware operation.
 *
 * @property userRepository Repository for managing user presence data
 */
class UpdatePresenceUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    companion object {
        private const val DEBOUNCE_TIMEOUT_MS = 500L
        private const val RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * Updates user presence status with battery optimization and error handling.
     * Implements debouncing to prevent excessive API calls and respects privacy settings.
     *
     * @param userId Unique identifier of the user
     * @param presence New presence status with privacy settings
     * @return Flow emitting Result of the presence update operation
     */
    operator fun invoke(
        userId: String,
        presence: UserPresence
    ): Flow<Result<Unit>> = flow {
        try {
            // Validate input parameters
            if (!validateInput(userId, presence)) {
                throw IllegalArgumentException("Invalid presence update parameters")
            }

            // Check network state before attempting update
            userRepository.networkState.collect { networkState ->
                when (networkState) {
                    is UserRepository.NetworkState.Connected -> {
                        // Apply debouncing to prevent frequent updates
                        flow { emit(presence) }
                            .debounce(DEBOUNCE_TIMEOUT_MS)
                            .collect { debouncedPresence ->
                                var retryCount = 0
                                var success = false

                                while (!success && retryCount < MAX_RETRY_ATTEMPTS) {
                                    try {
                                        userRepository.updateUserPresence(userId, debouncedPresence)
                                        success = true
                                        emit(Result.success(Unit))
                                    } catch (e: Exception) {
                                        retryCount++
                                        if (retryCount >= MAX_RETRY_ATTEMPTS) {
                                            throw e
                                        }
                                        kotlinx.coroutines.delay(RETRY_DELAY_MS * retryCount)
                                    }
                                }
                            }
                    }
                    is UserRepository.NetworkState.Disconnected -> {
                        // Cache presence update locally only when offline
                        userRepository.updateUserPresence(userId, presence)
                        emit(Result.success(Unit))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update presence status")
            emit(Result.failure(e))
        }
    }.catch { e ->
        Timber.e(e, "Error in presence update flow")
        emit(Result.failure(e))
    }

    /**
     * Validates presence update input parameters.
     * Ensures all required fields are present and valid.
     *
     * @param userId User identifier to validate
     * @param presence Presence object to validate
     * @return true if input is valid, false otherwise
     */
    private fun validateInput(userId: String, presence: UserPresence): Boolean {
        return when {
            userId.isBlank() -> false
            presence.userId != userId -> false
            !isValidStatus(presence.status) -> false
            presence.lastActive <= 0 -> false
            else -> true
        }
    }

    /**
     * Validates if the provided status is a valid USER_STATUS enum value.
     *
     * @param status Status to validate
     * @return true if status is valid, false otherwise
     */
    private fun isValidStatus(status: USER_STATUS): Boolean {
        return when (status) {
            USER_STATUS.ONLINE,
            USER_STATUS.AWAY,
            USER_STATUS.DO_NOT_DISTURB,
            USER_STATUS.OFFLINE,
            USER_STATUS.INACTIVE -> true
            else -> false
        }
    }
}