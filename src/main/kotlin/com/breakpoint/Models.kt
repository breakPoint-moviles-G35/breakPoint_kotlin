package com.breakpoint

data class SpaceItem(
    val id: String,
    val title: String,
    val address: String,
    val hour: String,
    val rating: Double,
    val price: Int
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
