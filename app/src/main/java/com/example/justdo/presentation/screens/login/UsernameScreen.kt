package com.example.justdo.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.justdo.presentation.viewmodels.UserViewModel
import com.example.justdo.ui.theme.TelegramColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameScreen(
    userViewModel: UserViewModel,
    onBack: () -> Unit
) {
    val uiState by userViewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Создание профиля",
                        color = TelegramColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TelegramColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TelegramColors.TopBar
                )
            )
        },
        containerColor = TelegramColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Аватар плейсхолдер
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(TelegramColors.Primary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = TelegramColors.Primary,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Как вас зовут?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TelegramColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Стилизованное поле ввода имени
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = TelegramColors.TextSecondary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Ваше имя",
                        style = MaterialTheme.typography.labelMedium,
                        color = TelegramColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Поле ввода имени
                    BasicTextField(
                        value = username,
                        onValueChange = { username = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = TelegramColors.TextPrimary
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (username.isEmpty()) {
                                    Text(
                                        text = "Введите имя",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TelegramColors.TextSecondary.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = "Это имя будут видеть другие пользователи",
                    style = MaterialTheme.typography.bodySmall,
                    color = TelegramColors.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            userViewModel.saveUsernameAndSignIn(username)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramColors.Primary
                    )
                ) {
                    Text(
                        text = "Завершить регистрацию",
                        fontSize = MaterialTheme.typography.titleMedium.fontSize
                    )
                }

                // Показываем ошибки и индикаторы загрузки
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoading) {
                    CircularProgressIndicator(color = TelegramColors.Primary)
                }

                if (uiState.error.isNotEmpty()) {
                    Text(
                        text = uiState.error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}