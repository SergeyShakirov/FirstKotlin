package com.example.justdo.data.models

data class ImgurResponse(
    val data: ImageData,
    val success: Boolean,
    val status: Int
)

data class ImageData(
    val id: String,
    val title: String?,
    val description: String?,
    val link: String
)