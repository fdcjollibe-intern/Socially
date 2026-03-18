package com.apollo.socially.ui.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.domain.model.User
import com.apollo.socially.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val repository: IAuthRepository) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(emailOrUsername: String, password: String) {
        if (!validateInput(emailOrUsername, password)) {
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = if (isEmail(emailOrUsername)) {
                    // Login with email
                    repository.signInWithEmail(emailOrUsername, password)
                } else {
                    // Login with username
                    repository.signInWithUsername(emailOrUsername, password)
                }
                
                // Fetch user data from Firestore
                val uid = result.user?.uid ?: throw Exception("User ID not found")
                val user = repository.getUserFromFirestore(uid) 
                    ?: throw Exception("User data not found")
                
                _loginState.value = LoginState.Success(user)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val result = repository.signInWithGoogle(idToken)
                
                // Fetch user data from Firestore
                val uid = result.user?.uid ?: throw Exception("User ID not found")
                val user = repository.getUserFromFirestore(uid) 
                    ?: throw Exception("User data not found")
                
                _loginState.value = LoginState.Success(user)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Google login failed")
            }
        }
    }

    private fun isEmail(input: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    private fun validateInput(emailOrUsername: String, password: String): Boolean {
        if (emailOrUsername.isBlank()) {
            _loginState.value = LoginState.Error("Email or username is required")
            return false
        }
        if (password.isBlank()) {
            _loginState.value = LoginState.Error("Password is required")
            return false
        }
        if (password.length < 6) {
            _loginState.value = LoginState.Error("Password must be at least 6 characters")
            return false
        }
        return true
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}
