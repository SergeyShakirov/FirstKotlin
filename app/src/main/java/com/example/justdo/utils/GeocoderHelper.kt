package com.example.justdo.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume

/**
 * Класс для обратного геокодирования координат в названия мест
 */
class GeocoderHelper(private val context: Context) {
    // Кэш для хранения результатов геокодирования
    private val locationCache = mutableMapOf<String, String>()

    /**
     * Получает название местоположения по координатам
     * @param location координаты в формате LatLng
     * @return Название местоположения или строка с координатами, если геокодирование не удалось
     */
    suspend fun getLocationName(location: LatLng): String {
        // Создаем ключ для кэша, округляя координаты до 3 знаков после запятой
        val cacheKey = getCacheKey(location)

        // Проверяем, есть ли результат в кэше
        locationCache[cacheKey]?.let { return it }

        return try {
            withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Используем разные подходы в зависимости от версии Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Для Android 13 (API 33) и выше используем асинхронный API
                    getFromLocationAsync(geocoder, location.latitude, location.longitude)
                } else {
                    // Для более ранних версий Android используем старый синхронный API
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses?.isNotEmpty() == true) {
                        formatAddress(addresses[0])
                    } else {
                        getFallbackLocationName(location)
                    }
                }
            }.also {
                // Сохраняем результат в кэш
                locationCache[cacheKey] = it
            }
        } catch (e: IOException) {
            Log.e("GeocoderHelper", "Ошибка геокодирования", e)
            // В случае ошибки возвращаем запасной вариант
            getFallbackLocationName(location).also {
                // Сохраняем результат в кэш
                locationCache[cacheKey] = it
            }
        }
    }

    /**
     * Асинхронно получает адрес с помощью нового API для Android 13+
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getFromLocationAsync(
        geocoder: Geocoder,
        latitude: Double,
        longitude: Double
    ): String = suspendCancellableCoroutine { continuation ->
        try {
            // В новом API Tiramisu (Android 13+) используется лямбда-функция
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (addresses.isNotEmpty()) {
                    continuation.resume(formatAddress(addresses[0]))
                } else {
                    continuation.resume(getFallbackLocationName(LatLng(latitude, longitude)))
                }
            }
        } catch (e: Exception) {
            Log.e("GeocoderHelper", "Ошибка при асинхронном геокодировании", e)
            if (continuation.isActive) {
                continuation.resume(getFallbackLocationName(LatLng(latitude, longitude)))
            }
        }
    }

    /**
     * Форматирует адрес в читаемый формат
     * @param address объект Address с данными адреса
     * @return Отформатированная строка с адресом
     */
    private fun formatAddress(address: Address): String {
        // Город или район
        val locality = address.locality ?: address.subAdminArea ?: address.adminArea

        // Если удалось получить название населенного пункта
        if (locality != null) {
            // Добавляем дополнительные детали, если доступны
            val subLocality = address.subLocality
            val thoroughfare = address.thoroughfare

            return when {
                subLocality != null -> "$locality, $subLocality"
                thoroughfare != null -> "$locality, $thoroughfare"
                else -> locality
            }
        }

        // Если не удалось получить название места, используем страну или координаты
        return address.countryName ?: "Неизвестное место"
    }

    /**
     * Запасной вариант названия местоположения, если геокодирование не удалось
     */
    private fun getFallbackLocationName(location: LatLng): String {
        return "Рядом с ${location.latitude.toInt()}, ${location.longitude.toInt()}"
    }

    /**
     * Создает ключ для кэширования по координатам
     */
    private fun getCacheKey(location: LatLng): String {
        // Округляем координаты до 3 знаков после запятой для группировки близких мест
        val lat = (location.latitude * 1000).toInt() / 1000.0
        val lng = (location.longitude * 1000).toInt() / 1000.0
        return "${lat}_${lng}"
    }

    /**
     * Очищает кэш геокодирования
     */
    fun clearCache() {
        locationCache.clear()
    }
}