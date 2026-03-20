package com.apollo.socially.ui.story

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.StoryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StoryRowViewModel(app: Application) : AndroidViewModel(app) {

    private val storyRepository = StoryRepository()
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val rows: List<StoryRowItem>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    data class StoryRowItem(
        val userId: String,
        val username: String,
        val avatarUrl: String?,
        val hasUnseenStory: Boolean,
        val isMyStory: Boolean,
        val hasStory: Boolean, // false = show + icon only
        val userWithStories: StoryRepository.UserWithStories?
    )

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadStories()
    }

    fun loadStories() {
        viewModelScope.launch {
            // Fetch current user's avatar directly so "Your Story" always shows
            val currentUserAvatarUrl = runCatching {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUid)
                    .get()
                    .await()
                    .getString("profileImageUrl")
            }.getOrNull()

            storyRepository.getActiveStoriesWithUsers()
                .onSuccess { usersWithStories ->
                    val rows = mutableListOf<StoryRowItem>()
                    val myStories = usersWithStories.find { it.user.uid == currentUid }

                    // Always show "Your Story" first
                    rows.add(
                        StoryRowItem(
                            userId = currentUid,
                            username = "Your Story",
                            avatarUrl = currentUserAvatarUrl,
                            hasUnseenStory = false,
                            isMyStory = true,
                            hasStory = myStories != null,
                            userWithStories = myStories
                        )
                    )

                    // Add other users' stories
                    usersWithStories
                        .filter { it.user.uid != currentUid }
                        .forEach { uws ->
                            rows.add(
                                StoryRowItem(
                                    userId = uws.user.uid,
                                    username = uws.user.username,
                                    avatarUrl = uws.user.profileImageUrl,
                                    hasUnseenStory = uws.hasUnseenStory(currentUid),
                                    isMyStory = false,
                                    hasStory = true,
                                    userWithStories = uws
                                )
                            )
                        }

                    _uiState.value = UiState.Success(rows)
                }
                .onFailure {
                    // On failure, still show "Your Story"
                    _uiState.value = UiState.Success(
                        listOf(
                            StoryRowItem(
                                userId = currentUid,
                                username = "Your Story",
                                avatarUrl = currentUserAvatarUrl,
                                hasUnseenStory = false,
                                isMyStory = true,
                                hasStory = false,
                                userWithStories = null
                            )
                        )
                    )
                }
        }
    }


}