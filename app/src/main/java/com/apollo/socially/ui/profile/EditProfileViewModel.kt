package com.apollo.socially.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.UserRepository
import com.apollo.socially.domain.model.User
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class EditProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = UserRepository(app.applicationContext)

    // ── UI States ──────────────────────────────────────────────

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    enum class UsernameState {
        IDLE, CHECKING, AVAILABLE, TAKEN, INVALID, SAME
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    private val _usernameState = MutableStateFlow(UsernameState.IDLE)
    val usernameState: StateFlow<UsernameState> = _usernameState

    private var usernameDebounceJob: Job? = null
    private var originalUsername = ""

    // ── Load user ──────────────────────────────────────────────

    fun loadUser() {
        viewModelScope.launch {
            repository.getCurrentUser()
                .onSuccess {
                    _user.value = it
                    originalUsername = it.username
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to load user")
                }
        }
    }

    // ── Username availability check (debounced 500ms) ──────────

    fun checkUsername(input: String) {
        usernameDebounceJob?.cancel()

        val cleaned = input.trim()

        // Same as original — no need to check
        if (cleaned == originalUsername) {
            _usernameState.value = UsernameState.SAME
            return
        }

        // Validate format first
        if (cleaned.length < 3) {
            _usernameState.value = UsernameState.INVALID
            return
        }
        if (!cleaned.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _usernameState.value = UsernameState.INVALID
            return
        }

        _usernameState.value = UsernameState.CHECKING

        usernameDebounceJob = viewModelScope.launch {
            delay(500)
            try {
                val taken = repository.isUsernameTaken(cleaned)
                _usernameState.value = if (taken) UsernameState.TAKEN else UsernameState.AVAILABLE
            } catch (e: Exception) {
                Log.e("EditProfileVM", "Username check failed: ${e.message}")
                _usernameState.value = UsernameState.IDLE
            }
        }
    }

    // ── Save profile ───────────────────────────────────────────

    fun saveProfile(displayName: String, username: String, bio: String) {
        if (displayName.isBlank()) {
            _uiState.value = UiState.Error("Display name cannot be empty")
            return
        }
        val uState = _usernameState.value
        if (uState == UsernameState.TAKEN) {
            _uiState.value = UiState.Error("Username is already taken")
            return
        }
        if (uState == UsernameState.INVALID) {
            _uiState.value = UiState.Error("Invalid username format")
            return
        }
        if (uState == UsernameState.CHECKING) {
            _uiState.value = UiState.Error("Please wait, checking username...")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateProfile(displayName, username, bio)
                .onSuccess { _uiState.value = UiState.Success("Profile updated") }
                .onFailure { _uiState.value = UiState.Error(it.message ?: "Update failed") }
        }
    }

    // ── Upload avatar ──────────────────────────────────────────

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.updateAvatar(uri) { _uploadProgress.value = it }
                .onSuccess {
                    _uploadProgress.value = 0
                    _uiState.value = UiState.Success("Profile photo updated")
                    loadUser()
                }
                .onFailure {
                    _uploadProgress.value = 0
                    _uiState.value = UiState.Error(it.message ?: "Upload failed")
                }
        }
    }

    fun resetState() { _uiState.value = UiState.Idle }
}