package com.delayedmessaging.android.util

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe manager for application theme settings with Material Design 3 support.
 * Handles theme switching between light and dark modes with system default fallback.
 *
 * @property context Application context for theme resources and configuration
 * @version 1.0
 */
class ThemeManager private constructor(private val context: Context) {

    private val preferenceManager = PreferenceManager.getInstance(context)
    private val mutex = Mutex()
    private val TAG = "ThemeManager"
    private val isThemeChanging = AtomicBoolean(false)
    private var themeChangeListener: ((Boolean) -> Unit)? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    init {
        Logger.debug(TAG, "Initializing ThemeManager")
        initializeTheme()
    }

    /**
     * Sets the application theme mode with thread safety and error handling.
     *
     * @param isDarkMode Boolean indicating whether to enable dark mode
     */
    suspend fun setThemeMode(isDarkMode: Boolean) = mutex.withLock {
        try {
            if (isThemeChanging.compareAndSet(false, true)) {
                Logger.debug(TAG, "Setting theme mode: isDarkMode=$isDarkMode")
                
                // Save theme preference
                preferenceManager.saveThemeMode(isDarkMode)
                
                // Apply theme mode
                val nightMode = if (isDarkMode) {
                    AppCompatDelegate.MODE_NIGHT_YES
                } else {
                    AppCompatDelegate.MODE_NIGHT_NO
                }
                
                AppCompatDelegate.setDefaultNightMode(nightMode)
                
                // Notify listeners
                themeChangeListener?.invoke(isDarkMode)
                
                Logger.debug(TAG, "Theme mode applied successfully")
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to set theme mode", e)
            throw e
        } finally {
            isThemeChanging.set(false)
        }
    }

    /**
     * Checks if the application is currently in dark mode.
     *
     * @return Boolean indicating whether dark mode is active
     */
    suspend fun isDarkMode(): Boolean = mutex.withLock {
        try {
            return preferenceManager.getThemeMode()
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to check dark mode status", e)
            return isSystemInDarkMode()
        }
    }

    /**
     * Checks if the system is currently in dark mode.
     *
     * @return Boolean indicating whether system is in dark mode
     */
    fun isSystemInDarkMode(): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    /**
     * Sets a listener for theme change events.
     *
     * @param listener Callback function receiving boolean indicating dark mode state
     */
    fun setThemeChangeListener(listener: ((Boolean) -> Unit)?) {
        themeChangeListener = listener
    }

    /**
     * Applies the saved theme mode or falls back to system default.
     */
    private fun initializeTheme() {
        coroutineScope.launch {
            try {
                val savedDarkMode = preferenceManager.getThemeMode()
                val nightMode = when {
                    savedDarkMode -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_NO
                }
                AppCompatDelegate.setDefaultNightMode(nightMode)
                Logger.debug(TAG, "Theme initialized: isDarkMode=$savedDarkMode")
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to initialize theme", e)
                // Fallback to system default
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ThemeManager? = null

        /**
         * Gets the singleton instance of ThemeManager.
         *
         * @param context Application context
         * @return ThemeManager instance
         */
        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }
}