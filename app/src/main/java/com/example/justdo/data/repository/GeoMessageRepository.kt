package com.example.justdo.data.repository

import android.util.Log
import com.example.justdo.data.models.GeoMessage
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Репозиторий для работы с геолокационными сообщениями.
 * Отвечает за отправку, получение и слежение за сообщениями в определенном радиусе.
 */
class GeoMessageRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "GeoMessageRepository"

    private val geoMessagesCollection = firestore.collection("geoMessages")

    /**
     * Отправляет новое сообщение с привязкой к геолокации
     */
    suspend fun sendGeoMessage(message: GeoMessage): Boolean {
        return try {
            val messageData = hashMapOf(
                "id" to message.id,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "latitude" to message.location.latitude,
                "longitude" to message.location.longitude,
                "radiusMeters" to message.radiusMeters,
                "avatarUrl" to message.avatarUrl
            )

            geoMessagesCollection.document(message.id)
                .set(messageData)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending geo message", e)
            false
        }
    }

    /**
     * Получает сообщения в пределах указанного радиуса от текущей локации
     */
    suspend fun getNearbyMessages(location: LatLng, radiusMeters: Double): List<GeoMessage> {
        return try {
            // Получаем все сообщения и фильтруем на стороне клиента
            // В будущем можно оптимизировать с помощью GeoFirestore или Firebase Functions
            val snapshot = geoMessagesCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val latitude = doc.getDouble("latitude") ?: return@mapNotNull null
                    val longitude = doc.getDouble("longitude") ?: return@mapNotNull null
                    val messageLocation = LatLng(latitude, longitude)

                    // Проверяем, находится ли сообщение в указанном радиусе
                    if (isInRadius(location, messageLocation, radiusMeters)) {
                        GeoMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            location = messageLocation,
                            radiusMeters = doc.getDouble("radiusMeters") ?: 500.0,
                            avatarUrl = doc.getString("avatarUrl")
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapping geo message", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting nearby messages", e)
            emptyList()
        }
    }

    /**
     * Проверяет, находится ли точка в пределах указанного радиуса
     */
    private fun isInRadius(center: LatLng, point: LatLng, radiusMeters: Double): Boolean {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            center.latitude, center.longitude,
            point.latitude, point.longitude,
            results
        )
        return results[0] <= radiusMeters
    }

    /**
     * Настраивает слушатель для получения обновлений геосообщений в реальном времени
     */
    fun listenToNearbyMessages(
        location: LatLng,
        radiusMeters: Double,
        onMessagesUpdate: (List<GeoMessage>) -> Unit
    ): ListenerRegistration {
        return geoMessagesCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening to geo messages", e)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val latitude = doc.getDouble("latitude") ?: return@mapNotNull null
                        val longitude = doc.getDouble("longitude") ?: return@mapNotNull null
                        val messageLocation = LatLng(latitude, longitude)

                        // Фильтруем сообщения по радиусу
                        if (isInRadius(location, messageLocation, radiusMeters)) {
                            GeoMessage(
                                id = doc.id,
                                senderId = doc.getString("senderId") ?: "",
                                senderName = doc.getString("senderName") ?: "",
                                text = doc.getString("text") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                location = messageLocation,
                                radiusMeters = doc.getDouble("radiusMeters") ?: 500.0,
                                avatarUrl = doc.getString("avatarUrl")
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping real-time geo message", e)
                        null
                    }
                } ?: emptyList()

                onMessagesUpdate(messages)
            }
    }
}