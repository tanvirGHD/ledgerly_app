package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences

// SharedPreferences wrapper for persistent authentication token and user preferences
class TokenStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ledgerly_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, "User")
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, "")
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "BDT") ?: "BDT"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    fun saveAuth(token: String, name: String, email: String, currency: String = "BDT") {
        this.token = token
        this.userName = name
        this.userEmail = email
        this.currency = currency
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_NAME).remove(KEY_USER_EMAIL).apply()
    }

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()
}
