package com.breakpoint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.math.abs

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
    private fun SpaceDto.toSpaceItem(): SpaceItem {
        val hourlyPrice = try {
            // Backend expone 'price' como decimal (string). Es precio por hora.
            (price ?: "0").toDouble().toInt()
        } catch (_: Throwable) { 0 }
        val latLng = parseGeoToLatLng(geo)
        return SpaceItem(
            id = id,
            title = title,
            imageUrl = imageUrl,
            address = geo.orEmpty(),
            hour = "",
            rating = rating_avg ?: 0.0,
            price = hourlyPrice,
            subtitle = subtitle,
            geo = geo,
            latitude = latLng?.first,
            longitude = latLng?.second
        )
    }

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

    suspend fun getSpaces(): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.getSpaces().map { it.toSpaceItem() }
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
                    val jsonStart = raw.indexOf('"')
                    // Fallback simple si no es JSON estándar
                    if (raw.contains("\"message\"")) {
                        val key = "\"message\""
                        val idx = raw.indexOf(key)
                        if (idx >= 0) raw.substring(idx + key.length).trim() else raw
                    } else raw
                } catch (_: Throwable) { raw }

                // Horario ocupado: mapear a mensaje en español para la UI
                if (code == 400 && backendMessage.contains("Time slot not available", ignoreCase = true)) {
                    return@withContext Result.failure(IllegalStateException("Esa hora no está disponible. Por favor selecciona otra."))
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


