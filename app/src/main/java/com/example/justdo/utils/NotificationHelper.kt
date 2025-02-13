package com.example.justdo.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.justdo.MainActivity
import com.example.justdo.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_GENERAL = "general_channel"
        const val CHANNEL_IMPORTANT = "important_channel"
        const val CHANNEL_MARKETING = "marketing_channel"
    }

    fun ensureNotificationChannelsExist() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            // Проверяем и создаем важный канал
            if (notificationManager.getNotificationChannel(CHANNEL_IMPORTANT) == null) {
                createImportantChannel(notificationManager)
            }

            // Проверяем и создаем общий канал
            if (notificationManager.getNotificationChannel(CHANNEL_GENERAL) == null) {
                createGeneralChannel(notificationManager)
            }

            // Проверяем и создаем маркетинговый канал
            if (notificationManager.getNotificationChannel(CHANNEL_MARKETING) == null) {
                createMarketingChannel(notificationManager)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createImportantChannel(notificationManager: NotificationManager) {
        val channelImportant = NotificationChannel(
            CHANNEL_IMPORTANT,
            "Важные уведомления",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Срочные и важные уведомления"
            enableLights(true)
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channelImportant)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createGeneralChannel(notificationManager: NotificationManager) {
        val channelGeneral = NotificationChannel(
            CHANNEL_GENERAL,
            "Общие уведомления",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Общие уведомления приложения"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channelGeneral)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMarketingChannel(notificationManager: NotificationManager) {
        val channelMarketing = NotificationChannel(
            CHANNEL_MARKETING,
            "Маркетинговые уведомления",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Новости и специальные предложения"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channelMarketing)
    }

    fun sendNotification(
        channelId: String,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(getPriorityForChannel(channelId))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(notificationId, notification)
    }

    private fun getPriorityForChannel(channelId: String): Int {
        return when (channelId) {
            CHANNEL_IMPORTANT -> NotificationCompat.PRIORITY_HIGH
            CHANNEL_GENERAL -> NotificationCompat.PRIORITY_DEFAULT
            CHANNEL_MARKETING -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    fun cancelAllNotifications() {
        TODO("Not yet implemented")
    }
}