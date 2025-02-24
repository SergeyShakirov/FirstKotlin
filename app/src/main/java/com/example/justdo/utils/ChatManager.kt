package com.example.justdo.utils

import android.content.Context

class ChatManager private constructor(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "chat_preferences"
        private const val PREF_ACTIVE_CHAT = "active_chat"

        @Volatile
        private var instance: ChatManager? = null

        fun getInstance(context: Context): ChatManager {
            return instance ?: synchronized(this) {
                instance ?: ChatManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setActiveChat(chatId: String?) {
        sharedPreferences.edit().apply {
            if (chatId != null) {
                putString(PREF_ACTIVE_CHAT, chatId)
            } else {
                remove(PREF_ACTIVE_CHAT)
            }
            apply()
        }
    }

    fun getActiveChat(): String? {
        return sharedPreferences.getString(PREF_ACTIVE_CHAT, null)
    }
}