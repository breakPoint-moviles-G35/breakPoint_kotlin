package com.breakpoint

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    suspend fun login(email: String, password: String): Result<UserDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val resp = ApiProvider.auth.login(LoginRequest(email, password))
            ApiProvider.setToken(resp.access_token)
            Result.success(resp.user)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun register(email: String, password: String, name: String?): Result<UserDto> = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = ApiProvider.auth.register(RegisterRequest(email, password, name))
            Result.success(user)
        } catch (t: Throwable) {
            Result.failure(t)
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
            val per30 = (base_price_per_30m ?: "0").toDouble()
            (per30 * 2).toInt()
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

    suspend fun getAvailable(start: String, end: String): Result<List<SpaceItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val list = ApiProvider.space.getAvailable(start, end).map { it.toSpaceItem() }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}


