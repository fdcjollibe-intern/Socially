package com.apollo.socially.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Idle : AuthState
    object Loading : AuthState
    data class Success(val uid: String) : AuthState
    data class Error(val message: String?) : AuthState
}

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = repo.registerWithEmail(email, password)
                _state.value = AuthState.Success(res.user?.uid ?: "")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = repo.signInWithEmail(email, password)
                _state.value = AuthState.Success(res.user?.uid ?: "")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message)
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val res = repo.signInWithGoogle(idToken)
                _state.value = AuthState.Success(res.user?.uid ?: "")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message)
            }
        }
    }

    fun signOut() {
        repo.signOut()
        _state.value = AuthState.Idle
    }
}

