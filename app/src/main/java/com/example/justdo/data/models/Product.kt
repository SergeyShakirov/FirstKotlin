package com.example.justdo.data.models

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val seller: User?,
    val isFavorite: Boolean = false
)