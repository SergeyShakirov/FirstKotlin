package com.example.justdo.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Сервис для получения данных о погоде через OpenWeatherMap API
 */
class OpenWeatherService {
    companion object {
        // API ключ для OpenWeatherMap (бесплатный уровень)
        // В реальном проекте API ключ должен храниться в защищенном месте,
        // например, в BuildConfig или через шифрование
        private const val API_KEY = "c579b1fa460e36193367cf2b4bb6ed10"
        private const val TAG = "OpenWeatherService"
    }

    /**
     * Получение данных о погоде по координатам
     */
    suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://api.openweathermap.org/data/2.5/weather?" +
                        "lat=$latitude&lon=$longitude&appid=$API_KEY&units=metric&lang=ru"

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = connection.inputStream.bufferedReader()
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    parseWeatherData(response.toString())
                } else {
                    Log.e(TAG, "Ошибка при запросе погоды: код $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении данных о погоде", e)
                null
            }
        }
    }

    /**
     * Парсинг данных о погоде из JSON ответа
     */
    private fun parseWeatherData(jsonResponse: String): WeatherData {
        val jsonObject = JSONObject(jsonResponse)

        // Температура
        val main = jsonObject.getJSONObject("main")
        val temperature = main.getDouble("temp")

        // Детали погоды (описание, иконка)
        val weatherArray = jsonObject.getJSONArray("weather")
        val weather = weatherArray.getJSONObject(0)
        val description = weather.getString("description")
        val iconCode = weather.getString("icon")

        // Облачность
        val clouds = jsonObject.getJSONObject("clouds")
        val cloudiness = clouds.getInt("all")

        // Ветер
        val wind = jsonObject.getJSONObject("wind")
        val windSpeed = wind.getDouble("speed")

        return WeatherData(
            temperature = "${temperature.toInt()}°C",
            description = description,
            iconCode = iconCode,
            windSpeed = "${windSpeed.toInt()} м/с",
            cloudiness = "$cloudiness%",
            humidity = "${main.getInt("humidity")}%"
        )
    }
}

/**
 * Модель данных о погоде
 */
data class WeatherData(
    val temperature: String,
    val description: String,
    val iconCode: String,
    val windSpeed: String,
    val cloudiness: String,
    val humidity: String
)