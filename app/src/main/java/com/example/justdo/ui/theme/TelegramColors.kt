package com.example.justdo.ui.theme

import androidx.compose.ui.graphics.Color

// Основные цвета Telegram-стиля
object TelegramColors {
    // Фоны
    val Background = Color(0xFF121B22)  // Основной темный фон
    val Surface = Color(0xFF1A2632)     // Поверхности (карточки, диалоги)
    val TopBar = Color(0xFF1F2936)      // Верхняя панель, чуть светлее фона
    val NavigationBar = Color(0xFF1A2632) // Нижняя панель навигации

    // Элементы сообщений
    val MyMessage = Color(0xFF2B5278)   // Синий для собственных сообщений
    val OtherMessage = Color(0xFF222E3A) // Темно-серый для чужих сообщений
    val DateBadge = Color(0xFF1E2C3A)   // Фон для дат и бейджей

    // Текст
    val TextPrimary = Color.White       // Основной текст
    val TextSecondary = Color(0xFF8B9398) // Вторичный текст и иконки
    val TextHint = Color(0xFF6B7C85)    // Подсказки, временный текст
    val NameColor = Color(0xFF6CB1ED)   // Голубой цвет для имен пользователей

    // Акценты
    val Primary = Color(0xFF5EAAEC)     // Основной акцентный цвет (кнопки, выделение)
    val Online = Color(0xFF69BF72)      // Индикатор онлайн
    val Error = Color(0xFFEC5E5E)       // Ошибки и предупреждения

    // Аватары (как в Telegram)
    val AvatarRed = Color(0xFFE17076)
    val AvatarBlue = Color(0xFF4AB2F4)
    val AvatarCyan = Color(0xFF2CA5E0)
    val AvatarPurple = Color(0xFF9069CD)
    val AvatarGreen = Color(0xFF67B35D)
    val AvatarOrange = Color(0xFFFFB774)
}