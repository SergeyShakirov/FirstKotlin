package com.example.justdo.utils

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthHelper(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)

    /**
     * Проверяет, доступна ли биометрическая аутентификация
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Проверяет, настроена ли биометрическая аутентификация для приложения
     */
    fun isBiometricEnabled(): Boolean {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("biometric_enabled", false)
    }

    /**
     * Сохраняет настройку биометрической аутентификации
     */
    fun setBiometricEnabled(enabled: Boolean) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    /**
     * Сохраняет ID пользователя для биометрической аутентификации
     */
    fun saveBiometricUserId(userId: String) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("biometric_user_id", userId).apply()
    }

    /**
     * Получает ID пользователя для биометрической аутентификации
     */
    fun getBiometricUserId(): String? {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return prefs.getString("biometric_user_id", null)
    }

    /**
     * Показывает диалог биометрической аутентификации
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Вход по отпечатку пальца",
        subtitle: String = "Используйте биометрию для входа",
        description: String = "Приложите палец к сканеру отпечатков",
        negativeButtonText: String = "Отмена",
        onSuccess: () -> Unit,
        onError: (Int, CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Очищает сохраненные данные биометрической аутентификации
     */
    fun clearBiometricData() {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("biometric_enabled")
            .remove("biometric_user_id")
            .apply()
    }
}