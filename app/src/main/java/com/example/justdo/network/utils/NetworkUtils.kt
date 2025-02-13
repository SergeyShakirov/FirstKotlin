package com.example.justdo.network.utils

import android.util.Base64
import java.text.SimpleDateFormat
import java.util.Locale

object NetworkUtils {
    fun getBasicAuth(username: String, password: String): String {
        val auth = "$username:$password"
        return Base64.encodeToString(auth.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    fun parseTimestamp(timestampString: String): Long {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            formatter.parse(timestampString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}