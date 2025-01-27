package com.delayedmessaging.android.di

import android.content.Context
import com.delayedmessaging.android.data.local.AppDatabase
import com.delayedmessaging.android.network.NetworkMonitor
import com.delayedmessaging.android.util.Constants.API_CONFIG
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import com.jakewharton.timber.Timber
import javax.inject.Singleton

/**
 * Dagger Hilt module providing application-level dependencies with enhanced network
 * and concurrency support for the Delayed Messaging application.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides singleton instance of Room database with offline support and encryption.
     *
     * @param context Application context for database initialization
     * @return Thread-safe, encrypted AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    /**
     * Provides application-wide coroutine scope with error handling and supervision.
     * Uses SupervisorJob to prevent child coroutine failures from affecting siblings.
     *
     * @return Application-scoped CoroutineScope with error handling
     */
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "Coroutine exception in application scope")
        }
        return CoroutineScope(SupervisorJob() + exceptionHandler)
    }

    /**
     * Provides network connectivity monitor with automatic retry capabilities.
     * Monitors network state changes and provides real-time connectivity updates.
     *
     * @param context Application context for network monitoring
     * @param applicationScope Coroutine scope for network operations
     * @return Singleton NetworkMonitor instance
     */
    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context,
        applicationScope: CoroutineScope
    ): NetworkMonitor {
        return NetworkMonitor(
            context = context,
            coroutineScope = applicationScope,
            connectTimeout = API_CONFIG.CONNECT_TIMEOUT,
            retryCount = API_CONFIG.RETRY_COUNT,
            retryDelay = API_CONFIG.RETRY_DELAY_MS
        )
    }

    /**
     * Provides message DAO for database operations with offline support.
     *
     * @param database Application database instance
     * @return Thread-safe MessageDao implementation
     */
    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase) = database.messageDao()

    /**
     * Provides user DAO for database operations with offline support.
     *
     * @param database Application database instance
     * @return Thread-safe UserDao implementation
     */
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase) = database.userDao()
}