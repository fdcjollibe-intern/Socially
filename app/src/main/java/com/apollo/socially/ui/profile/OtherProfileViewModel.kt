package com.apollo.socially.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.cache.OtherProfileCache
import com.apollo.socially.data.repository.FollowRepository
import com.apollo.socially.data.repository.PostRepository
import com.apollo.socially.data.repository.UserRepository
import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OtherProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val userRepository = UserRepository(app.applicationContext)
    private val postRepository = PostRepository()
    private val followRepository = FollowRepository()

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val user: User,
            val posts: List<Post>,
            val isFollowing: Boolean,
            val hasMore: Boolean = false,
            val isLoadingMore: Boolean = false
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private var currentUser: User? = null
    private var currentIsFollowing: Boolean = false
    private var lastDocument: DocumentSnapshot? = null
    private val allPosts = mutableListOf<Post>()
    private var currentUserId: String? = null

    fun loadUserProfile(userId: String) {
        currentUserId = userId

        // If valid cache exists, show it immediately then check for updates
        if (OtherProfileCache.hasCache(userId)) {
            val cachedUser = OtherProfileCache.getUser(userId)!!
            val cachedPosts = OtherProfileCache.getPosts(userId)!!
            val cachedIsFollowing = OtherProfileCache.getIsFollowing(userId)

            currentUser = cachedUser
            currentIsFollowing = cachedIsFollowing
            allPosts.clear()
            allPosts.addAll(cachedPosts)
            lastDocument = null

            _uiState.value = UiState.Success(
                user = cachedUser,
                posts = cachedPosts,
                isFollowing = cachedIsFollowing,
                hasMore = false
            )

            // Background check for updates
            checkForUpdates(userId)
            return
        }

        // No cache — full load with skeleton
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            allPosts.clear()
            lastDocument = null

            userRepository.getUserById(userId)
                .onSuccess { user ->
                    currentUser = user
                    currentIsFollowing = followRepository.isFollowing(userId).getOrDefault(false)
                    loadMorePosts()
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to load profile")
                }
        }
    }

    private fun checkForUpdates(userId: String) {
        viewModelScope.launch {
            userRepository.getUserById(userId)
                .onSuccess { freshUser ->
                    val cachedUser = currentUser ?: return@onSuccess
                    val currentState = _uiState.value as? UiState.Success ?: return@onSuccess

                    val postCountChanged = freshUser.postsCount != cachedUser.postsCount
                    val userInfoChanged = freshUser.displayName != cachedUser.displayName
                            || freshUser.username != cachedUser.username
                            || freshUser.bio != cachedUser.bio
                            || freshUser.profileImageUrl != cachedUser.profileImageUrl
                            || freshUser.followersCount != cachedUser.followersCount
                            || freshUser.followingCount != cachedUser.followingCount

                    when {
                        postCountChanged && freshUser.postsCount > cachedUser.postsCount -> {
                            // New posts — show shimmer placeholders then reload
                            val newCount = freshUser.postsCount - cachedUser.postsCount
                            val placeholders = (1..newCount).map {
                                Post(
                                    id = "placeholder_$it",
                                    userId = userId,
                                    mediaUrls = listOf("placeholder"),
                                    caption = "",
                                    likesCount = 0,
                                    commentsCount = 0
                                )
                            }
                            _uiState.value = currentState.copy(
                                user = freshUser,
                                posts = placeholders + allPosts
                            )

                            // Reload posts to replace placeholders
                            OtherProfileCache.invalidate(userId)
                            allPosts.clear()
                            lastDocument = null
                            currentUser = freshUser
                            loadMorePosts()
                        }

                        postCountChanged && freshUser.postsCount < cachedUser.postsCount -> {
                            // Posts deleted — reload grid silently
                            OtherProfileCache.invalidate(userId)
                            allPosts.clear()
                            lastDocument = null
                            currentUser = freshUser
                            loadMorePosts()
                        }

                        userInfoChanged -> {
                            // Only header info changed — update silently
                            currentUser = freshUser
                            OtherProfileCache.updateUser(userId, freshUser)
                            _uiState.value = currentState.copy(user = freshUser)
                        }

                        else -> { /* nothing changed */ }
                    }
                }
        }
    }

    fun loadMorePosts() {
        val user = currentUser ?: return
        val userId = currentUserId ?: return
        val currentState = _uiState.value

        if (currentState is UiState.Success && currentState.isLoadingMore) return

        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            postRepository.getUserPosts(userId, lastDocument = lastDocument)
                .onSuccess { postPage ->
                    allPosts.addAll(postPage.posts)
                    lastDocument = postPage.lastDocument

                    // Cache after first page load
                    if (lastDocument == null || !postPage.hasMore) {
                        OtherProfileCache.cache(
                            userId = userId,
                            user = user,
                            posts = allPosts.toList(),
                            isFollowing = currentIsFollowing
                        )
                    }

                    _uiState.value = UiState.Success(
                        user = user,
                        posts = allPosts.toList(),
                        isFollowing = currentIsFollowing,
                        hasMore = postPage.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure {
                    if (allPosts.isEmpty()) {
                        OtherProfileCache.cache(userId, user, emptyList(), currentIsFollowing)
                        _uiState.value = UiState.Success(
                            user = user,
                            posts = emptyList(),
                            isFollowing = currentIsFollowing,
                            hasMore = false,
                            isLoadingMore = false
                        )
                    } else {
                        _uiState.value = UiState.Success(
                            user = user,
                            posts = allPosts.toList(),
                            isFollowing = currentIsFollowing,
                            hasMore = false,
                            isLoadingMore = false
                        )
                    }
                }
        }
    }

    fun toggleFollow(userId: String, currentlyFollowing: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = if (currentlyFollowing) {
                followRepository.unfollowUser(userId)
            } else {
                followRepository.followUser(userId)
            }

            result.onSuccess {
                currentIsFollowing = !currentlyFollowing
                OtherProfileCache.updateIsFollowing(userId, currentIsFollowing)

                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    // Fetch fresh user data to get updated counts
                    userRepository.getUserById(userId)
                        .onSuccess { updatedUser ->
                            currentUser = updatedUser
                            OtherProfileCache.updateUser(userId, updatedUser)
                            
                            // Emit event for other screens
                            com.apollo.socially.data.repository.FollowEventBus.emit(
                                com.apollo.socially.data.repository.FollowEventBus.FollowEvent(
                                    targetUserId = userId,
                                    isNowFollowing = currentIsFollowing
                                )
                            )
                            
                            _uiState.value = currentState.copy(
                                user = updatedUser,
                                isFollowing = currentIsFollowing
                            )
                        }.onFailure {
                            _uiState.value = currentState.copy(isFollowing = currentIsFollowing)
                        }
                }
                onComplete(true)
            }.onFailure { error ->
                android.util.Log.e("OtherProfileVM", "Follow toggle failed: ${error.message}", error)
                onComplete(false)
            }
        }
    }



}