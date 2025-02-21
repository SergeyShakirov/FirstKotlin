package com.example.justdo.network.api

import com.example.justdo.data.models.ImgurResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImgurApi {
    @Multipart
    @POST("3/image")
    suspend fun uploadImage(
        @Header("Authorization") clientId: String,
        @Part image: MultipartBody.Part
    ): Response<ImgurResponse>
}