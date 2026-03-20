package com.apollo.socially.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.FollowRepository
import com.apollo.socially.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FollowersFollowingViewModel(app: Application) : AndroidViewModel(app) {

    private val followRepository = FollowRepository()

    data class UserWithFollowState(
        val user: User,
        val isFollowing: Boolean
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val followers: List<UserWithFollowState>, 
            val following: List<UserWithFollowState>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
    
    private val allFollowers = mutableListOf<UserWithFollowState>()
    private val allFollowing = mutableListOf<UserWithFollowState>()

    fun loadFollowData(userId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val followersResult = followRepository.getFollowers(userId)
            val followingResult = followRepository.getFollowing(userId)

            if (followersResult.isSuccess && followingResult.isSuccess) {
                val followers = followersResult.getOrDefault(emptyList())
                val following = followingResult.getOrDefault(emptyList())
                
                // Check follow status for each user
                val followersWithState = followers.map { user ->
                    val isFollowing = followRepository.isFollowing(user.uid)
                        .getOrDefault(false)
                    UserWithFollowState(user, isFollowing)
                }
                
                val followingWithState = following.map { user ->
                    val isFollowing = followRepository.isFollowing(user.uid)
                        .getOrDefault(false)
                    UserWithFollowState(user, isFollowing)
                }
                
                allFollowers.clear()
                allFollowers.addAll(followersWithState)
                allFollowing.clear()
                allFollowing.addAll(followingWithState)
                
                _uiState.value = UiState.Success(
                    followers = allFollowers.toList(),
                    following = allFollowing.toList()
                )
            } else {
                val error = followersResult.exceptionOrNull() ?: followingResult.exceptionOrNull()
                _uiState.value = UiState.Error(error?.message ?: "Failed to load data")
            }
        }
    }

    fun toggleFollow(userId: String, isCurrentlyFollowing: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = if (isCurrentlyFollowing) {
                followRepository.unfollowUser(userId)
            } else {
                followRepository.followUser(userId)
            }
            
            result.onSuccess {
                val newState = !isCurrentlyFollowing
                
                // Update the follow state in both lists
                val updatedFollowers = allFollowers.map { userWithState ->
                    if (userWithState.user.uid == userId) {
                        userWithState.copy(isFollowing = newState)
                    } else {
                        userWithState
                    }
                }
                allFollowers.clear()
                allFollowers.addAll(updatedFollowers)
                
                val updatedFollowing = allFollowing.map { userWithState ->
                    if (userWithState.user.uid == userId) {
                        userWithState.copy(isFollowing = newState)
                    } else {
                        userWithState
                    }
                }
                allFollowing.clear()
                allFollowing.addAll(updatedFollowing)
                
                // Update the UI state to trigger recomposition
                _uiState.value = UiState.Success(
                    followers = allFollowers.toList(),
                    following = allFollowing.toList()
                )
                
                onComplete(newState)
            }.onFailure {
                onComplete(isCurrentlyFollowing) // Revert on error
            }
        }
    }
}

