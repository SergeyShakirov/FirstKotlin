package com.example.justdo.data.models

data class User(
    val id: String,
    val name: String,
    val password: String = "",
    val email: String = ""
)