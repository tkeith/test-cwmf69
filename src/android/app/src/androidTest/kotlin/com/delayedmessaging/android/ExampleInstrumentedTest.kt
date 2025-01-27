package com.delayedmessaging.android

import androidx.test.platform.app.InstrumentationRegistry // v1.5.1
import androidx.test.ext.junit.runners.AndroidJUnit4 // v1.1.5
import org.junit.Test // v4.13.2
import org.junit.Assert.* // v4.13.2
import org.junit.runner.RunWith // v4.13.2

/**
 * Instrumented test class that verifies basic Android application context
 * and package name functionality for the Delayed Messaging application.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    /**
     * Verifies that the application context returns the correct package name.
     * This test ensures the basic Android test infrastructure is working and
     * the application is properly configured.
     */
    @Test
    fun useAppContext() {
        // Get the instrumentation context for the Delayed Messaging application
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        // Verify the package name matches the expected value
        assertEquals("com.delayedmessaging.android", appContext.packageName)
    }
}