package com.example.justdo.data.repository

import android.content.Context
import android.net.Uri
import com.example.justdo.network.api.ImgurApi
import com.example.justdo.network.constants.ImgurConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ImgurRepository {
    private val api: ImgurApi

    init {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Accept", "application/json")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.imgur.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ImgurApi::class.java)
    }

    suspend fun uploadImage(context: Context, imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                // Читаем файл
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw IOException("Не удалось открыть файл")

                val bytes = inputStream.use { it.readBytes() }

                // Создаем MultipartBody.Part
                val requestFile = bytes.toRequestBody(
                    "image/*".toMediaTypeOrNull(),
                    0, bytes.size
                )

                val body = MultipartBody.Part.createFormData(
                    "image",
                    "photo.jpg",
                    requestFile
                )

                // Отправляем запрос
                val response = api.uploadImage(
                    "Client-ID ${ImgurConfig.CLIENT_ID}",
                    body
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    response.body()?.data?.link
                        ?: throw IOException("Нет ссылки на изображение")
                } else {
                    throw IOException("Ошибка загрузки: ${response.code()}")
                }
            } catch (e: Exception) {
                throw IOException("Ошибка при загрузке изображения", e)
            }
        }
    }
}