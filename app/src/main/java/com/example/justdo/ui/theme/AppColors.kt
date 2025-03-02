package com.example.justdo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Цветовая палитра приложения
 */
object AppColors {
    // Основные цвета
    val Primary = Color(0xFFD32F2F)        // Основной красный
    val PrimaryDark = Color(0xFF9A0007)    // Темно-красный
    val PrimaryLight = Color(0xFFFF6659)   // Светло-красный

    // Фоновые цвета
    val Background = Color.White
    val Surface = Color(0xFFF5F5F5)        // Светло-серый фон
    val SurfaceVariant = Color(0xFFE0E0E0) // Серый вариант фона

    // Текстовые цвета
    val TextPrimary = Color(0xFF212121)    // Основной текст
    val TextSecondary = Color(0xFF757575)  // Вторичный текст
    val TextHint = Color(0xFFBDBDBD)       // Подсказки

    // Акцентные цвета
    val Accent = Color(0xFF2979FF)         // Синий акцент
    val AccentLight = Color(0x222979FF)    // Синий с прозрачностью

    // Цвета статуса
    val Success = Color(0xFF4CAF50)        // Зеленый (успех)
    val Warning = Color(0xFFFFC107)        // Желтый (предупреждение)
    val Error = Color(0xFFD32F2F)          // Красный (ошибка)
}