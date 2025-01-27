package com.delayedmessaging.android.viewmodel

import com.delayedmessaging.android.data.repository.AuthRepository
import com.delayedmessaging.android.domain.model.User
import com.delayedmessaging.android.domain.usecase.LoginUseCase
import com.delayedmessaging.android.ui.viewmodel.AuthUiState
import com.delayedmessaging.android.ui.viewmodel.AuthViewModel
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import com.google.common.truth.Truth.assertThat // version: 1.1.3
import io.mockk.coEvery // version: 1.13.5
import io.mockk.coVerify // version: 1.13.5
import io.mockk.mockk // version: 1.13.5
import io.mockk.slot // version: 1.13.5
import kotlinx.coroutines.ExperimentalCoroutinesApi // version: 1.6.4
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher // version: 1.6.4
import kotlinx.coroutines.test.runBlockingTest // version: 1.6.4
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class AuthViewModelTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val loginUseCase = mockk<LoginUseCase>()
    private val authRepository = mockk<AuthRepository>()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        // Default mock responses
        coEvery { authRepository.isLoggedIn() } returns false
        coEvery { authRepository.clearAuth() } returns Unit

        viewModel = AuthViewModel(loginUseCase, authRepository)
    }

    @After
    fun cleanup() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `login with valid credentials emits success state`() = runBlockingTest {
        // Given
        val username = "validUser"
        val password = "ValidPass123!"
        val mockUser = mockk<User>()
        
        coEvery { 
            loginUseCase.execute(username, password) 
        } returns flowOf(Result.success(mockUser))

        // When
        viewModel.login(username, password)

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Success::class.java)
        val successState = viewModel.uiState.value as AuthUiState.Success
        assertThat(successState.user).isEqualTo(mockUser)
    }

    @Test
    fun `login with invalid password shows appropriate error`() = runBlockingTest {
        // Test cases for password validation
        val testCases = listOf(
            "" to "Password cannot be empty",
            "short" to "Password must be at least ${AUTH_CONFIG.MIN_PASSWORD_LENGTH} characters",
            "a".repeat(65) to "Password cannot exceed ${AUTH_CONFIG.MAX_PASSWORD_LENGTH} characters",
            "nouppercaseornumber" to "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
        )

        testCases.forEach { (password, expectedError) ->
            // When
            viewModel.login("username", password)

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
            val errorState = viewModel.uiState.value as AuthUiState.Error
            assertThat(errorState.message).isEqualTo(expectedError)
        }
    }

    @Test
    fun `login enforces rate limiting`() = runBlockingTest {
        // Given
        val username = "testUser"
        val password = "ValidPass123!"
        
        // Simulate multiple failed login attempts
        repeat(AUTH_CONFIG.MAX_LOGIN_ATTEMPTS) {
            coEvery { 
                loginUseCase.execute(username, password) 
            } returns flowOf(Result.failure(Exception("Invalid credentials")))
            
            viewModel.login(username, password)
        }

        // When attempting one more login
        viewModel.login(username, password)

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
        val errorState = viewModel.uiState.value as AuthUiState.Error
        assertThat(errorState.message).contains("Too many login attempts")
    }

    @Test
    fun `logout clears auth state`() = runBlockingTest {
        // When
        viewModel.logout()

        // Then
        coVerify { authRepository.clearAuth() }
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Initial::class.java)
    }

    @Test
    fun `checkAuthState returns correct authentication status`() = runBlockingTest {
        // Given
        coEvery { authRepository.isLoggedIn() } returns true

        // When
        val result = viewModel.checkAuthState()

        // Then
        assertThat(result).isTrue()
        coVerify { authRepository.isLoggedIn() }
    }

    @Test
    fun `login shows loading state during authentication`() = runBlockingTest {
        // Given
        val username = "testUser"
        val password = "ValidPass123!"
        
        coEvery { 
            loginUseCase.execute(username, password) 
        } returns flowOf(Result.success(mockk()))

        // When
        var loadingObserved = false
        viewModel.uiState.collect { state ->
            if (state is AuthUiState.Loading) {
                loadingObserved = true
            }
        }
        viewModel.login(username, password)

        // Then
        assertThat(loadingObserved).isTrue()
    }

    @Test
    fun `login with network error shows appropriate error message`() = runBlockingTest {
        // Given
        val username = "testUser"
        val password = "ValidPass123!"
        
        coEvery { 
            loginUseCase.execute(username, password) 
        } returns flowOf(Result.failure(Exception("Network error")))

        // When
        viewModel.login(username, password)

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
        val errorState = viewModel.uiState.value as AuthUiState.Error
        assertThat(errorState.message).isEqualTo("Invalid username or password")
    }

    @Test
    fun `login with invalid username shows appropriate error`() = runBlockingTest {
        // Test cases for username validation
        val testCases = listOf(
            "" to "Username cannot be empty",
            "ab" to "Username must be at least ${AUTH_CONFIG.MIN_USERNAME_LENGTH} characters",
            "a".repeat(31) to "Username cannot exceed ${AUTH_CONFIG.MAX_USERNAME_LENGTH} characters",
            "invalid@username" to "Username can only contain letters, numbers, dots, underscores and hyphens"
        )

        testCases.forEach { (username, expectedError) ->
            // When
            viewModel.login(username, "ValidPass123!")

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
            val errorState = viewModel.uiState.value as AuthUiState.Error
            assertThat(errorState.message).isEqualTo(expectedError)
        }
    }

    @Test
    fun `rate limit reset after successful login`() = runBlockingTest {
        // Given
        val username = "testUser"
        val password = "ValidPass123!"
        
        // Simulate failed attempts
        repeat(AUTH_CONFIG.MAX_LOGIN_ATTEMPTS - 1) {
            coEvery { 
                loginUseCase.execute(username, password) 
            } returns flowOf(Result.failure(Exception("Invalid credentials")))
            
            viewModel.login(username, password)
        }

        // When successful login occurs
        coEvery { 
            loginUseCase.execute(username, password) 
        } returns flowOf(Result.success(mockk()))
        
        viewModel.login(username, password)

        // Then should be able to attempt login again
        viewModel.login(username, password)
        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Success::class.java)
    }
}