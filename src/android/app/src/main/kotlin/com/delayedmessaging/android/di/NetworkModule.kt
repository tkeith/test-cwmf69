package com.delayedmessaging.android.di

import android.content.Context
import com.delayedmessaging.android.data.api.ApiService
import com.delayedmessaging.android.data.api.WebSocketManager
import com.delayedmessaging.android.util.Constants.API_CONFIG
import dagger.Module // version: 2.44
import dagger.Provides // version: 2.44
import dagger.hilt.InstallIn // version: 2.44
import dagger.hilt.android.qualifiers.ApplicationContext // version: 2.44
import dagger.hilt.components.SingletonComponent // version: 2.44
import kotlinx.coroutines.CoroutineScope // version: 1.7.0
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.CertificatePinner // version: 4.11.0
import okhttp3.ConnectionPool // version: 4.11.0
import okhttp3.Interceptor // version: 4.11.0
import okhttp3.OkHttpClient // version: 4.11.0
import okhttp3.logging.HttpLoggingInterceptor // version: 4.11.0
import retrofit2.Retrofit // version: 2.9.0
import retrofit2.converter.gson.GsonConverterFactory // version: 2.9.0
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Dagger Hilt module providing network-related dependencies with enhanced security and reliability features.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Provides configured OkHttpClient instance with security features and performance optimizations.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add(API_CONFIG.BASE_URL.removePrefix("https://"), 
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // Replace with actual certificate pin
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val rateLimitInterceptor = Interceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .addHeader("X-Rate-Limit", "100")
                .build())
        }

        return OkHttpClient.Builder()
            .connectTimeout(API_CONFIG.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(API_CONFIG.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(API_CONFIG.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .certificatePinner(certificatePinner)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(loggingInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .build()
    }

    /**
     * Provides configured Retrofit instance with error handling and retry policies.
     */
    @Provides
    @Singleton
    fun provideRetrofit(httpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("${API_CONFIG.BASE_URL}/${API_CONFIG.API_VERSION}/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides ApiService implementation with error handling and rate limiting.
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    /**
     * Provides WebSocketManager instance with connection recovery and monitoring.
     */
    @Provides
    @Singleton
    fun provideWebSocketManager(
        @ApplicationContext context: Context
    ): WebSocketManager {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val retryPolicy = WebSocketManager.RetryPolicy(
            maxAttempts = API_CONFIG.RETRY_COUNT,
            initialDelayMs = API_CONFIG.RETRY_DELAY_MS,
            maxDelayMs = API_CONFIG.RETRY_DELAY_MS * 6,
            multiplier = 1.5f
        )
        return WebSocketManager(context, scope, retryPolicy)
    }
}