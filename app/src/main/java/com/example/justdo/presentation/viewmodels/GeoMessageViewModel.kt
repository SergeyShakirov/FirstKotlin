package com.example.justdo.presentation.viewmodels

import android.content.Context
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.justdo.data.models.GeoMessage
import com.example.justdo.data.models.User
import com.example.justdo.data.repository.GeoMessageRepository
import com.example.justdo.data.repository.UserRepository
import com.example.justdo.utils.GeocoderHelper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel для работы с геолокационными сообщениями.
 */
class GeoMessageViewModel(
    private val userRepository: UserRepository,
    private val geoMessageRepository: GeoMessageRepository,
    private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<GeoMessage>>(emptyList())
    val messages: StateFlow<List<GeoMessage>> = _messages

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = userRepository.currentUser

    // Группировка сообщений по местоположению
    private val _groupedMessages = MutableStateFlow<Map<String, List<GeoMessage>>>(emptyMap())
    val groupedMessages: StateFlow<Map<String, List<GeoMessage>>> = _groupedMessages

    private var messageListener: ListenerRegistration? = null

    // Firebase
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Геокодер для получения названий мест
    private val geocoderHelper = GeocoderHelper(context)

    init {
        viewModelScope.launch {
            try {
                getCurrentUser()
            } catch (e: Exception) {
                Log.e("GeoMessageViewModel", "Ошибка при инициализации", e)
            }
        }
    }

    /**
     * Получает текущего пользователя из Firebase
     */
    private suspend fun getCurrentUser() {
        userRepository.getCurrentUser()
    }

    /**
     * Устанавливает текущего пользователя
     */
    fun setCurrentUser(user: User?) {
        userRepository.setCurrentUser(user)
    }

    /**
     * Устанавливает текущее местоположение пользователя
     */
    fun setCurrentLocation(location: LatLng) {
        viewModelScope.launch {
            val previousLocation = _currentLocation.value
            _currentLocation.value = location

            // Если локация изменилась значительно или это первое местоположение,
            // обновляем сообщения и подписываемся на изменения
            if (previousLocation == null ||
                calculateDistance(previousLocation, location) > 100) { // 100 метров
                stopMessageListener()
                startMessageListener(location)
                loadNearbyMessages(location)
            }
        }
    }

    /**
     * Загружает сообщения рядом с указанной локацией и группирует их
     */
    private fun loadNearbyMessages(location: LatLng, radiusMeters: Double = 500.0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val nearbyMessages = geoMessageRepository.getNearbyMessages(location, radiusMeters)
                _messages.value = nearbyMessages

                // Группируем сообщения по местоположению
                _groupedMessages.value = groupMessagesByLocationAndDate(nearbyMessages)

            } catch (e: Exception) {
                Log.e("GeoMessageViewModel", "Ошибка загрузки сообщений", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Группирует сообщения по местоположению с использованием геокодирования
     * Исправленная версия без использования async
     */
    private suspend fun groupMessagesByLocationAndDate(messages: List<GeoMessage>): Map<String, List<GeoMessage>> {
        if (messages.isEmpty()) return emptyMap()

        // Группируем сообщения по примерному местоположению
        val locationGroups = messages.groupBy { message ->
            // Округляем координаты для группировки близких мест
            val lat = (message.location.latitude * 100).toInt() / 100.0
            val lng = (message.location.longitude * 100).toInt() / 100.0
            "${lat}_${lng}"
        }

        // Преобразуем Map<String, List<GeoMessage>> в Map<String, List<GeoMessage>>
        // где ключ меняется с координат на название места
        val result = mutableMapOf<String, List<GeoMessage>>()

        // Для каждой группы получаем название места через геокодер
        for ((locationKey, messagesInLocation) in locationGroups) {
            // Разбиваем ключ обратно на координаты
            val parts = locationKey.split("_")
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()

                // Получаем название места
                val locationName = geocoderHelper.getLocationName(LatLng(lat, lng))

                // Добавляем в результат
                result[locationName] = messagesInLocation
            } else {
                // Если что-то пошло не так с ключом, используем запасной вариант
                result["Неизвестное место"] = messagesInLocation
            }
        }

        return result
    }

    /**
     * Отправляет новое геолокационное сообщение
     */
    suspend fun sendGeoMessage(text: String): GeoMessage? {
        if (currentLocation.value == null || currentUser.value == null) {
            return null
        }

        val user = currentUser.value!!
        val location = currentLocation.value!!

        return try {
            val message = GeoMessage(
                id = UUID.randomUUID().toString(),
                senderId = user.id,
                senderName = user.username,
                avatarUrl = user.avatarUrl,
                text = text,
                location = location,
                timestamp = System.currentTimeMillis()
            )

            // Используем repository для отправки сообщения
            geoMessageRepository.sendGeoMessage(message)

            // Возвращаем созданное сообщение
            message
        } catch (e: Exception) {
            Log.e(TAG, "Error sending geo message", e)
            null
        }
    }

    /**
     * Запускает слушатель сообщений для указанной локации
     */
    private fun startMessageListener(location: LatLng, radiusMeters: Double = 500.0) {
        messageListener = geoMessageRepository.listenToNearbyMessages(location, radiusMeters) { messages ->
            _messages.value = messages

            // Обновляем группировку при получении новых сообщений
            viewModelScope.launch {
                _groupedMessages.value = groupMessagesByLocationAndDate(messages)
            }
        }
    }

    /**
     * Останавливает активный слушатель сообщений
     */
    private fun stopMessageListener() {
        messageListener?.remove()
        messageListener = null
    }

    /**
     * Рассчитывает расстояние между двумя точками в метрах
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            point1.latitude, point1.longitude,
            point2.latitude, point2.longitude,
            results
        )
        return results[0]
    }

    /**
     * Очищает ресурсы при уничтожении ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        stopMessageListener()
    }

    /**
     * Устанавливает состояние загрузки
     */
    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    /**
     * Форматирует дату для группировки сообщений
     */
    fun getDateGroup(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        return when {
            isSameDay(calendar, today) -> "Сегодня"
            isYesterday(calendar, today) -> "Вчера"
            else -> {
                val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("ru"))
                dateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * Проверяет, относится ли дата к сегодняшнему дню
     */
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Проверяет, относится ли дата к вчерашнему дню
     */
    private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR) - 1
    }

    /**
     * Фабрика для создания GeoMessageViewModel с нужными зависимостями
     */
    class Factory(
        private val userRepository: UserRepository,
        private val geoMessageRepository: GeoMessageRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GeoMessageViewModel::class.java)) {
                return GeoMessageViewModel(userRepository, geoMessageRepository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}