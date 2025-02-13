package com.example.justdo.services

import com.example.justdo.network.AuthApi
import com.example.justdo.network.AuthApi.getCurrentUserId
import com.example.justdo.network.AuthApi.saveFcmToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.justdo.utils.NotificationHelper
import com.example.justdo.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()
        // Проверяем/создаем каналы при создании сервиса
        NotificationHelper(this).ensureNotificationChannelsExist()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Отправляем новый токен на сервер
        sendRegistrationToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val data = remoteMessage.data
        val targetUserId = data["userId"]

        try {
            val user = SessionManager(this).getCredentials()
            if (user != null && targetUserId == user.id) {
                val channelId = when (data["type"]) {
                    "important" -> NotificationHelper.CHANNEL_IMPORTANT
                    "marketing" -> NotificationHelper.CHANNEL_MARKETING
                    else -> NotificationHelper.CHANNEL_GENERAL
                }

                NotificationHelper(this).sendNotification(
                    channelId = channelId,
                    title = data["title"] ?: "Уведомление",
                    message = data["message"] ?: "",
                    notificationId = System.currentTimeMillis().toInt()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendRegistrationToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = SessionManager(this@MyFirebaseMessagingService).getCredentials()
                if (user != null) {
                    saveFcmToken(user.id, token)
                }
            } catch (e: Exception) {
                throw Exception("Ошибка сохранения токена: ${e.message}")
            }
        }
    }
}