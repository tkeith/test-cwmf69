package com.delayedmessaging.android.util

import android.util.Log
import com.delayedmessaging.android.BuildConfig
import com.delayedmessaging.android.util.Constants.API_CONFIG
import java.util.UUID
import java.util.regex.Pattern

/**
 * Production-ready logging utility for the Delayed Messaging Android application.
 * Provides secure, optimized, and comprehensive logging capabilities with built-in
 * security controls and performance optimizations.
 *
 * @version 1.0
 */
object Logger {
    private const val TAG_PREFIX = "DelayedMsg-"
    private const val ENABLE_DEBUG_LOGS = BuildConfig.DEBUG
    private const val MAX_LOG_SIZE = 4000

    // Log levels for controlling log output
    private object LogLevel {
        const val DEBUG = 3
        const val INFO = 2
        const val WARN = 1
        const val ERROR = 0
    }

    // Patterns for masking sensitive data
    private object MaskPattern {
        const val CREDIT_CARD = "\\d{4}-\\d{4}-\\d{4}-(\\d{4})"
        const val EMAIL = "(\\w+)@\\w+\\.\\w+"
        const val SSN = "\\d{3}-\\d{2}-(\\d{4})"
        const val PHONE = "\\d{3}-\\d{3}-(\\d{4})"
        const val AUTH_TOKEN = "Bearer\\s+([\\w-]+\\.){2}[\\w-]+"
    }

    // Replacement patterns for masked data
    private object MaskReplacement {
        const val CREDIT_CARD = "****-****-****-$1"
        const val EMAIL = "****@$1"
        const val SSN = "***-**-$1"
        const val PHONE = "***-***-$1"
        const val AUTH_TOKEN = "Bearer ****"
    }

    /**
     * Logs debug messages when debug logging is enabled.
     * Only active in debug builds for performance optimization.
     *
     * @param tag Logging tag for message categorization
     * @param message Debug message to log
     */
    fun debug(tag: String, message: String) {
        if (ENABLE_DEBUG_LOGS) {
            try {
                val formattedTag = formatTag(tag)
                val sanitizedMessage = maskSensitiveData(message)
                logLongMessage(formattedTag, sanitizedMessage) { t, m ->
                    Log.d(t, m)
                }
            } catch (e: Exception) {
                error("Logger", "Failed to log debug message", e)
            }
        }
    }

    /**
     * Logs informational messages.
     *
     * @param tag Logging tag for message categorization
     * @param message Informational message to log
     */
    fun info(tag: String, message: String) {
        try {
            val formattedTag = formatTag(tag)
            val sanitizedMessage = maskSensitiveData(message)
            logLongMessage(formattedTag, sanitizedMessage) { t, m ->
                Log.i(t, m)
            }
        } catch (e: Exception) {
            error("Logger", "Failed to log info message", e)
        }
    }

    /**
     * Logs warning messages.
     *
     * @param tag Logging tag for message categorization
     * @param message Warning message to log
     */
    fun warn(tag: String, message: String) {
        try {
            val formattedTag = formatTag(tag)
            val sanitizedMessage = maskSensitiveData(message)
            logLongMessage(formattedTag, sanitizedMessage) { t, m ->
                Log.w(t, m)
            }
        } catch (e: Exception) {
            error("Logger", "Failed to log warning message", e)
        }
    }

    /**
     * Logs error messages with enhanced error tracking.
     *
     * @param tag Logging tag for message categorization
     * @param message Error message to log
     * @param throwable Optional exception for stack trace logging
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        try {
            val correlationId = UUID.randomUUID().toString()
            val threadInfo = Thread.currentThread().name
            val formattedTag = formatTag(tag)
            
            val errorMessage = buildString {
                append("CorrelationId: $correlationId")
                append(" | Thread: $threadInfo")
                append(" | Message: $message")
                throwable?.let {
                    append(" | Exception: ${it.javaClass.simpleName}")
                    append(" | Cause: ${it.message}")
                }
            }

            val sanitizedMessage = maskSensitiveData(errorMessage)
            logLongMessage(formattedTag, sanitizedMessage) { t, m ->
                if (throwable != null) {
                    Log.e(t, m, throwable)
                } else {
                    Log.e(t, m)
                }
            }
        } catch (e: Exception) {
            // Fallback error logging
            Log.e(TAG_PREFIX + "Logger", "Critical error in logging", e)
        }
    }

    /**
     * Formats the log tag with consistent prefix and validation.
     *
     * @param tag Base tag to format
     * @return Formatted tag with prefix
     */
    private fun formatTag(tag: String): String {
        require(tag.isNotBlank()) { "Tag cannot be empty" }
        return TAG_PREFIX + tag.take(23) // Ensure total tag length <= 23 characters (Android limit)
    }

    /**
     * Masks sensitive information in log messages.
     *
     * @param message Original message to sanitize
     * @return Sanitized message with sensitive data masked
     */
    private fun maskSensitiveData(message: String): String {
        var sanitizedMessage = message
        
        // Apply masking patterns
        with(sanitizedMessage) {
            sanitizedMessage = replace(Pattern.compile(MaskPattern.CREDIT_CARD), MaskReplacement.CREDIT_CARD)
            sanitizedMessage = replace(Pattern.compile(MaskPattern.EMAIL), MaskReplacement.EMAIL)
            sanitizedMessage = replace(Pattern.compile(MaskPattern.SSN), MaskReplacement.SSN)
            sanitizedMessage = replace(Pattern.compile(MaskPattern.PHONE), MaskReplacement.PHONE)
            sanitizedMessage = replace(Pattern.compile(MaskPattern.AUTH_TOKEN), MaskReplacement.AUTH_TOKEN)
        }

        return sanitizedMessage
    }

    /**
     * Handles logging of messages that exceed Android's log message size limit.
     *
     * @param tag Formatted tag for the log
     * @param message Message to log
     * @param logFunction Function to perform the actual logging
     */
    private inline fun logLongMessage(
        tag: String,
        message: String,
        logFunction: (String, String) -> Unit
    ) {
        if (message.length > MAX_LOG_SIZE) {
            val chunkCount = message.length / MAX_LOG_SIZE + 1
            for (i in 0 until chunkCount) {
                val start = i * MAX_LOG_SIZE
                val end = minOf(start + MAX_LOG_SIZE, message.length)
                logFunction(tag, "[$i/$chunkCount] ${message.substring(start, end)}")
            }
        } else {
            logFunction(tag, message)
        }
    }
}