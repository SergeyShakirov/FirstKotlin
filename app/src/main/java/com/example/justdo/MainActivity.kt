package com.example.justdo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.justdo.data.repository.AuthRepository
import com.example.justdo.presentation.screens.MyApp
import com.google.firebase.Firebase
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {
    private lateinit var repository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = AuthRepository()

        Firebase.initialize(this)

        setContent {
            MyApp(
                repository = repository
            )
        }
    }
}