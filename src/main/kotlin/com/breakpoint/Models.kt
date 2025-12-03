package com.breakpoint

data class SpaceItem(
    val id: String,
    val title: String,
    val imageUrl: String?,
    val address: String,
    val hour: String,
    val rating: Double,
    val price: Int,
    val subtitle: String? = null,
    val geo: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val capacity: Int = 0 // Added capacity for filtering
    val amenities: List<String>? = null
)

data class DetailedSpace(
    val id: String,
    val title: String,
    val address: String,
    val fullAddress: String,
    val hour: String,
    val rating: Double,
    val reviewCount: Int,
    val price: Int,
    val description: String,
    val amenities: List<String>,
    val images: List<String>,
    val hostName: String,
    val hostRating: Double,
    val availability: String,
    val capacity: Int,
    val size: String
)

// Analytics for host: impact of amenities on reservations
data class AmenityImpact(
    val name: String,
    val rateWith: Double,
    val rateWithout: Double,
    val lift: Double,
    val spacesWith: Int,
    val spacesWithout: Int
)

data class ReservationItem(
    val title: String,
    val hour: String,
    val address: String,
    val rating: Double
)

data class ReservationData(
    val spaceId: String,
    val spaceTitle: String,
    val spacePrice: Int,
    val selectedDate: String,
    val selectedTime: String,
    val duration: Int, // en horas
    val totalPrice: Int,
    val guestCount: Int
)

// --- New Dashboard Data for Multithreading Strategy ---
data class SpaceDashboard(
    val all: List<SpaceItem>,
    val topRated: List<SpaceItem>,
    val budget: List<SpaceItem>,
    val bigGroups: List<SpaceItem>
)

fun SpaceItem.asSpaceDto(): SpaceDto {
    val priceText = if (price > 0) price.toString() else null
    return SpaceDto(
        id = id,
        title = title,
        imageUrl = imageUrl,
        subtitle = subtitle,
        geo = geo ?: address,
        capacity = capacity,
        amenities = null,
        accessibility = null,
        rules = null,
        price = priceText,
        rating_avg = rating
    )
}

fun SpaceDto.toSpaceItem(): SpaceItem {
    val priceInt = try {
        price?.toDouble()?.toInt() ?: 0
    } catch (_: Throwable) {
        0
    }
    val latLng = parseLatLngFromGeo(geo)
    return SpaceItem(
        id = id,
        title = title,
        imageUrl = imageUrl,
        address = geo.orEmpty(),
        hour = subtitle.orEmpty(),
        rating = rating_avg ?: 0.0,
        price = priceInt,
        subtitle = subtitle,
        geo = geo,
        latitude = latLng?.first,
        longitude = latLng?.second,
        capacity = capacity
        amenities = amenities
    )
}

private fun parseLatLngFromGeo(raw: String?): Pair<Double, Double>? {
    if (raw.isNullOrBlank()) return null
    val regex = Regex("-?\\d+(?:\\.\\d+)?")
    val values = regex.findAll(raw).mapNotNull { it.value.toDoubleOrNull() }.toList()
    if (values.size < 2) return null
    val a = values[0]
    val b = values[1]
    val lat: Double
    val lng: Double
    if (kotlin.math.abs(a) > 90 && kotlin.math.abs(b) <= 90) {
        lat = b; lng = a
    } else {
        lat = a; lng = b
    }
    return lat to lng
}
