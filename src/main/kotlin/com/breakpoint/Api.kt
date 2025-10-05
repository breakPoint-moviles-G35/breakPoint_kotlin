package com.breakpoint

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Path

// DTOs
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val access_token: String, val user: UserDto)
data class RegisterRequest(val email: String, val password: String, val name: String? = null, val role: String? = null)

data class UserDto(
    val id: String,
    val email: String,
    val name: String?,
    val role: String?,
    val status: String?
)

data class SpaceDto(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val geo: String?,
    val capacity: Int,
    val amenities: List<String>?,
    val accessibility: List<String>?,
    val rules: String?,
    val price: String?,
    val rating_avg: Double?
)

// Detail DTOs (subset of backend response for /space/:id)
data class SpaceBookingDto(
    val slot_start: String,
    val slot_end: String
)

data class SpaceDetailDto(
    val id: String,
    val title: String,
    val bookings: List<SpaceBookingDto>?
)

// Booking
data class CreateBookingRequest(
    val spaceId: String,
    val slotStart: String,
    val slotEnd: String,
    val guestCount: Int
)

data class BookingDto(
    val id: String,
    val status: String
)

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): UserDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("auth/profile")
    suspend fun profile(): UserDto
}

interface SpaceApi {
    @GET("space")
    suspend fun getSpaces(): List<SpaceDto>

    @GET("space/sorted")
    suspend fun getSpacesSorted(): List<SpaceDto>

    @GET("space/available")
    suspend fun getAvailable(
        @Query("start") start: String,
        @Query("end") end: String
    ): List<SpaceDto>

    @GET("space/{id}")
    suspend fun getSpaceDetail(@Path("id") id: String): SpaceDetailDto
}

interface BookingApi {
    @POST("booking")
    suspend fun create(@Body body: CreateBookingRequest): BookingDto

    @GET("booking")
    suspend fun listMine(): List<BookingListItemDto>

    @GET("booking/active-now")
    suspend fun activeNow(): List<BookingListItemDto>

    @POST("booking/{id}/checkout")
    suspend fun checkout(@Path("id") id: String): BookingDto
}

data class BookingListItemDto(
    val id: String,
    val status: String,
    val slotStart: String,
    val slotEnd: String,
    val totalAmount: String?,
    val currency: String?,
    val space: SpaceSummaryDto?
)

data class SpaceSummaryDto(
    val id: String?,
    val title: String?,
    val imageUrl: String?,
    val price: String?,
    val capacity: Int?
)

class AuthorizationInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenProvider()
        val original = chain.request()
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else original
        return chain.proceed(request)
    }
}

object ApiProvider {
    // TODO: adjust baseUrl to your running backend
    private const val baseUrl = "http://10.0.2.2:3000/" // Android emulator to localhost

    @Volatile private var authToken: String? = null
    @Volatile private var onUnauthorized: (() -> Unit)? = null
    @Volatile private var suppressUnauthorizedNav: Boolean = false

    fun setToken(token: String?) { authToken = token }
    fun setOnUnauthorized(handler: (() -> Unit)?) { onUnauthorized = handler }
    fun currentToken(): String? = authToken
    fun setSuppressUnauthorizedNav(suppress: Boolean) { suppressUnauthorizedNav = suppress }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val response = chain.proceed(
                    chain.request().newBuilder().apply {
                        authToken?.let { addHeader("Authorization", "Bearer $it") }
                    }.build()
                )
                if (response.code == 401) {
                    if (!suppressUnauthorizedNav) onUnauthorized?.invoke()
                }
                response
            }
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val space: SpaceApi by lazy { retrofit.create(SpaceApi::class.java) }
    val booking: BookingApi by lazy { retrofit.create(BookingApi::class.java) }
}

