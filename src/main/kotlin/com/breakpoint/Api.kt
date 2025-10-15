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
import retrofit2.http.PATCH
import retrofit2.http.DELETE
import retrofit2.Response
import com.google.gson.annotations.SerializedName

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

data class HostProfileDetailDto(
    val id: String?,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("ratingAverage") val ratingAverage: Double? = null,
    @SerializedName("totalReviews") val totalReviews: Int? = null
)

data class ReviewDto(
    val id: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("spaceId") val spaceId: String?
)

data class ReviewStatsDto(
    @SerializedName("average") val average: Double? = null,
    @SerializedName("avgRating") val avgRating: Double? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("reviews") val reviews: Int? = null
) {
    val resolvedAverage: Double
        get() = average ?: avgRating ?: rating ?: 0.0
    val resolvedCount: Int
        get() = count ?: total ?: reviews ?: 0
}

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

interface HostProfileApi {
    @GET("host-profile/{id}")
    suspend fun findById(@Path("id") id: String): HostProfileDetailDto

    @GET("host-profile")
    suspend fun listAll(): List<HostProfileDetailDto>

    @GET("host-profile/my-profile")
    suspend fun myProfile(): HostProfileDetailDto
}

interface ReviewApi {
    @GET("review/space/{spaceId}")
    suspend fun listForSpace(@Path("spaceId") spaceId: String): List<ReviewDto>

    @GET("review/space/{spaceId}/stats")
    suspend fun statsForSpace(@Path("spaceId") spaceId: String): ReviewStatsDto
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
    val hostProfile: HostProfileApi by lazy { retrofit.create(HostProfileApi::class.java) }
    val review: ReviewApi by lazy { retrofit.create(ReviewApi::class.java) }
}
