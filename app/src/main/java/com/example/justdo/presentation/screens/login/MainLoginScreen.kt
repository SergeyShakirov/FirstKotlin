package com.example.justdo.presentation.screens.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.justdo.R
import com.example.justdo.presentation.viewmodels.UserViewModel
import com.example.justdo.ui.theme.TelegramColors
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun MainLoginScreen(
    userViewModel: UserViewModel,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val uiState by userViewModel.uiState.collectAsState()

    // Регистрация лаунчера для Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { token ->
                    userViewModel.signInWithGoogle(token)
                }
            } catch (e: ApiException) {
                userViewModel.setError("Google sign in failed: ${e.message}")
            }
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
                        val intent = userViewModel.getGoogleSignInIntent()
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

                Button(
                    onClick = {
                        userViewModel.showPhoneLogin()
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
                        style = MaterialTheme.typography.bodyMedium,
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