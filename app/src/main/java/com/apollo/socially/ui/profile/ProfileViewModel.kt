package com.apollo.socially.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.cache.ProfileCache
import com.apollo.socially.data.repository.FollowEventBus
import com.apollo.socially.data.repository.PostRepository
import com.apollo.socially.data.repository.UserRepository
import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(app: Application) : AndroidViewModel(app) {

    private val userRepository = UserRepository(app.applicationContext)
    private val postRepository = PostRepository()
    private val auth = FirebaseAuth.getInstance()

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val user: User, 
            val posts: List<Post>,
            val hasMore: Boolean = false,
            val isLoadingMore: Boolean = false
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
    
    private var currentUser: User? = null
    private var lastDocument: DocumentSnapshot? = null
    private val allPosts = mutableListOf<Post>()

    init {
        loadUser()
    }

    private fun observeFollowEvents() {
        viewModelScope.launch {
            FollowEventBus.events.collect { event ->
                // When we follow/unfollow someone, our own followingCount changes
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val user = currentState.user
                    val delta = if (event.isNowFollowing) 1 else -1
                    val updatedUser = user.copy(
                        followingCount = (user.followingCount + delta).coerceAtLeast(0)
                    )
                    currentUser = updatedUser
                    _uiState.value = currentState.copy(user = updatedUser)
                }
            }
        }
    }

    fun loadUser(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                _uiState.value = UiState.Error("Not logged in")
                return@launch
            }

            // Try cache first
            if (!forceRefresh && ProfileCache.hasCacheForUser(userId)) {
                val cachedUser = ProfileCache.getUserCache(userId)
                val cachedPosts = ProfileCache.getPostsCache(userId)

                if (cachedUser != null && cachedPosts != null) {
                    currentUser = cachedUser
                    // ── FIX: restore allPosts from cache so loadMorePosts
                    //    doesn't re-append the same page on resume
                    allPosts.clear()
                    allPosts.addAll(cachedPosts)
                    lastDocument = null // background check will reset properly

                    _uiState.value = UiState.Success(
                        user = cachedUser,
                        posts = cachedPosts,
                        hasMore = false
                    )

                    checkForUpdates(userId)
                    return@launch
                }
            }

            // No cache or force refresh
            _uiState.value = UiState.Loading
            allPosts.clear()
            lastDocument = null

            userRepository.getCurrentUser()
                .onSuccess { user ->
                    currentUser = user
                    loadMorePosts()
                }
                .onFailure {
                    _uiState.value = UiState.Error(it.message ?: "Failed to load profile")
                }
        }
    }

    private suspend fun checkForUpdates(userId: String) {
        userRepository.getCurrentUser()
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
                    postCountChanged -> {
                        val newPostCount = ProfileCache.getNewPostCount(userId, freshUser.postsCount)

                        if (newPostCount > 0) {
                            // New posts — show shimmer placeholders, then reload grid
                            val placeholders = (1..newPostCount).map {
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

                            // Now do a real reload to replace placeholders with actual posts
                            ProfileCache.invalidateCache()
                            allPosts.clear()
                            lastDocument = null
                            currentUser = freshUser
                            loadMorePosts()

                        } else {
                            // Posts were deleted — full reload
                            ProfileCache.invalidateCache()
                            loadUser(forceRefresh = true)
                        }
                    }

                    userInfoChanged -> {
                        // Only user info changed — update header silently, no grid reload
                        currentUser = freshUser
                        ProfileCache.updateUserInCache(userId, freshUser)
                        _uiState.value = currentState.copy(user = freshUser)
                    }

                    else -> {
                        // Nothing changed, no-op
                    }
                }
            }
    }
    
    fun loadMorePosts() {
        val user = currentUser ?: return
        val userId = auth.currentUser?.uid ?: return
        val currentState = _uiState.value
        
        // Don't load if already loading
        if (currentState is UiState.Success && currentState.isLoadingMore) return
        
        // Update state to show loading more indicator
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(isLoadingMore = true)
        }
        
        viewModelScope.launch {
            postRepository.getCurrentUserPosts(lastDocument = lastDocument)
                .onSuccess { postPage ->
                    allPosts.addAll(postPage.posts)
                    lastDocument = postPage.lastDocument
                    
                    Log.d("ProfileVM", "Loaded ${postPage.posts.size} posts, total: ${allPosts.size}, hasMore: ${postPage.hasMore}")
                    
                    // Cache the data
                    ProfileCache.cacheUserData(userId, user, allPosts.toList())
                    
                    _uiState.value = UiState.Success(
                        user = user,
                        posts = allPosts.toList(),
                        hasMore = postPage.hasMore,
                        isLoadingMore = false
                    )
                }
                .onFailure { error ->
                    Log.e("ProfileVM", "Failed to load posts: ${error.message}", error)
                    // If this is first load, show empty list
                    if (allPosts.isEmpty()) {
                        ProfileCache.cacheUserData(userId, user, emptyList())
                        _uiState.value = UiState.Success(
                            user = user,
                            posts = emptyList(),
                            hasMore = false,
                            isLoadingMore = false
                        )
                    } else {
                        // If loading more failed, just stop the loading indicator
                        _uiState.value = UiState.Success(
                            user = user,
                            posts = allPosts.toList(),
                            hasMore = false,
                            isLoadingMore = false
                        )
                    }
                }
        }
    }

    suspend fun isPostLiked(postId: String): Boolean {
        return postRepository.isLiked(postId).getOrDefault(false)
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean, onSuccess: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            val result = postRepository.toggleLike(postId, currentlyLiked)
            result.onSuccess { newLikedState ->
                val newLikeCount = if (newLikedState && !currentlyLiked) {
                    1
                } else if (!newLikedState && currentlyLiked) {
                    -1
                } else {
                    0
                }
                onSuccess(newLikedState, newLikeCount)
            }
        }
    }
}