package com.example.justdo.presentation.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.justdo.R
import com.example.justdo.presentation.viewmodels.CountryInfo
import com.example.justdo.presentation.viewmodels.LoginViewModel
import com.example.justdo.ui.theme.TelegramColors
import com.example.justdo.utils.BiometricAuthHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.text.style.TextAlign
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val uiState by viewModel.uiState.collectAsState()

    // Обработка изменений состояния
    LaunchedEffect(uiState) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    // Инициализация Google Sign-In при создании
    LaunchedEffect(Unit) {
        try {
            viewModel.initGoogleSignIn()
            viewModel.resetState()
        } catch (e: Exception) {
            viewModel.setError("Ошибка инициализации Google Sign-In: ${e.message}")
        }
    }

    // Показываем биометрический диалог, если требуется
    if (uiState.showBiometricPrompt && activity != null) {
        val biometricHelper = BiometricAuthHelper(context)

        // Показываем диалог биометрической аутентификации
        LaunchedEffect(uiState.showBiometricPrompt) {
            biometricHelper.showBiometricPrompt(
                activity = activity,
//                title = "Вход по отпечатку пальца",
//                subtitle = "Используйте биометрию для быстрого входа",
//                description = "Приложите палец к сканеру отпечатков",
//                negativeButtonText = "Отмена",
                onSuccess = {
                    viewModel.onBiometricSuccess()
                },
                onError = { errorCode, errString ->
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        viewModel.setError("Биометрическая аутентификация не удалась: $errString")
                    }
                    viewModel.resetBiometricPrompt()
                },
//                onFailed = {
//                    viewModel.setError("Не удалось распознать отпечаток пальца. Попробуйте еще раз.")
//                    viewModel.resetBiometricPrompt()
//                }
            )
        }
    }

    // Выбор экрана в зависимости от состояния
    when {
        uiState.isVerificationCompleted -> {
            UsernameScreen(
                viewModel = viewModel,
                onBack = { viewModel.backToVerification() }
            )
        }
        uiState.showPhoneLogin -> {
            PhoneAuthScreen(
                viewModel = viewModel,
                onBack = { viewModel.resetState() }
            )
        }
        else -> {
            MainLoginScreen(
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun MainLoginScreen(
    viewModel: LoginViewModel
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by viewModel.uiState.collectAsState()

    // Регистрация лаунчера для Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                viewModel.setError("Google sign in failed: ${e.message}")
            }
        }
    }

    // Проверяем биометрию при загрузке экрана
    LaunchedEffect(Unit) {
        // Пытаемся запустить биометрическую аутентификацию, если доступно
        if (viewModel.uiState.value.isBiometricAvailable) {
            // Задержка для завершения анимации загрузки экрана
            delay(300)
            viewModel.authenticateWithBiometric()
        }
    }

    Scaffold(
        containerColor = TelegramColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Логотип приложения
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Logo",
                    tint = TelegramColors.Primary,
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "Войти в приложение",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TelegramColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка входа через Google
                Button(
                    onClick = {
                        Log.e("PHONE_AUTH", "Нажата кнопка Google")
                        val intent = viewModel.getGoogleSignInIntent()
                        googleSignInLauncher.launch(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = "Войти с аккаунтом Google")
                    }
                }

                // Разделитель
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = TelegramColors.TextSecondary.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "или",
                        color = TelegramColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = TelegramColors.TextSecondary.copy(alpha = 0.3f)
                    )
                }

                // Кнопка входа по отпечатку пальца (если доступно)
                if (uiState.isBiometricAvailable) {
                    Button(
                        onClick = {
                            viewModel.authenticateWithBiometric()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF32A852) // Зеленый цвет
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Отпечаток пальца",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(text = "Войти по отпечатку пальца")
                        }
                    }

                    // Еще один разделитель после кнопки биометрии
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TelegramColors.TextSecondary.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "или",
                            color = TelegramColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = TelegramColors.TextSecondary.copy(alpha = 0.3f)
                        )
                    }
                }

                // Кнопка входа по телефону
                Button(
                    onClick = {
                        viewModel.showPhoneLogin()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramColors.Primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(text = "Войти по номеру телефона")
                    }
                }

                // Отображение ошибки
                if (uiState.error.isNotEmpty()) {
                    Text(
                        text = uiState.error,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Индикатор загрузки
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = TelegramColors.Primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by viewModel.uiState.collectAsState()

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
                            Log.e("PHONE_AUTH", "Нажата кнопка отправки кода")
                            if (phoneNumber.isNotBlank()) {
                                // Вызываем startPhoneAuth
                                viewModel.startPhoneAuth(
                                    phoneNumber = phoneNumber,
                                    activity = activity,
                                    onVerificationSent = { /* Уже обновляется в ViewModel */ },
                                    onError = { /* Уже обновляется в ViewModel */ }
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
                                viewModel.selectCountry(country)
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
                                        viewModel.verifyPhoneCode(filtered)
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

                    // Кнопка "Подтвердить" убрана и заменена на индикатор загрузки или сообщение
//                    if (uiState.isLoading) {
//                        Spacer(modifier = Modifier.height(16.dp))
//                        CircularProgressIndicator(
//                            color = TelegramColors.Primary,
//                            modifier = Modifier.size(30.dp)
//                        )
//                        Text(
//                            text = "Проверка кода...",
//                            color = TelegramColors.TextSecondary,
//                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
//                            modifier = Modifier.padding(top = 8.dp)
//                        )
//                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
                            viewModel.saveUsernameAndSignIn(username)
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

@Composable
fun CountryPickerDialog(
    countries: List<CountryInfo>,
    onDismiss: () -> Unit,
    onCountrySelected: (CountryInfo) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TelegramColors.Background
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выберите страну",
                    style = MaterialTheme.typography.titleLarge,
                    color = TelegramColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = TelegramColors.TextSecondary.copy(alpha = 0.3f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    countries.forEach { country ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCountrySelected(country) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${country.flagEmoji} ${country.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TelegramColors.TextPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = country.code,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TelegramColors.Primary
                            )
                        }

                        HorizontalDivider(color = TelegramColors.TextSecondary.copy(alpha = 0.1f))
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelegramColors.Primary
                    )
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}