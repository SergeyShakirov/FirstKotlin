package com.example.justdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.justdo.data.MessengerRepository
import com.example.justdo.presentation.screens.MyApp

class MainActivity : ComponentActivity() {

    private val repository = MessengerRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(repository)
        }
    }
}