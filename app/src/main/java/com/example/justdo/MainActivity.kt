package com.example.justdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.JustDoApp
import com.example.justdo.ui.theme.JustDoTheme
import com.example.justdo.ui.theme.TelegramColors
import com.google.firebase.Firebase
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Firebase.initialize(this)

        setContent {
            JustDoTheme {
                // Основной контейнер
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TelegramColors.Background
                ) {
                    JustDoApp()
                }
            }
        }
    }
}