package com.example.justdo.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

@Composable
fun ExpandableLoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    error: String?,
    isLoading: Boolean,
    onLoginClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showLoginForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        isExpanded = true
        delay(300)
        showLoginForm = true
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Полупрозрачный фон с градиентом
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // Содержимое формы
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedTitle(isExpanded)

            //Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showLoginForm,
                enter = fadeIn() + expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )

                    if (!error.isNullOrEmpty()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                        )
                    ) {
                        Text("Войти")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedTitle(isExpanded: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.5f,
        label = "title scale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        label = "title alpha"
    )

//    Text(
//        text = "Вход",
//        style = MaterialTheme.typography.headlineMedium,
//        modifier = Modifier
//            .graphicsLayer {
//                scaleX = scale
//                scaleY = scale
//                alpha = contentAlpha
//            },
//        color = MaterialTheme.colorScheme.primary
//    )
}