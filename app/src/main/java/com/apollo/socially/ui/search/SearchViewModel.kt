package com.apollo.socially.ui.search

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.FollowRepository
import com.apollo.socially.data.repository.SearchRepository
import com.apollo.socially.domain.model.User
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {

    private val searchRepository = SearchRepository()
    private val followRepository = FollowRepository()

    data class UserWithFollowState(
        val user: User,
        val isFollowing: Boolean
    )

    sealed class UiState {
        object Idle : UiState()
        object Searching : UiState()
        data class Success(
            val users: List<UserWithFollowState>,
            val hasMore: Boolean = false,
            val isLoadingMore: Boolean = false
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState
    
    private var searchJob: Job? = null
    private var currentQuery: String = ""
    private var lastDocument: DocumentSnapshot? = null
    private val allUsers = mutableListOf<UserWithFollowState>()
    
    // Debouncing: wait 800ms after user stops typing
    fun search(query: String) {
        searchJob?.cancel()
        
        val trimmedQuery = query.trim()
        
        if (trimmedQuery.isEmpty()) {
            _uiState.value = UiState.Idle
            currentQuery = ""
            allUsers.clear()
            lastDocument = null
            return
        }
        
        currentQuery = trimmedQuery
        
        searchJob = viewModelScope.launch {
            _uiState.value = UiState.Searching
            delay(800) // Wait 800ms (debounce)
            
            // Reset for new search
            allUsers.clear()
            lastDocument = null
            
            performSearch()
        }
    }
    
    fun loadMore() {
        val currentState = _uiState.value
        
        // Don't load if already loading or no more results
        if (currentState is UiState.Success && currentState.isLoadingMore) return
        if (currentState is UiState.Success && !currentState.hasMore) return
        
        // Update state to show loading more
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(isLoadingMore = true)
        }
        
        viewModelScope.launch {
            performSearch()
        }
    }
    
    private suspend fun performSearch() {
        searchRepository.searchUsers(currentQuery, lastDocument = lastDocument)
            .onSuccess { result ->
                Log.d("SearchVM", "Found ${result.users.size} users")
                
                // Check follow status for each user
                val usersWithFollowState = result.users.map { user ->
                    val isFollowing = followRepository.isFollowing(user.uid)
                        .getOrDefault(false)
                    UserWithFollowState(user, isFollowing)
                }
                
                allUsers.addAll(usersWithFollowState)
                lastDocument = result.lastDocument
                
                _uiState.value = UiState.Success(
                    users = allUsers.toList(),
                    hasMore = result.hasMore,
                    isLoadingMore = false
                )
            }
            .onFailure { error ->
                Log.e("SearchVM", "Search failed: ${error.message}", error)
                _uiState.value = UiState.Error(error.message ?: "Search failed")
            }
    }
    
    fun toggleFollow(userId: String, currentlyFollowing: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            Log.d("SearchVM", "Toggling follow for $userId, currently: $currentlyFollowing")
            
            val result = if (currentlyFollowing) {
                followRepository.unfollowUser(userId)
            } else {
                followRepository.followUser(userId)
            }
            
            result.onSuccess {
                val newState = !currentlyFollowing
                Log.d("SearchVM", "Follow toggled successfully, new state: $newState")
                
                // Update the follow state in our list
                val updatedUsers = allUsers.map { userWithState ->
                    if (userWithState.user.uid == userId) {
                        userWithState.copy(isFollowing = newState)
                    } else {
                        userWithState
                    }
                }
                allUsers.clear()
                allUsers.addAll(updatedUsers)
                
                // Update the UI state to trigger recomposition
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    _uiState.value = currentState.copy(users = allUsers.toList())
                }
                
                onComplete(newState)
            }.onFailure { error ->
                Log.e("SearchVM", "Follow toggle failed: ${error.message}", error)
                onComplete(currentlyFollowing) // Revert on error
            }
        }
    }
}

