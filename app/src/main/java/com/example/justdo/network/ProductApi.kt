package com.example.justdo.network

import com.example.justdo.data.models.User
import com.example.justdo.data.models.Product
import com.example.justdo.network.constants.NetworkConstants
import com.example.justdo.network.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ProductApi {
    private const val BASE_URL = NetworkConstants.BASE_URL

    private fun parseProductsResponse(jsonString: String): List<Product> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val products = mutableListOf<Product>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val seller = jsonObject.getJSONObject("seller")
                val user = User(
                    id = seller.getString("id"),
                    name = seller.getString("name")
                )
                products.add(
                    Product(
                        id = jsonObject.getString("id"),
                        name = jsonObject.getString("name"),
                        price = jsonObject.getDouble("price"),
                        description = jsonObject.getString("description"),
                        seller = user,
                    )
                )
            }
            products
        } catch (e: Exception) {
            throw Exception("Ошибка парсинга ответа: ${e.message}")
        }
    }

    suspend fun fetchProducts(): List<Product> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.PRODUCTS}")

        val username = AuthApi.getCurrentUsername()
        val password = AuthApi.getCurrentPassword()
        
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Basic ${NetworkUtils.getBasicAuth(username, password)}")
                setRequestProperty("Content-Type", "application/json")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "Нет дополнительной информации"
                throw Exception("Ошибка HTTP ${connection.responseCode}: $errorResponse")
            }

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use {
                it.readText()
            }

            parseProductsResponse(response)

        } catch (e: Exception) {
            throw Exception("Ошибка получения товаров: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun addProduct(product: Product): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL${NetworkConstants.Endpoints.ADD_PRODUCT}")

        val username = AuthApi.getCurrentUsername()
        val password = AuthApi.getCurrentPassword()

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty(
                    "Authorization",
                    "Basic ${NetworkUtils.getBasicAuth(username, password)}"
                )
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val jsonBody = """
                {
                    "name": "${product.name}",
                    "description": "${product.description}",
                    "price": ${product.price}
                }
                """.trimIndent()

            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            connection.responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            throw Exception("Ошибка добавления товара: ${e.message}")
        } finally {
            connection.disconnect()
        }
    }
}