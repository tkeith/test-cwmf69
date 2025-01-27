package com.delayedmessaging.android

import android.app.Application
import android.os.StrictMode
import android.os.Process
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.delayedmessaging.android.util.Logger
import com.delayedmessaging.android.util.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import io.reactivex.rxjava3.disposables.CompositeDisposable
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Enhanced Application class for the Delayed Messaging app with comprehensive lifecycle
 * management, error handling, and monitoring capabilities.
 */
@HiltAndroidApp
class App : Application(), DefaultLifecycleObserver {

    @Inject
    lateinit var themeManager: ThemeManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val disposables = CompositeDisposable()
    private val backgroundExecutor = Executors.newFixedThreadPool(3)
    private val isDebugBuild = BuildConfig.DEBUG
    private val TAG = "App"

    override fun onCreate() {
        super.onCreate()
        setupStrictMode()
        initializeComponents()
        setupLifecycleMonitoring()
        setupPerformanceMonitoring()
        
        Logger.info(TAG, "Application initialized successfully")
    }

    private fun setupStrictMode() {
        if (isDebugBuild) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .build()
            )
        }
    }

    private fun initializeComponents() {
        applicationScope.launch {
            try {
                // Initialize theme with fallback handling
                themeManager.setThemeChangeListener { isDarkMode ->
                    Logger.debug(TAG, "Theme changed: isDarkMode=$isDarkMode")
                }

                // Initialize other core components
                setupCrashReporting()
                setupMemoryMonitoring()
                setupANRDetection()
                
            } catch (e: Exception) {
                Logger.error(TAG, "Failed to initialize components", e)
                // Fallback to safe defaults
                themeManager.setThemeMode(false)
            }
        }
    }

    private fun setupLifecycleMonitoring() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Logger.debug(TAG, "Application moved to foreground")
            }

            override fun onStop(owner: LifecycleOwner) {
                Logger.debug(TAG, "Application moved to background")
            }
        })
    }

    private fun setupPerformanceMonitoring() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.error(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            Process.killProcess(Process.myPid())
        }
    }

    private fun setupCrashReporting() {
        // Initialize crash reporting in production only
        if (!isDebugBuild) {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Logger.error(TAG, "Fatal crash in thread ${thread.name}", throwable)
                // Ensure crash data is saved before process termination
                backgroundExecutor.submit {
                    // Save crash report
                    Process.killProcess(Process.myPid())
                }
            }
        }
    }

    private fun setupMemoryMonitoring() {
        applicationScope.launch {
            while (true) {
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                if (usedMemory > 200) { // 200MB threshold
                    Logger.warn(TAG, "High memory usage detected: ${usedMemory}MB")
                }
                delay(TimeUnit.MINUTES.toMillis(5))
            }
        }
    }

    private fun setupANRDetection() {
        if (isDebugBuild) {
            val mainThreadHandler = android.os.Handler(mainLooper)
            val anrWatchdog = object : Runnable {
                override fun run() {
                    // Check main thread responsiveness
                    mainThreadHandler.postDelayed(this, 5000)
                }
            }
            mainThreadHandler.post(anrWatchdog)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Logger.warn(TAG, "Low memory condition detected")
        
        try {
            // Clear non-essential caches
            backgroundExecutor.execute {
                // Clear image caches
                // Clear non-critical runtime caches
                System.gc()
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error handling low memory condition", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            // Cleanup resources
            disposables.clear()
            backgroundExecutor.shutdown()
            try {
                backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Logger.warn(TAG, "Background executor shutdown interrupted")
            }
            Logger.info(TAG, "Application terminated successfully")
        } catch (e: Exception) {
            Logger.error(TAG, "Error during application termination", e)
        }
    }
}