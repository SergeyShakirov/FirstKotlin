package com.example.justdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.screens.MyApp

class MainActivity : ComponentActivity() {
    private lateinit var repository: AuthRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AuthRepository()
        setContent {
            MyApp(repository)
        }
    }
}