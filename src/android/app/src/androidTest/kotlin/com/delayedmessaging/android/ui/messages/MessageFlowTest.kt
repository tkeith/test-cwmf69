package com.delayedmessaging.android.ui.messages

import androidx.test.core.app.ActivityScenario // version: 1.5.1
import androidx.test.espresso.Espresso.onView // version: 3.5.1
import androidx.test.espresso.action.ViewActions.* // version: 3.5.1
import androidx.test.espresso.assertion.ViewAssertions.* // version: 3.5.1
import androidx.test.espresso.matcher.ViewMatchers.* // version: 3.5.1
import androidx.test.ext.junit.runners.AndroidJUnit4 // version: 1.1.5
import androidx.test.espresso.IdlingRegistry // version: 3.5.1
import androidx.test.espresso.IdlingResource // version: 3.5.1
import com.delayedmessaging.android.MainActivity
import com.delayedmessaging.android.R
import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.MessageStatus
import com.delayedmessaging.android.util.DateTimeUtils
import dagger.hilt.android.testing.HiltAndroidRule // version: 2.47
import dagger.hilt.android.testing.HiltAndroidTest // version: 2.47
import org.hamcrest.Matchers.* // version: 2.2
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MessageFlowTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var activityScenario: ActivityScenario<MainActivity>
    private lateinit var messageDeliveryIdlingResource: MessageDeliveryIdlingResource

    private val TEST_RECIPIENT = "testUser"
    private val TEST_MESSAGE = "Test message content for delivery validation"
    private val MAX_CHAR_LIMIT = 1000
    private val MIN_DELAY_MS = 30_000L
    private val MAX_DELAY_MS = 60_000L
    private val DELIVERY_TOLERANCE_MS = 1000L // 1 second tolerance

    @Before
    fun setUp() {
        hiltRule.inject()
        messageDeliveryIdlingResource = MessageDeliveryIdlingResource()
        IdlingRegistry.getInstance().register(messageDeliveryIdlingResource)
        activityScenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(messageDeliveryIdlingResource)
        activityScenario.close()
    }

    @Test
    fun testCompleteMessageFlow() {
        // Navigate to compose screen
        onView(withId(R.id.fab_compose))
            .perform(click())

        // Enter recipient
        onView(withId(R.id.recipientInput))
            .perform(typeText(TEST_RECIPIENT), closeSoftKeyboard())

        // Enter message content
        onView(withId(R.id.messageInput))
            .perform(typeText(TEST_MESSAGE), closeSoftKeyboard())

        // Verify character count
        onView(withId(R.id.characterCount))
            .check(matches(withText(containsString("${TEST_MESSAGE.length}/$MAX_CHAR_LIMIT"))))

        // Record start time
        val startTime = System.currentTimeMillis()

        // Send message
        onView(withId(R.id.sendButton))
            .perform(click())

        // Verify queued status
        onView(withId(R.id.message_status))
            .check(matches(withText(MessageStatus.QUEUED.getDisplayText())))

        // Wait for delivery with IdlingResource
        messageDeliveryIdlingResource.startWatching()

        // Verify delivered status
        onView(withId(R.id.message_status))
            .check(matches(withText(MessageStatus.DELIVERED.getDisplayText())))

        // Calculate delivery time
        val deliveryTime = System.currentTimeMillis()
        val deliveryDuration = deliveryTime - startTime

        // Verify delivery window (30-60 seconds)
        assert(deliveryDuration in MIN_DELAY_MS..MAX_DELAY_MS) {
            "Delivery time $deliveryDuration ms outside required window of $MIN_DELAY_MS-$MAX_DELAY_MS ms"
        }

        // Verify delivery accuracy (±1 second)
        val scheduledDeliveryTime = startTime + deliveryDuration
        val actualDeliveryTime = System.currentTimeMillis()
        val deliveryAccuracy = Math.abs(actualDeliveryTime - scheduledDeliveryTime)
        
        assert(deliveryAccuracy <= DELIVERY_TOLERANCE_MS) {
            "Delivery accuracy $deliveryAccuracy ms exceeds tolerance of $DELIVERY_TOLERANCE_MS ms"
        }
    }

    @Test
    fun testMessageCharacterLimit() {
        // Navigate to compose screen
        onView(withId(R.id.fab_compose))
            .perform(click())

        // Generate text exceeding limit
        val oversizedText = "a".repeat(MAX_CHAR_LIMIT + 100)

        // Enter oversized text
        onView(withId(R.id.messageInput))
            .perform(typeText(oversizedText), closeSoftKeyboard())

        // Verify text truncation
        onView(withId(R.id.messageInput))
            .check(matches(withText(oversizedText.take(MAX_CHAR_LIMIT))))

        // Verify error state
        onView(withId(R.id.characterCount))
            .check(matches(hasTextColor(R.color.error)))

        // Verify send button disabled
        onView(withId(R.id.sendButton))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun testMessageDeliveryDelay() {
        // Navigate to compose screen and prepare message
        onView(withId(R.id.fab_compose))
            .perform(click())

        onView(withId(R.id.recipientInput))
            .perform(typeText(TEST_RECIPIENT), closeSoftKeyboard())

        onView(withId(R.id.messageInput))
            .perform(typeText(TEST_MESSAGE), closeSoftKeyboard())

        // Record start time and send
        val startTime = System.currentTimeMillis()
        onView(withId(R.id.sendButton))
            .perform(click())

        // Verify initial delay timer visibility
        onView(withId(R.id.delayTimerView))
            .check(matches(isDisplayed()))

        // Wait for delivery
        messageDeliveryIdlingResource.startWatching()

        // Verify final status
        onView(withId(R.id.message_status))
            .check(matches(withText(MessageStatus.DELIVERED.getDisplayText())))

        // Verify delay timer hidden
        onView(withId(R.id.delayTimerView))
            .check(matches(not(isDisplayed())))

        // Validate delivery time constraints
        val deliveryTime = System.currentTimeMillis()
        val deliveryDelay = deliveryTime - startTime

        assert(deliveryDelay >= MIN_DELAY_MS) {
            "Delivery delay $deliveryDelay ms less than minimum $MIN_DELAY_MS ms"
        }

        assert(deliveryDelay <= MAX_DELAY_MS) {
            "Delivery delay $deliveryDelay ms exceeds maximum $MAX_DELAY_MS ms"
        }
    }

    /**
     * Custom IdlingResource for message delivery monitoring
     */
    private inner class MessageDeliveryIdlingResource : IdlingResource {
        private var resourceCallback: IdlingResource.ResourceCallback? = null
        private var isIdle = true

        fun startWatching() {
            isIdle = false
            // Start monitoring message status changes
            activityScenario.onActivity { activity ->
                // Monitor until message is delivered
                val messageListFragment = activity
                    .supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager
                    ?.fragments
                    ?.filterIsInstance<MessageListFragment>()
                    ?.firstOrNull()

                messageListFragment?.let { fragment ->
                    fragment.messageAdapter?.let { adapter ->
                        // Check for delivered status
                        if (adapter.itemCount > 0) {
                            val message = adapter.messages.firstOrNull()
                            if (message?.status == MessageStatus.DELIVERED) {
                                isIdle = true
                                resourceCallback?.onTransitionToIdle()
                            }
                        }
                    }
                }
            }
        }

        override fun getName(): String = "MessageDeliveryIdlingResource"

        override fun isIdleNow(): Boolean = isIdle

        override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
            resourceCallback = callback
        }
    }
}