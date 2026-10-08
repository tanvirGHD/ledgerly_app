package com.example.myapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.TokenStore
import com.example.myapplication.model.LoginRequest
import com.example.myapplication.model.RegisterRequest
import com.example.myapplication.model.UpdatePasswordRequest
import com.example.myapplication.model.UpdateProfileRequest
import com.example.myapplication.repository.LedgerlyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

// ViewModel for managing User Auth, Token Persistence, Profile, and Dark Mode settings
class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: LedgerlyRepository = LedgerlyRepository()
    val tokenStore = TokenStore(application)

    private val _isLoggedIn = MutableStateFlow(tokenStore.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userName = MutableStateFlow(tokenStore.userName ?: "User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(tokenStore.userEmail ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _currency = MutableStateFlow(tokenStore.currency)
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _isDarkMode = MutableStateFlow(tokenStore.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repository.login(LoginRequest(email.trim(), password))
                tokenStore.saveAuth(res.token, res.user.name, res.user.email, res.user.currency)
                _userName.value = res.user.name
                _userEmail.value = res.user.email
                _currency.value = res.user.currency
                _isLoggedIn.value = true
                onSuccess()
            } catch (e: HttpException) {
                val err = parseHttpError(e)
                onError(err)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "login error", e)
                onError("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun register(name: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = repository.register(RegisterRequest(name.trim(), email.trim(), password))
                tokenStore.saveAuth(res.token, res.user.name, res.user.email, res.user.currency)
                _userName.value = res.user.name
                _userEmail.value = res.user.email
                _currency.value = res.user.currency
                _isLoggedIn.value = true
                onSuccess()
            } catch (e: HttpException) {
                val err = parseHttpError(e)
                onError(err)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "register error", e)
                onError("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun updateProfile(name: String, currency: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = repository.updateProfile(UpdateProfileRequest(name.trim(), currency))
                tokenStore.userName = user.name
                tokenStore.currency = user.currency
                _userName.value = user.name
                _currency.value = user.currency
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to update profile: ${e.localizedMessage}")
            }
        }
    }

    fun updatePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updatePassword(UpdatePasswordRequest(newPassword))
                onSuccess()
            } catch (e: Exception) {
                onError("Failed to update password: ${e.localizedMessage}")
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        tokenStore.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    fun logout() {
        tokenStore.clear()
        _isLoggedIn.value = false
    }

    private fun parseHttpError(e: HttpException): String {
        return try {
            val body = e.response()?.errorBody()?.string()
            if (!body.isNullOrBlank()) {
                val json = JSONObject(body)
                json.optString("error", "Authentication failed.")
            } else "Server error (${e.code()})"
        } catch (ex: Exception) {
            "Error: ${e.message()}"
        }
    }
}
