package com.example.justdo.data.repository

import android.Manifest
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.justdo.data.models.Message
import com.example.justdo.data.models.User
import com.example.justdo.data.models.Product
import com.example.justdo.network.AuthApi
import com.example.justdo.network.MessagesApi
import com.example.justdo.network.ProductApi
import com.example.justdo.network.UserApi
import com.example.justdo.data.database.dao.MessageDao
import com.example.justdo.utils.NotificationHelper
import com.example.justdo.utils.SessionManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.RemoteMessage

class MessengerRepository(
    private val context: Context,
    private val messageDao: MessageDao? = null // Опциональный параметр для обратной совместимости
) {
    private val sessionManager = SessionManager(context)

    // Существующие методы
    suspend fun login(username: String, password: String): User {
        return try {
            val user = AuthApi.login(username, password)
            sessionManager.saveCredentials(user.id, username, password)
            initializePushNotifications(context)
            user

        } catch (e: Exception) {
            throw Exception("Ошибка авторизации")
        }
    }

    private suspend fun saveFcmToken(userId: String, token: String) {
        try {
            AuthApi.saveFcmToken(userId, token)
        } catch (e: Exception) {
            Log.e("MessengerRepository", "Ошибка сохранения FCM токена", e)
            throw Exception("Ошибка сохранения токена: ${e.message}")
        }
    }

    private suspend fun tokenFCM(userId: String): String {
        return try {
            AuthApi.tokenFCM(userId)
        } catch (e: Exception) {
            Log.e("MessengerRepository", "Ошибка получения FCM токена", e)
            throw Exception("Ошибка получения токена: ${e.message}")
        }
    }

    suspend fun register(username: String, password: String): User {
        return try {
            AuthApi.register(username, password)
            AuthApi.login(username, password)
        } catch (e: Exception) {
            Log.e("MessengerRepository", "Ошибка при регистрации", e)
            throw Exception("Ошибка регистрации: ${e.message}")
        }
    }

    suspend fun getUsers(): List<User> {
        return try {
            UserApi.getUsers()
        } catch (e: Exception) {
            throw Exception("Ошибка получения списка пользователей")
        }
    }

    suspend fun sendMessage(userId: String, message: String): Boolean {
        return try {
            val success = MessagesApi.sendMessage(userId, message)

            if (success){

                val token = tokenFCM(userId)

                val fcmPayload = mapOf(
                    "data" to mapOf(
                        "userId" to userId,
                        "type" to "important",
                        "title" to "Новое сообщение",
                        "message" to message
                    )
                )

                // Отправляем уведомление
                sendFCMNotification(token.toString(), fcmPayload)

            }
            success
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Существующий метод обновления сообщений (можно оставить для обратной совместимости)
    suspend fun refreshMessages(userId: String): List<Message> {
        return try {
            MessagesApi.refreshMessages(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Методы для работы с продуктами остаются без изменений
    suspend fun products(): List<Product> {
        return try {
            ProductApi.fetchProducts()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addProduct(product: Product): Boolean {
        return try {
            ProductApi.addProduct(product)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun initializePushNotifications(context: Context) {
        // Инициализация каналов
        NotificationHelper(context).ensureNotificationChannelsExist()

        val user: User? = sessionManager.getCredentials()

        // Получаем токен и сохраняем на сервер
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    // Сохраняем токен на сервер через корутину
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (user != null) {
                                saveFcmToken(user.id, token)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
    }

    private fun sendFCMNotification(token: String, payload: Map<String, Any>) {
    }
}