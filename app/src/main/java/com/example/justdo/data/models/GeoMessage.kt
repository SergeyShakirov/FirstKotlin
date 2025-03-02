package com.example.justdo.data.models

import com.google.android.gms.maps.model.LatLng
import java.util.*

/**
 * Модель данных для геолокационного сообщения.
 * Такие сообщения привязаны к определенной точке на карте и видны
 * пользователям, находящимся в пределах указанного радиуса.
 */
data class GeoMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val location: LatLng = LatLng(0.0, 0.0),
    val radiusMeters: Double = 500.0,
    val avatarUrl: String? = null
)