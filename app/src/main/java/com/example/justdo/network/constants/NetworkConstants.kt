package com.example.justdo.network.constants

object NetworkConstants {
    const val BASE_URL = "https://1c.moveit.kz/mobileapp/hs/KT"
    //const val BASE_URL = "http://10.0.2.2/KT/hs/KT"

    object Endpoints {
        const val LOGIN = "/login"
        const val REGISTER = "/createUser"
        const val USERS = "/users"
        const val MESSAGES = "/messages"
        const val SEND_MESSAGE = "/send"
        const val PRODUCTS = "/items"
        const val ADD_PRODUCT = "/addItem"
        const val ADD_FCM_TOKEN = "/saveToken"
        const val FCM_TOKEN = "/tokenFCM"
    }
}