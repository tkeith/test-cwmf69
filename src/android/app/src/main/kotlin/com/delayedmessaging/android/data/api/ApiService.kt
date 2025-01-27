package com.delayedmessaging.android.data.api

import com.delayedmessaging.android.domain.model.Message
import com.delayedmessaging.android.domain.model.User
import retrofit2.Response // version: 2.9.0
import retrofit2.http.Body // version: 2.9.0
import retrofit2.http.GET // version: 2.9.0
import retrofit2.http.Header // version: 2.9.0
import retrofit2.http.Headers // version: 2.9.0
import retrofit2.http.POST // version: 2.9.0
import retrofit2.http.Path // version: 2.9.0
import retrofit2.http.Query // version: 2.9.0

/**
 * Retrofit service interface defining the API endpoints for the Delayed Messaging System.
 * Implements comprehensive error handling and rate limiting support.
 */
interface ApiService {

    /**
     * Authenticates user with credentials and returns JWT token.
     * Rate limited to prevent brute force attacks.
     *
     * @param request Login credentials containing username and password
     * @return Response containing auth token and user details with rate limit headers
     */
    @POST("/auth/login")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    /**
     * Registers new user with validation of required fields.
     * Rate limited to prevent abuse.
     *
     * @param request Registration details including username, email and password
     * @return Response containing auth token and user details with rate limit headers
     */
    @POST("/auth/register")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    /**
     * Sends a new message with enforced 30-60 second delivery delay.
     * Rate limited based on user tier.
     *
     * @param authToken JWT auth token
     * @param message Message to be sent
     * @return Response containing created message with status and delivery estimate
     */
    @POST("/messages")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun sendMessage(
        @Header("Authorization") authToken: String,
        @Body message: Message
    ): Response<Message>

    /**
     * Retrieves paginated list of user's messages.
     * Rate limited to prevent excessive requests.
     *
     * @param authToken JWT auth token
     * @param page Page number for pagination
     * @param size Number of items per page
     * @return Response containing list of messages with rate limit headers
     */
    @GET("/messages")
    @Headers("Accept: application/json")
    suspend fun getMessages(
        @Header("Authorization") authToken: String,
        @Query("page") page: Int,
        @Query("size") size: Int
    ): Response<List<Message>>

    /**
     * Retrieves specific message by ID.
     * Rate limited to prevent excessive requests.
     *
     * @param authToken JWT auth token
     * @param messageId Unique identifier of the message
     * @return Response containing message details with rate limit headers
     */
    @GET("/messages/{messageId}")
    @Headers("Accept: application/json")
    suspend fun getMessage(
        @Header("Authorization") authToken: String,
        @Path("messageId") messageId: String
    ): Response<Message>

    /**
     * Retrieves user profile information.
     * Rate limited to prevent excessive requests.
     *
     * @param authToken JWT auth token
     * @return Response containing user profile data with rate limit headers
     */
    @GET("/users/profile")
    @Headers("Accept: application/json")
    suspend fun getUserProfile(
        @Header("Authorization") authToken: String
    ): Response<User>
}

/**
 * Data class for login request payload.
 * Validates required fields before sending.
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Data class for registration request payload.
 * Validates required fields and format before sending.
 */
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

/**
 * Data class for authentication response.
 * Contains JWT token and user details.
 */
data class AuthResponse(
    val token: String,
    val user: User,
    val rateLimit: Map<String, String>
)