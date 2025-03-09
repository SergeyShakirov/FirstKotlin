package com.example.justdo.presentation.screens.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.justdo.presentation.viewmodels.UserViewModel
import com.example.justdo.ui.theme.TelegramColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    userViewModel: UserViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by userViewModel.uiState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var showCountryPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isCodeSent) "Введите код" else "Вход по телефону",
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
                if (!uiState.isCodeSent) {
                    // Экран ввода телефона
                    Text(
                        text = "Введите ваш номер телефона",
                        style = MaterialTheme.typography.titleLarge,
                        color = TelegramColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Выбор страны
                    val selectedCountry = uiState.selectedCountry
                    if (selectedCountry != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCountryPicker = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedCountry.flagEmoji} ${selectedCountry.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TelegramColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = selectedCountry.code,
                                style = MaterialTheme.typography.titleMedium,
                                color = TelegramColors.Primary
                            )

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Выбрать страну",
                                tint = TelegramColors.TextSecondary
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = TelegramColors.TextSecondary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // Поле ввода телефона с визуальным оформлением
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
                            text = "Номер телефона",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelegramColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Отображение кода страны и поля ввода в одной строке
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Код страны
                            if (selectedCountry != null) {
                                Text(
                                    text = selectedCountry.code,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TelegramColors.Primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            // Поле ввода номера
                            BasicTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = TelegramColors.TextPrimary
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (phoneNumber.isEmpty()) {
                                            Text(
                                                text = "Введите номер",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = TelegramColors.TextSecondary.copy(alpha = 0.6f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "Номер будет использован только для авторизации",
                        style = MaterialTheme.typography.bodySmall,
                        color = TelegramColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопка входа по телефону
                    Button(
                        onClick = {
                            if (phoneNumber.isNotBlank()) {
                                // Вызываем startPhoneAuth
                                userViewModel.startPhoneAuth(
                                    phoneNumber = phoneNumber,
                                    activity = activity,
                                )
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
                            text = "Получить код",
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    }

                    // Диалог выбора страны
                    if (showCountryPicker) {
                        CountryPickerDialog(
                            countries = uiState.countryList,
                            onDismiss = { showCountryPicker = false },
                            onCountrySelected = { country ->
                                userViewModel.selectCountry(country)
                                showCountryPicker = false
                            }
                        )
                    }
                } else {
                    // Экран ввода кода
                    Text(
                        text = "Введите код из СМС",
                        style = MaterialTheme.typography.titleLarge,
                        color = TelegramColors.TextPrimary
                    )
                    Text(
                        text = "Мы отправили код на номер ${uiState.formattedPhoneNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TelegramColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Стилизованное поле ввода кода с центровкой и автоматической проверкой
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = TelegramColors.TextSecondary.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Код из СМС",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelegramColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Поле ввода кода с автоматической проверкой
                        BasicTextField(
                            value = verificationCode,
                            onValueChange = {
                                // Ограничиваем ввод только цифрами и длиной до 6 символов
                                val filtered = it.filter { char -> char.isDigit() }
                                if (filtered.length <= 6) {
                                    verificationCode = filtered

                                    // Если введено 6 цифр, автоматически проверяем код
                                    if (filtered.length == 6) {
                                        userViewModel.verifyPhoneCode(filtered)
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                color = TelegramColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 4.sp,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (verificationCode.isEmpty()) {
                                        Text(
                                            text = "______",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = TelegramColors.TextSecondary.copy(alpha = 0.3f),
                                            letterSpacing = 4.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
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