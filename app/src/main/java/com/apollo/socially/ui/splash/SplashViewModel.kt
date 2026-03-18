package com.apollo.socially.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.domain.repository.ISessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashNavigationState {
    object Loading : SplashNavigationState()
    object NavigateToLogin : SplashNavigationState()
    object NavigateToMain : SplashNavigationState()
}

class SplashViewModel(
    private val sessionRepository: ISessionRepository
) : ViewModel() {
    
    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            // Delay for 3 seconds to show splash screen
            delay(3000)
            
            // Check if user is logged in from RoomDatabase
            val isLoggedIn = sessionRepository.isUserLoggedIn()
            
            _navigationState.value = if (isLoggedIn) {
                SplashNavigationState.NavigateToMain
            } else {
                SplashNavigationState.NavigateToLogin
            }
        }
    }
}

