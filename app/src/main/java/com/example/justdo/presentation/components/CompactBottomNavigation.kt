package com.example.justdo.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.justdo.ui.components.BottomNavItem
import com.example.justdo.ui.theme.TelegramColors

/**
 * Компонент нижней навигации в стиле Telegram
 * Сохраняет совместимость с существующим кодом, но использует стиль Telegram
 */
@Composable
fun CompactBottomNavigation(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController,
    isVisible: Boolean = true
) {
    // Используем цвета Telegram
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Surface(
            color = TelegramColors.NavigationBar,
            contentColor = TelegramColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Карта
                NavItem(
                    icon = Icons.Default.Map,
                    isSelected = selectedItem == 0,
                    onClick = {
                        onItemSelected(0)
                        navController.navigate("map_tab") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Чат
                NavItem(
                    icon = Icons.Default.Forum,
                    isSelected = selectedItem == 1,
                    onClick = {
                        onItemSelected(1)
                        navController.navigate("geochat_tab") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // Профиль
                NavItem(
                    icon = Icons.Default.Person,
                    isSelected = selectedItem == 2,
                    onClick = {
                        onItemSelected(2)
                        navController.navigate("profile_tab") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(300)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TelegramColors.Primary.copy(alpha = 0.1f))
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) TelegramColors.Primary else TelegramColors.TextSecondary,
            modifier = Modifier.size(26.dp)
        )
    }
}