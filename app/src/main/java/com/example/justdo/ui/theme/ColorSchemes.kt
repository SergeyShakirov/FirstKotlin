package com.example.justdo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

val RedWhiteColorScheme = ColorScheme(
    primary = Color(0xFFD32F2F),           // Яркий красный
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD32F2F).copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFFD32F2F).copy(alpha = 0.5f),

    secondary = Color(0xFFF44336),         // Немного более светлый красный
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF44336).copy(alpha = 0.2f),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFB71C1C),          // Темно-красный
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB71C1C).copy(alpha = 0.2f),
    onTertiaryContainer = Color.White,

    background = Color.White,
    onBackground = Color.Black,

    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color.White.copy(alpha = 0.9f),
    onSurfaceVariant = Color.Black,
    surfaceTint = Color(0xFFD32F2F),

    inverseSurface = Color.Black,
    inverseOnSurface = Color.White,

    error = Color(0xFFB71C1C),             // Темно-красный для ошибок
    onError = Color.White,
    errorContainer = Color(0xFFB71C1C).copy(alpha = 0.2f),
    onErrorContainer = Color.White,

    outline = Color.Red.copy(alpha = 0.3f),
    outlineVariant = Color.Red.copy(alpha = 0.1f),
    scrim = Color.Black.copy(alpha = 0.5f),

    // Новые параметры в последней версии Material 3
    surfaceBright = Color.White,
    surfaceDim = Color.Gray.copy(alpha = 0.1f),
    surfaceContainer = Color.White.copy(alpha = 0.9f),
    surfaceContainerHigh = Color.White.copy(alpha = 0.8f),
    surfaceContainerHighest = Color.White.copy(alpha = 0.7f),
    surfaceContainerLow = Color.White.copy(alpha = 0.95f),
    surfaceContainerLowest = Color.White
)