package com.apollo.socially.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.domain.model.User
import com.apollo.socially.domain.repository.IAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: User) : RegisterState()
    data class Error(val message: String) : RegisterState()
    data class EmailTaken(val message: String) : RegisterState()
    data class UsernameTaken(val message: String) : RegisterState()
}

class RegisterViewModel(private val repository: IAuthRepository) : ViewModel() {

    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun register(
        displayName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (!validateInput(displayName, username, email, password, confirmPassword)) return

        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            try {
                if (repository.isEmailTaken(email)) {
                    _registerState.value = RegisterState.EmailTaken("Email is already registered")
                    return@launch
                }
                if (repository.isUsernameTaken(username)) {
                    _registerState.value = RegisterState.UsernameTaken("Username is already taken")
                    return@launch
                }

                val result = repository.registerWithEmail(email, password, displayName, username)
                val uid = result.user?.uid ?: throw Exception("User ID not found")
                val user = repository.getUserFromFirestore(uid)
                    ?: throw Exception("User data not found")

                _registerState.value = RegisterState.Success(user)
            } catch (e: Exception) {
                _registerState.value = RegisterState.Error(e.message ?: "Registration failed")
            }
        }
    }

    private fun validateInput(
        displayName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (displayName.isBlank()) {
            _registerState.value = RegisterState.Error("Display name is required")
            return false
        }
        if (displayName.length < 2) {
            _registerState.value = RegisterState.Error("Display name must be at least 2 characters")
            return false
        }
        if (username.isBlank()) {
            _registerState.value = RegisterState.Error("Username is required")
            return false
        }
        if (username.length < 3) {
            _registerState.value = RegisterState.Error("Username must be at least 3 characters")
            return false
        }
        if (!username.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _registerState.value = RegisterState.Error("Username can only contain letters, numbers, and underscore")
            return false
        }
        if (email.isBlank()) {
            _registerState.value = RegisterState.Error("Email is required")
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerState.value = RegisterState.Error("Invalid email format")
            return false
        }
        if (password.isBlank()) {
            _registerState.value = RegisterState.Error("Password is required")
            return false
        }
        if (password.length < 6) {
            _registerState.value = RegisterState.Error("Password must be at least 6 characters")
            return false
        }
        if (password != confirmPassword) {
            _registerState.value = RegisterState.Error("Passwords do not match")
            return false
        }
        return true
    }

    fun resetState() {
        _registerState.value = RegisterState.Idle
    }
}
