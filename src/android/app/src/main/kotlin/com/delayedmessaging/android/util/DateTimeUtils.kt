package com.delayedmessaging.android.util

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.security.SecureRandom
import java.util.logging.Logger
import java.util.Locale
import java.util.TimeZone

/**
 * Thread-safe utility class for high-precision date and time manipulation.
 * Handles message scheduling, delivery timing, and delay calculations with millisecond accuracy.
 * Version: 1.0
 */
object DateTimeUtils {
    private val logger = Logger.getLogger(DateTimeUtils::class.java.name)
    private val secureRandom = SecureRandom()
    private const val MIN_DELAY_MS = 30_000L // 30 seconds in milliseconds
    private const val MAX_DELAY_MS = 60_000L // 60 seconds in milliseconds
    private const val MILLIS_IN_SECOND = 1_000L
    
    // Thread-local date formatter for thread safety
    private val threadLocalFormatter = ThreadLocal.withInitial {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    /**
     * Calculates the delivery time for a message using cryptographically secure random delay.
     * @param creationTimeMillis The message creation timestamp in milliseconds
     * @return Calculated delivery time in milliseconds
     * @throws IllegalArgumentException if input timestamp is invalid
     */
    @Synchronized
    fun calculateDeliveryTime(creationTimeMillis: Long): Long {
        validateTimestamp(creationTimeMillis)
        
        val currentTime = getCurrentTimeMillis()
        if (creationTimeMillis < currentTime) {
            throw IllegalArgumentException("Creation time cannot be in the past")
        }

        // Generate secure random delay between MIN_DELAY_MS and MAX_DELAY_MS
        val randomDelayRange = MAX_DELAY_MS - MIN_DELAY_MS
        val randomDelay = MIN_DELAY_MS + (secureRandom.nextDouble() * randomDelayRange).toLong()
        
        logger.fine("Generated delay: $randomDelay ms")

        // Check for potential overflow
        if (Long.MAX_VALUE - creationTimeMillis < randomDelay) {
            throw ArithmeticException("Delivery time calculation would overflow")
        }

        val deliveryTime = creationTimeMillis + randomDelay
        logger.fine("Calculated delivery time: $deliveryTime")
        
        return deliveryTime
    }

    /**
     * Verifies if the delivery time falls within the valid 30-60 second window.
     * @param creationTimeMillis Message creation timestamp in milliseconds
     * @param deliveryTimeMillis Scheduled delivery timestamp in milliseconds
     * @return true if delivery time is valid, false otherwise
     */
    fun isValidDeliveryWindow(creationTimeMillis: Long, deliveryTimeMillis: Long): Boolean {
        validateTimestamp(creationTimeMillis)
        validateTimestamp(deliveryTimeMillis)

        val differenceMs = deliveryTimeMillis - creationTimeMillis
        val differenceSeconds = TimeUnit.MILLISECONDS.toSeconds(differenceMs)

        val isValid = differenceSeconds in 30..60
        logger.fine("Delivery window validation: $isValid (${differenceSeconds}s)")
        
        return isValid
    }

    /**
     * Formats timestamp with thread-safe locale support.
     * @param timestampMillis Timestamp to format in milliseconds
     * @return Formatted timestamp string in ISO-8601 format
     * @throws IllegalArgumentException if timestamp is invalid
     */
    fun formatMessageTimestamp(timestampMillis: Long): String {
        validateTimestamp(timestampMillis)
        
        return try {
            val formatter = threadLocalFormatter.get()
            formatter.format(Date(timestampMillis))
        } catch (e: Exception) {
            logger.warning("Error formatting timestamp: ${e.message}")
            throw IllegalArgumentException("Failed to format timestamp", e)
        }
    }

    /**
     * Calculates remaining time until message delivery with overflow protection.
     * @param scheduledDeliveryTimeMillis Scheduled delivery timestamp in milliseconds
     * @return Remaining milliseconds until delivery
     * @throws IllegalArgumentException if scheduled time is invalid
     */
    fun getRemainingDeliveryTime(scheduledDeliveryTimeMillis: Long): Long {
        validateTimestamp(scheduledDeliveryTimeMillis)
        
        val currentTime = getCurrentTimeMillis()
        if (scheduledDeliveryTimeMillis < currentTime) {
            return 0L
        }

        val remainingTime = scheduledDeliveryTimeMillis - currentTime
        logger.fine("Remaining delivery time: $remainingTime ms")
        
        return remainingTime
    }

    /**
     * Gets current system time with validation.
     * @return Current time in milliseconds
     * @throws RuntimeException if system time is invalid
     */
    fun getCurrentTimeMillis(): Long {
        val systemTime = System.currentTimeMillis()
        validateTimestamp(systemTime)
        
        return systemTime
    }

    /**
     * Validates timestamp for basic sanity checks.
     * @param timestampMillis Timestamp to validate
     * @throws IllegalArgumentException if timestamp is invalid
     */
    private fun validateTimestamp(timestampMillis: Long) {
        if (timestampMillis < 0) {
            throw IllegalArgumentException("Timestamp cannot be negative")
        }
        
        // Basic sanity check for timestamp (year 2000 to 2100)
        val minValidTime = 946684800000L  // 2000-01-01
        val maxValidTime = 4102444800000L // 2100-01-01
        
        if (timestampMillis < minValidTime || timestampMillis > maxValidTime) {
            throw IllegalArgumentException("Timestamp outside valid range")
        }
    }

    /**
     * Converts milliseconds to seconds with proper rounding.
     * @param milliseconds Time in milliseconds
     * @return Time in seconds
     */
    private fun millisecondsToSeconds(milliseconds: Long): Long {
        return TimeUnit.MILLISECONDS.toSeconds(milliseconds)
    }
}