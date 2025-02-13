package com.example.justdo.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private class ParticleState(
    val initialX: Float,
    val initialY: Float,
    screenWidth: Int,
    screenHeight: Int
) {
    val size = Random.nextInt(4, 12)
    val moveRangeX = Random.nextInt(50, screenWidth / 2)
    val moveRangeY = Random.nextInt(50, screenHeight / 2)
    val duration = Random.nextInt(3000, 8000)
    val delay = Random.nextInt(0, 2000)
    val colorIndex = Random.nextInt(0, 3) // Индекс для выбора цвета
}

@Composable
fun AnimatedBackground() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    Box(modifier = Modifier.fillMaxSize()) {
        // Градиентные волны
        AnimatedWaves()

        // Частицы
        val particles = remember {
            List(30) {
                ParticleState(
                    initialX = Random.nextFloat() * screenWidth,
                    initialY = Random.nextFloat() * screenHeight,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }

        val colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )

        particles.forEach { particle ->
            FloatingParticle(particle, colors[particle.colorIndex])
        }
    }
}

@Composable
private fun AnimatedWaves() {
    val infiniteTransition = rememberInfiniteTransition(label = "waves")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "wave offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                    )
                )
            )
    )
}

@Composable
private fun FloatingParticle(state: ParticleState, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "particle")

    val xOffset by infiniteTransition.animateFloat(
        initialValue = -state.moveRangeX.toFloat(),
        targetValue = state.moveRangeX.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(state.duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x offset"
    )

    val yOffset by infiniteTransition.animateFloat(
        initialValue = -state.moveRangeY.toFloat(),
        targetValue = state.moveRangeY.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(state.duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y offset"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(state.duration / 2),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(state.duration / 3),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset(
                x = (state.initialX + xOffset).dp,
                y = (state.initialY + yOffset).dp
            )
            .scale(scale)
            .size(state.size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}