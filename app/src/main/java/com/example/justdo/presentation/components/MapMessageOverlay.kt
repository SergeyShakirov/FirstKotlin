package com.example.justdo.presentation.components

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.justdo.data.models.GeoMessage
import kotlinx.coroutines.delay

@Composable
fun MapMessageOverlay(
    messages: List<GeoMessage>,
    currentUserId: String,
    onMessageDismiss: (String) -> Unit = {}
) {
    // Если нет сообщений, ничего не отображаем
    if (messages.isEmpty()) return

    // Создаем локальную копию списка сообщений
    val messagesList = remember { mutableStateListOf<GeoMessage>() }

    // Состояние видимости контейнера сообщений
    var containerVisible by remember { mutableStateOf(true) }

    // Состояние развернутости формы (свернута/развернута)
    var isExpanded by remember { mutableStateOf(false) }

    // Время последнего поступления сообщения
    var lastMessageTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Анимация прозрачности контейнера
    val containerAlpha by animateFloatAsState(
        targetValue = if (containerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    // Анимация высоты контейнера в зависимости от состояния развернутости
    val containerHeight by animateDpAsState(
        targetValue = with(LocalDensity.current) {
            if (isExpanded) {
                // Половина экрана в развернутом состоянии
                (LocalDensity.current.density * 400f).toDp()
            } else {
                // Обычная высота в свернутом состоянии
                (LocalDensity.current.density * 100f).toDp()
            }
        },
        animationSpec = tween(durationMillis = 300) // Быстрая анимация для отзывчивости
    )

    // Запускаем таймер для проверки неактивности, но только если форма не развернута
    LaunchedEffect(lastMessageTime, isExpanded) {
        if (!isExpanded) { // Скрываем форму только если она не развернута
            delay(30000) // 30 секунд неактивности
            containerVisible = false
        }
    }

    // КЛЮЧЕВОЙ МОМЕНТ: Явно обновляем локальный список при изменении входного списка
    LaunchedEffect(messages) {
        Log.d("MapOverlay", "Обновление списка сообщений: ${messages.size} сообщений")

        // Показываем контейнер при получении новых сообщений
        containerVisible = true
        lastMessageTime = System.currentTimeMillis()

        // Очищаем старый список
        messagesList.clear()

        // Добавляем все сообщения из нового списка
        messagesList.addAll(messages)

        // Отладочный вывод полученных сообщений
        messages.forEach { message ->
            Log.d("MapOverlay", "Сообщение: ID=${message.id}, текст=${message.text}, время=${message.timestamp}")
        }
    }

    // Отображаем контейнер для сообщений над инпутом
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(vertical = 64.dp)) {
        // Создаем контейнер с градиентным фоном и возможностью клика
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight) // Анимированная высота
                .align(Alignment.BottomCenter)
                .alpha(containerAlpha)
                // Добавляем возможность клика для разворачивания/сворачивания
                .clickable {
                    // Переключаем состояние развернутости
                    isExpanded = !isExpanded
                    // Если разворачиваем, показываем контейнер (на всякий случай)
                    if (isExpanded) {
                        containerVisible = true
                    }
                    Log.d("MapOverlay", "Состояние формы изменено: ${if (isExpanded) "развернута" else "свернута"}")
                }
                // Градиент для плавного затухания
                .drawWithContent {
                    // Вертикальный градиент для плавного затухания сверху
                    val verticalBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,           // Верх прозрачный
                            Color.Black.copy(alpha = 0.6f), // Плавный переход
                            Color.Black.copy(alpha = 0.7f)  // Более заметный внизу
                        ),
                        startY = 0f,
                        endY = size.height
                    )

                    // Рисуем основной фон
                    drawRect(brush = verticalBrush)

                    // Рисуем содержимое (сообщения)
                    drawContent()
                }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = true, // Это перевернет порядок элементов - новые будут снизу
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp), // Отступы внутри списка
            ) {
                val sortedMessages = messagesList.sortedByDescending { it.timestamp }
                items(sortedMessages) { message ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        MapMessage(
                            message = message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // Увеличенный отступ между сообщениями
                }
            }
        }
    }
}