package com.breakpoint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
        return SpaceItem(
            id = id,
            title = title,
            address = geo ?: "",
            hour = "",
            rating = rating_avg ?: 0.0,
            price = hourlyPrice
        )
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
}


