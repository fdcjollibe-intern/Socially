package com.apollo.socially.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.NotificationRepository
import com.apollo.socially.domain.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit

class NotificationsViewModel(app: Application) : AndroidViewModel(app) {

    private val notificationRepository = NotificationRepository()

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val todayNotifications: List<Notification>,
            val yesterdayNotifications: List<Notification>,
            val last7DaysNotifications: List<Notification>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            android.util.Log.d("NotificationsVM", "Loading notifications...")
            
            notificationRepository.getUserNotifications()
                .onSuccess { notifications ->
                    android.util.Log.d("NotificationsVM", "Loaded ${notifications.size} notifications")
                    
                    val now = System.currentTimeMillis()
                    val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
                    val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
                    val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)

                    val today = mutableListOf<Notification>()
                    val yesterday = mutableListOf<Notification>()
                    val last7 = mutableListOf<Notification>()

                    notifications.forEach { notif ->
                        val createdTime = notif.createdAt?.time ?: 0
                        when {
                            createdTime >= oneDayAgo -> today.add(notif)
                            createdTime >= twoDaysAgo -> yesterday.add(notif)
                            createdTime >= sevenDaysAgo -> last7.add(notif)
                        }
                    }

                    android.util.Log.d("NotificationsVM", "Today: ${today.size}, Yesterday: ${yesterday.size}, Last7: ${last7.size}")

                    _uiState.value = UiState.Success(
                        todayNotifications = today,
                        yesterdayNotifications = yesterday,
                        last7DaysNotifications = last7
                    )
                }
                .onFailure { error ->
                    android.util.Log.e("NotificationsVM", "Failed to load: ${error.message}", error)
                    _uiState.value = UiState.Error(error.message ?: "Failed to load notifications")
                }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }
}

