package com.example.justdo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Создаем цветовую схему в стиле Telegram
private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramColors.Primary,
    onPrimary = Color.White,
    secondary = TelegramColors.NameColor,
    onSecondary = Color.White,
    background = TelegramColors.Background,
    onBackground = TelegramColors.TextPrimary,
    surface = TelegramColors.Surface,
    onSurface = TelegramColors.TextPrimary,
    error = TelegramColors.Error,
    onError = Color.White,
    surfaceVariant = TelegramColors.OtherMessage,
    primaryContainer = TelegramColors.MyMessage,
    onPrimaryContainer = Color.White,
    secondaryContainer = TelegramColors.DateBadge,
    onSecondaryContainer = TelegramColors.TextPrimary
)

// Для совместимости можно оставить светлую тему, но сделать ее идентичной темной
private val TelegramLightColorScheme = TelegramDarkColorScheme

// Обновленный AppTheme с поддержкой Telegram-стиля
@Composable
fun JustDoTheme(
    forceTelegramStyle: Boolean = true, // По умолчанию используем стиль Telegram
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Отключаем динамический цвет для стиля Telegram
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Если принудительно включен стиль Telegram, используем его
        forceTelegramStyle -> TelegramDarkColorScheme

        // Иначе используем стандартную логику с динамическими цветами
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TelegramColors.TopBar.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}