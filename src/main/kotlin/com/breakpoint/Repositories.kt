package com.breakpoint

import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.math.abs
import com.google.gson.JsonParser
import com.google.gson.JsonElement

class AuthRepository {
    suspend fun login(email: String, password: String): Result<UserDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = ApiProvider.auth.login(LoginRequest(email, password))
            ApiProvider.setToken(resp.access_token)
            Result.success(resp.user)
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 401) {
                Result.failure(IllegalStateException("Credenciales inválidas"))
            } else {
                Result.failure(t)
            }
        }
    }

    suspend fun register(email: String, password: String, name: String?, role: String?): Result<UserDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = ApiProvider.auth.register(RegisterRequest(email, password, name, role))
            Result.success(user)
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 409) {
                Result.failure(IllegalStateException("El correo ya está registrado"))
            } else {
                Result.failure(t)
            }
        }
    }

    suspend fun profile(): Result<UserDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = ApiProvider.auth.profile()
            Result.success(user)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class SpaceRepository {
    companion object {
        // Process-wide in-memory cache for spaces (persists across repository instances)
        private data class Cached<T>(val data: T, val timestampMs: Long)
        private val spacesCache = LruCache<String, Cached<List<SpaceDto>>>(2)
        private const val CACHE_TTL_MS: Long = 5 * 60 * 1000 // 5 minutes

        private fun isFresh(c: Cached<*>?): Boolean =
            c != null && (System.currentTimeMillis() - c.timestampMs) < CACHE_TTL_MS

        private fun putSpacesCache(key: String, data: List<SpaceDto>) {
            spacesCache.put(key, Cached(data, System.currentTimeMillis()))
        }

        fun invalidateSpacesCache() {
            spacesCache.evictAll()
        }
    }

    private fun SpaceDto.toNearestItem(): SpaceDto = this

    private fun parseGeoToLatLng(raw: String?): Pair<Double, Double>? {
        if (raw.isNullOrBlank()) return null
        val regex = Regex("-?\\d+(?:\\.\\d+)?")
        val components = regex.findAll(raw).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (components.size < 2) return null
        val a = components[0]
        val b = components[1]
        val lat: Double
        val lng: Double
        if (abs(a) > 90 && abs(b) <= 90) {
            lat = b; lng = a
        } else {
            lat = a; lng = b
        }
        return lat to lng
    }

    private fun SpaceDetailFullDto.toDetailedSpace(): DetailedSpace {
        val hourlyPrice = try { (price ?: "0").toDouble().toInt() } catch (_: Throwable) { 0 }
        val fullAddress = geo ?: ""
        val hostName = hostProfile?.id?.let { "Host ${it.take(4)}" } ?: "Host"
        return DetailedSpace(
            id = id,
            title = title,
            address = fullAddress,
            fullAddress = fullAddress,
            hour = "",
            rating = rating_avg ?: 0.0,
            reviewCount = bookings?.size ?: 0,
            price = hourlyPrice,
            description = rules ?: "",
            amenities = amenities ?: emptyList(),
            images = listOfNotNull(imageUrl),
            hostName = hostName,
            hostRating = (rating_avg ?: 0.0).coerceAtMost(5.0),
            availability = "",
            capacity = capacity,
            size = ""
        )
    }

    suspend fun getSpace(spaceId: String): Result<DetailedSpace> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dto = ApiProvider.space.getSpaceDetail(spaceId)
            Result.success(dto.toDetailedSpace())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getNearest(lat: Double, lng: Double): Result<SpaceDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dto = ApiProvider.space.nearest(lat, lng)
            Result.success(dto.toNearestItem())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getNearestList(lat: Double, lng: Double, limit: Int = 5): Result<List<SpaceDto>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.nearestList(lat, lng, limit)
            Result.success(list.map { it.toNearestItem() })
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getRecommendations(userId: String): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.recommendations(userId).map { it.toSpaceItem() }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getSpaces(): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val listDto = ApiProvider.space.getSpaces()
            putSpacesCache("spaces_all_v1", listDto)
            val list = listDto.map { it.toSpaceItem() }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getSpacesSorted(): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.getSpacesSorted().map { it.toSpaceItem() }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    // Cached sort-by-price: uses cached spaces if fresh, otherwise fetches and caches
    suspend fun getSpacesSortedByPriceCached(forceRefresh: Boolean = false): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val key = "spaces_all_v1"
            val cached = spacesCache.get(key)
            val source = if (!forceRefresh && isFresh(cached)) {
                cached!!.data
            } else {
                val fresh = ApiProvider.space.getSpaces()
                putSpacesCache(key, fresh)
                fresh
            }
            val sorted = source.sortedBy { dto ->
                try { (dto.price ?: "0").toDouble() } catch (_: Throwable) { Double.MAX_VALUE }
            }.map { it.toSpaceItem() }
            Result.success(sorted)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getAvailable(start: String, end: String): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.getAvailable(start, end).map { it.toSpaceItem() }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getPopularHours(spaceId: String): Result<List<Pair<Int, Int>>> = withContext(Dispatchers.IO) {
        // Returns list of (hourOfDay, count) sorted desc by count
        return@withContext try {
            val detail = ApiProvider.space.getSpaceDetail(spaceId)
            val counts = IntArray(24)
            fun parseDate(text: String): java.util.Date? {
                val patterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                )
                for (p in patterns) {
                    try {
                        val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                        return sdf.parse(text)
                    } catch (_: Throwable) {}
                }
                return null
            }
            detail.bookings.orEmpty().forEach { b ->
                try {
                    val start = parseDate(b.slot_start) ?: return@forEach
                    val end = parseDate(b.slot_end) ?: return@forEach
                    val cal = java.util.Calendar.getInstance().apply { time = start }
                    val calEnd = java.util.Calendar.getInstance().apply { time = end }
                    while (cal.before(calEnd)) {
                        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        counts[h] = counts[h] + 1
                        cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                    }
                } catch (_: Throwable) {}
            }
            val result = counts.mapIndexed { hour, c -> hour to c }.sortedByDescending { it.second }
            Result.success(result)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getHourlyHistogram(spaceId: String): Result<List<Int>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val detail = ApiProvider.space.getSpaceDetail(spaceId)
            val counts = IntArray(24)
            fun parseDate(text: String): java.util.Date? {
                val patterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                )
                for (p in patterns) {
                    try {
                        val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                        return sdf.parse(text)
                    } catch (_: Throwable) {}
                }
                return null
            }
            detail.bookings.orEmpty().forEach { b ->
                try {
                    val start = parseDate(b.slot_start) ?: return@forEach
                    val end = parseDate(b.slot_end) ?: return@forEach
                    val cal = java.util.Calendar.getInstance().apply { time = start }
                    val calEnd = java.util.Calendar.getInstance().apply { time = end }
                    while (cal.before(calEnd)) {
                        val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        counts[h] = counts[h] + 1
                        cal.add(java.util.Calendar.HOUR_OF_DAY, 1)
                    }
                } catch (_: Throwable) {}
            }
            Result.success(counts.toList())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun getWeekdayHistogram(spaceId: String): Result<List<Int>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val detail = ApiProvider.space.getSpaceDetail(spaceId)
            // Orden Lunes..Domingo
            val counts = IntArray(7)
            fun parseDate(text: String): java.util.Date? {
                val patterns = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                )
                for (p in patterns) {
                    try {
                        val sdf = java.text.SimpleDateFormat(p, java.util.Locale.US)
                        return sdf.parse(text)
                    } catch (_: Throwable) {}
                }
                return null
            }
            detail.bookings.orEmpty().forEach { b ->
                try {
                    val start = parseDate(b.slot_start) ?: return@forEach
                    val cal = java.util.Calendar.getInstance().apply { time = start }
                    // Calendar: Domingo=1...Sábado=7
                    val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // 1..7
                    val index = when (dow) {
                        java.util.Calendar.MONDAY -> 0
                        java.util.Calendar.TUESDAY -> 1
                        java.util.Calendar.WEDNESDAY -> 2
                        java.util.Calendar.THURSDAY -> 3
                        java.util.Calendar.FRIDAY -> 4
                        java.util.Calendar.SATURDAY -> 5
                        else -> 6 // Domingo
                    }
                    counts[index] = counts[index] + 1
                } catch (_: Throwable) {}
            }
            Result.success(counts.toList())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class HostRepository {
    suspend fun myProfile(): Result<HostProfileDetailDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val profile = ApiProvider.hostProfile.myProfile()
            Result.success(profile)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun listMySpaces(): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val profile = ApiProvider.hostProfile.myProfile()
            val spacesFromProfile = profile.spaces.orEmpty().map { it.toSpaceItem() }
            if (spacesFromProfile.isNotEmpty()) {
                Result.success(spacesFromProfile)
            } else {
                val spaces = ApiProvider.space.spacesByHost(profile.id).map { it.toSpaceItem() }
                Result.success(spaces)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    data class CreateSpaceInput(
        val title: String,
        val subtitle: String?,
        val geo: String?,
        val address: String?,
        val capacity: Int,
        val amenities: List<String>,
        val accessibility: List<String>,
        val imageUrl: String?,
        val rules: String?,
        val price: Double
    )

    suspend fun createSpace(input: CreateSpaceInput): Result<SpaceItem> = withContext(Dispatchers.IO) {
        return@withContext try {
            val profile = ApiProvider.hostProfile.myProfile()
            val body = CreateSpaceRequest(
                hostProfileId = profile.id,
                title = input.title,
                subtitle = input.subtitle,
                geo = input.geo ?: input.address,
                capacity = input.capacity,
                amenities = input.amenities.takeIf { it.isNotEmpty() },
                accessibility = input.accessibility.takeIf { it.isNotEmpty() },
                imageUrl = input.imageUrl,
                rules = input.rules,
                price = input.price
            )
            val created = ApiProvider.space.createSpace(body)
            Result.success(created.toSpaceItem())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updateSpacePrice(spaceId: String, price: Double): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            ApiProvider.space.updateSpace(spaceId, UpdateSpaceRequest(price = price))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun fetchBookingCount(spaceId: String): Result<Int> = withContext(Dispatchers.IO) {
        return@withContext try {
            val detail = ApiProvider.space.getSpaceDetail(spaceId)
            Result.success(detail.bookings?.size ?: 0)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class BookingRepository {
    suspend fun listMyBookings(): Result<List<BookingListItemDto>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.booking.listMine()
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun createBooking(
        spaceId: String,
        slotStartIso: String,
        slotEndIso: String,
        guestCount: Int
    ): Result<BookingDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dto = ApiProvider.booking.create(
                CreateBookingRequest(
                    spaceId = spaceId,
                    slotStart = slotStartIso,
                    slotEnd = slotEndIso,
                    guestCount = guestCount
                )
            )
            Result.success(dto)
        } catch (t: Throwable) {
            if (t is HttpException) {
                val code = t.code()
                val raw = try { t.response()?.errorBody()?.string().orEmpty() } catch (_: Throwable) { "" }
                val backendMessage = try {
                    // Intentar extraer el campo "message" del JSON de error de NestJS
                    val jsonEl: JsonElement = JsonParser.parseString(raw)
                    if (jsonEl.isJsonObject) {
                        val obj = jsonEl.asJsonObject
                        val msgEl = obj.get("message")
                        when {
                            msgEl == null || msgEl.isJsonNull -> raw
                            msgEl.isJsonPrimitive && msgEl.asJsonPrimitive.isString -> msgEl.asString
                            msgEl.isJsonArray && msgEl.asJsonArray.size() > 0 -> {
                                val first = msgEl.asJsonArray[0]
                                if (first.isJsonPrimitive && first.asJsonPrimitive.isString) first.asString else raw
                            }
                            else -> raw
                        }
                    } else raw
                } catch (_: Throwable) { raw }

                // Horario ocupado: mapear a mensaje en español para la UI
                if (code == 400 && backendMessage.contains("Time slot not available", ignoreCase = true)) {
                    return@withContext Result.failure(IllegalStateException("Esa hora no está disponible. Por favor selecciona otra."))
                }
                // Inicio en el pasado
                if (code == 400 && (
                        backendMessage.contains("hora de inicio ya ha pasado", ignoreCase = true) ||
                        backendMessage.contains("hora de inicio ya pasó", ignoreCase = true) ||
                        backendMessage.contains("start time has already passed", ignoreCase = true)
                    )) {
                    return@withContext Result.failure(IllegalStateException("La hora de inicio ya pasó. Elige otra hora."))
                }
                // Fechas inválidas
                if (code == 400 && (
                        backendMessage.contains("Invalid dates", ignoreCase = true) ||
                        backendMessage.contains("slotEnd must be after slotStart", ignoreCase = true)
                    )) {
                    return@withContext Result.failure(IllegalStateException("Las fechas seleccionadas no son válidas."))
                }
            }
            Result.failure(t)
        }
    }

    suspend fun findActiveNow(): Result<List<BookingListItemDto>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.booking.activeNow()
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun checkout(bookingId: String): Result<BookingDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val res = ApiProvider.booking.checkout(bookingId)
            Result.success(res)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun updateBooking(
        bookingId: String,
        slotStartIso: String? = null,
        slotEndIso: String? = null,
        status: String? = null
    ): Result<BookingDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val dto = ApiProvider.booking.update(
                bookingId,
                UpdateBookingRequest(slotStartIso, slotEndIso, status)
            )
            Result.success(dto)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = ApiProvider.booking.delete(bookingId)
            if (resp.isSuccessful) Result.success(Unit) else Result.failure(IllegalStateException("No se pudo eliminar"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

class ReviewRepository {
    suspend fun submit(spaceId: String, rating: Int, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            ApiProvider.review.create(CreateReviewRequest(space_id = spaceId, rating = rating, text = text))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}


