package com.example.justdo

import android.app.Application
import com.example.justdo.utils.ChatManager

class GogoApplication : Application() {
    lateinit var chatManager: ChatManager
        private set

    override fun onCreate() {
        super.onCreate()
        chatManager = ChatManager.getInstance(this)
    }
}