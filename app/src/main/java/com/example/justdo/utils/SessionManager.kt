package com.example.justdo.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.justdo.data.models.User

class SessionManager(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(userId: String, username: String, password: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun getCredentials(): User? {

        return sharedPreferences.getString(KEY_USER_ID, "")?.let {
            User(
                id = it,
                name = sharedPreferences.getString(KEY_USERNAME, "")!!,
                password = sharedPreferences.getString(KEY_PASSWORD, "")!!
            )
        }
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_USER_ID = "userId"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}