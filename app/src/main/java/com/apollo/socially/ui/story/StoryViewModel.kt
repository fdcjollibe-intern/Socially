package com.apollo.socially.ui.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.StoryRepository
import com.apollo.socially.domain.model.Story
import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StoryViewModel(app: Application) : AndroidViewModel(app) {

    private val storyRepository = StoryRepository()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    sealed class UiState {
        object Loading : UiState()
        data class Ready(
            val userGroups: List<StoryRepository.UserWithStories>,
            val startGroupIndex: Int = 0
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun loadForUser(userId: String) {
        viewModelScope.launch {
            storyRepository.getActiveStoriesWithUsers()
                .onSuccess { groups ->
                    val startIndex = groups.indexOfFirst { it.user.uid == userId }
                        .takeIf { it >= 0 } ?: 0
                    _uiState.value = UiState.Ready(groups, startIndex)
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed")
                }
        }
    }

    fun markStoryViewed(storyId: String) {
        viewModelScope.launch {
            storyRepository.markViewed(storyId)
        }
    }

    fun deleteStory(storyId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId)
                .onSuccess { onSuccess() }
        }
    }
}