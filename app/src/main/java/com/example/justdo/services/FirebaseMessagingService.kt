package com.example.justdo.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.justdo.GogoApplication
import com.example.justdo.MainActivity
import com.example.justdo.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {
    private val chatManager by lazy { (application as GogoApplication).chatManager }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Сохраняем новый токен в Firestore
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val activeChat = chatManager.getActiveChat()
        val chatId = message.data["chatId"]

        if (activeChat != null && activeChat == chatId) {
            return
        }

        // Создаем notification channel
        createNotificationChannel()

        // Показываем уведомление
        val notification = message.notification
        if (notification != null) {
            showNotification(notification.title, notification.body, message.data)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default",
                "Default Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Default notification channel"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        title: String?,
        body: String?,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            data?.get("chatId")?.let { chatId ->
                putExtra("chatId", chatId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "default")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@FirebaseMessagingService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        // Получаем текущего пользователя
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }
}