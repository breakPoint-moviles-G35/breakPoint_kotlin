package com.breakpoint

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.Response
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
    val subtitle: String? = null,
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

// Full detail mirroring backend Space entity (only fields we use on the app)
data class HostProfileDto(
    val id: String?,
    val verification_status: String?,
    val payout_method: String?
)

data class SpaceDetailFullDto(
    val id: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val geo: String?,
    val capacity: Int,
    val amenities: List<String>?,
    val accessibility: List<String>?,
    val rules: String?,
    val price: String?,
    val rating_avg: Double?,
    val bookings: List<SpaceBookingDto>?,
    val hostProfile: HostProfileDto?
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

data class UpdateBookingRequest(
    val slotStart: String? = null,
    val slotEnd: String? = null,
    val status: String? = null
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
    suspend fun getSpaceDetail(@Path("id") id: String): SpaceDetailFullDto

    @GET("space/nearest")
    suspend fun nearest(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double
    ): SpaceDto

    @GET("space/nearest/list")
    suspend fun nearestList(
        @Query("latitude") lat: Double,
        @Query("longitude") lng: Double,
        @Query("limit") limit: Int = 5
    ): List<SpaceDto>

    @GET("space/recommendations/{userId}")
    suspend fun recommendations(@Path("userId") userId: String): List<SpaceDto>
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

    @PATCH("booking/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateBookingRequest): BookingDto

    @DELETE("booking/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}

interface ReviewApi {
    @POST("review")
    suspend fun create(@Body body: CreateReviewRequest): ReviewDto
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

// Review DTOs
data class CreateReviewRequest(
    val space_id: String,
    val rating: Int,
    val text: String,
    val flags: Int? = null
)

data class ReviewDto(
    val id: String?,
    val space_id: String?,
    val user_id: String?,
    val rating: Int?,
    val text: String?
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
    // URL por defecto (puede ser cambiada en runtime desde la app)
    @Volatile private var baseUrl: String = "http://192.168.68.121:3000/"

    @Volatile private var authToken: String? = null
    @Volatile private var onUnauthorized: (() -> Unit)? = null
    @Volatile private var suppressUnauthorizedNav: Boolean = false

    // Event bus simple para navegación post-checkout
    private val _checkoutFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val checkoutFlow = _checkoutFlow.asSharedFlow()

    fun setToken(token: String?) { authToken = token }
    fun setOnUnauthorized(handler: (() -> Unit)?) { onUnauthorized = handler }
    fun currentToken(): String? = authToken
    fun setSuppressUnauthorizedNav(suppress: Boolean) { suppressUnauthorizedNav = suppress }
    fun triggerCheckoutSuccess(spaceId: String) { _checkoutFlow.tryEmit(spaceId) }

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

    @Volatile private var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Volatile var auth: AuthApi = retrofit.create(AuthApi::class.java)
        private set
    @Volatile var space: SpaceApi = retrofit.create(SpaceApi::class.java)
        private set
    @Volatile var booking: BookingApi = retrofit.create(BookingApi::class.java)
        private set
    @Volatile var review: ReviewApi = retrofit.create(ReviewApi::class.java)
        private set

    @Synchronized
    fun updateBaseUrl(url: String) {
        if (url.isBlank() || url == baseUrl) return
        baseUrl = if (url.endsWith('/')) url else "$url/"
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        auth = retrofit.create(AuthApi::class.java)
        space = retrofit.create(SpaceApi::class.java)
        booking = retrofit.create(BookingApi::class.java)
        review = retrofit.create(ReviewApi::class.java)
    }

    fun currentBaseUrl(): String = baseUrl

    suspend fun testConnectivity(url: String): Boolean = withContext(Dispatchers.IO) {
        val base = if (url.endsWith('/')) url else "$url/"
        val testUrl = base + "space"
        return@withContext try {
            val testClient = OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(3))
                .readTimeout(java.time.Duration.ofSeconds(3))
                .build()
            val req = Request.Builder().url(testUrl).get().build()
            val resp = testClient.newCall(req).execute()
            resp.use { true }
        } catch (_: Throwable) {
            false
        }
    }
}
