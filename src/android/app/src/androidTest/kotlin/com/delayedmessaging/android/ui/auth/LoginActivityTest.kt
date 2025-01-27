package com.delayedmessaging.android.ui.auth

import androidx.test.core.app.ActivityScenario // version: 1.5.1
import androidx.test.espresso.Espresso.onView // version: 3.5.1
import androidx.test.espresso.IdlingRegistry // version: 3.5.1
import androidx.test.espresso.action.ViewActions.* // version: 3.5.1
import androidx.test.espresso.assertion.ViewAssertions.matches // version: 3.5.1
import androidx.test.espresso.matcher.ViewMatchers.* // version: 3.5.1
import androidx.test.espresso.idling.CountingIdlingResource // version: 3.5.1
import com.delayedmessaging.android.R
import com.delayedmessaging.android.data.repository.AuthRepository
import com.delayedmessaging.android.util.Constants.AUTH_CONFIG
import com.delayedmessaging.android.util.Constants.UI_CONFIG
import dagger.hilt.android.testing.HiltAndroidRule // version: 2.44
import dagger.hilt.android.testing.HiltAndroidTest // version: 2.44
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import java.util.concurrent.TimeUnit

/**
 * Comprehensive instrumented test class for LoginActivity.
 * Verifies authentication flow, input validation, rate limiting, and UI state management.
 */
@HiltAndroidTest
class LoginActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var scenario: ActivityScenario<LoginActivity>
    private lateinit var idlingResource: CountingIdlingResource

    @Before
    fun setup() {
        hiltRule.inject()
        idlingResource = CountingIdlingResource("LoginTest")
        IdlingRegistry.getInstance().register(idlingResource)
        
        // Clear any existing auth state
        authRepository.clearAuth()
        
        // Launch activity
        scenario = ActivityScenario.launch(LoginActivity::class.java)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(idlingResource)
        scenario.close()
        authRepository.clearAuth()
    }

    @Test
    fun testEmptyUsernameShowsError() {
        // When: User attempts login with empty username
        onView(withId(R.id.loginButton))
            .perform(click())

        // Then: Username error is displayed
        onView(withId(R.id.usernameLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_username_empty)))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPasswordValidationRules() {
        // Given: Valid username
        onView(withId(R.id.usernameInput))
            .perform(typeText("validuser"), closeSoftKeyboard())

        // Test: Password too short
        onView(withId(R.id.passwordInput))
            .perform(typeText("short"), closeSoftKeyboard())
        onView(withId(R.id.loginButton))
            .perform(click())
        onView(withId(R.id.passwordLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_password_too_short)))

        // Test: Password missing uppercase
        onView(withId(R.id.passwordInput))
            .perform(clearText(), typeText("password123!"), closeSoftKeyboard())
        onView(withId(R.id.loginButton))
            .perform(click())
        onView(withId(R.id.passwordLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_password_requirements)))

        // Test: Password missing special character
        onView(withId(R.id.passwordInput))
            .perform(clearText(), typeText("Password123"), closeSoftKeyboard())
        onView(withId(R.id.loginButton))
            .perform(click())
        onView(withId(R.id.passwordLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_password_requirements)))
    }

    @Test
    fun testRateLimitingBehavior() {
        // Given: Invalid credentials
        val invalidCredentials = Pair("testuser", "WrongPass123!")

        // When: Attempting multiple rapid logins
        repeat(AUTH_CONFIG.MAX_LOGIN_ATTEMPTS + 1) {
            onView(withId(R.id.usernameInput))
                .perform(replaceText(invalidCredentials.first), closeSoftKeyboard())
            onView(withId(R.id.passwordInput))
                .perform(replaceText(invalidCredentials.second), closeSoftKeyboard())
            onView(withId(R.id.loginButton))
                .perform(click())

            // Wait for API response
            Thread.sleep(1000)
        }

        // Then: Rate limit error is displayed
        onView(withText(R.string.error_rate_limit))
            .check(matches(isDisplayed()))

        // And: Login button is disabled
        onView(withId(R.id.loginButton))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun testSuccessfulLoginFlow() {
        // Given: Valid credentials
        val validCredentials = Pair("testuser", "ValidPass123!")

        // When: Entering valid credentials
        onView(withId(R.id.usernameInput))
            .perform(typeText(validCredentials.first), closeSoftKeyboard())
        onView(withId(R.id.passwordInput))
            .perform(typeText(validCredentials.second), closeSoftKeyboard())

        // And: Clicking login
        onView(withId(R.id.loginButton))
            .perform(click())

        // Then: Loading indicator is shown
        onView(withId(R.id.loginProgress))
            .check(matches(isDisplayed()))

        // And: Login button is disabled during loading
        onView(withId(R.id.loginButton))
            .check(matches(not(isEnabled())))

        // Wait for login completion
        Thread.sleep(2000)

        // Verify: Auth token is stored
        assert(authRepository.isLoggedIn())
    }

    @Test
    fun testAccessibilityLabels() {
        // Verify content descriptions are set
        onView(withId(R.id.usernameInput))
            .check(matches(withContentDescription(R.string.username_input_description)))
        onView(withId(R.id.passwordInput))
            .check(matches(withContentDescription(R.string.password_input_description)))
        onView(withId(R.id.loginButton))
            .check(matches(withContentDescription(R.string.login_button_description)))
    }

    @Test
    fun testInputValidationRealTime() {
        // Test username validation on focus change
        onView(withId(R.id.usernameInput))
            .perform(typeText("a"), closeSoftKeyboard())
        onView(withId(R.id.passwordInput))
            .perform(click())
        onView(withId(R.id.usernameLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_username_too_short)))

        // Test password validation on focus change
        onView(withId(R.id.passwordInput))
            .perform(typeText("weak"), closeSoftKeyboard())
        onView(withId(R.id.usernameInput))
            .perform(click())
        onView(withId(R.id.passwordLayout))
            .check(matches(hasTextInputLayoutErrorText(R.string.error_password_too_short)))
    }

    private fun hasTextInputLayoutErrorText(expectedErrorText: Int): org.hamcrest.Matcher<android.view.View> {
        return object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("has error text: " + 
                    scenario.getActivity().getString(expectedErrorText))
            }

            override fun matchesSafely(view: android.view.View): Boolean {
                if (view !is com.google.android.material.textfield.TextInputLayout) {
                    return false
                }
                val error = view.error ?: return false
                val hint = scenario.getActivity().getString(expectedErrorText)
                return error.toString() == hint
            }
        }
    }
}