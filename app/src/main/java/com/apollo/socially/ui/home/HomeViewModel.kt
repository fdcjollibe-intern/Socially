package com.apollo.socially.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.PostRepository
import com.apollo.socially.data.repository.UserRepository
import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val firestore = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository(app.applicationContext)
    private val postRepository = PostRepository()
    private val notificationRepository = com.apollo.socially.data.repository.NotificationRepository()

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val posts: List<PostModel>,
            val hasMore: Boolean = false,
            val isLoadingMore: Boolean = false
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    private val allPosts = mutableListOf<PostModel>()
    private var lastDocument: DocumentSnapshot? = null
    private val userCache = mutableMapOf<String, User>()

    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications

    companion object {
        const val PAGE_SIZE = 8
    }

    init {
        loadFeed()
        checkUnreadNotifications()
    }
    
    fun checkUnreadNotifications() {
        viewModelScope.launch {
            try {
                val notifications = notificationRepository.getUserNotifications(limit = 50).getOrNull() ?: emptyList()
                val hasUnread = notifications.any { it.isRead == false }
                _hasUnreadNotifications.value = hasUnread
            } catch (e: Exception) {
                // Ignore
            }
        }
    }



    private suspend fun Post.toPostModel(user: User): PostModel {
        // Fetch the liked state for this post
        val isLiked = postRepository.isLiked(id).getOrDefault(false)
        
        return PostModel(
            id = id,
            userId = userId,
            username = user.username,
            userHandle = "@${user.username}",
            displayName = user.displayName,
            userAvatarUrl = user.profileImageUrl,
            imageUrlList = mediaUrls,
            caption = caption,
            likeCount = likesCount,
            isLiked = isLiked,
            timeAgo = createdAt?.toTimeAgo() ?: "just now"
        )
    }

    private fun Date.toTimeAgo(): String {
        val diff = System.currentTimeMillis() - time
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 7 -> "${days / 7}w ago"
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "just now"
        }
    }


    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            allPosts.clear()
            lastDocument = null
            fetchPosts()
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state is UiState.Success && (state.isLoadingMore || !state.hasMore)) return
        if (state is UiState.Success) {
            _uiState.value = state.copy(isLoadingMore = true)
        }
        viewModelScope.launch { fetchPosts() }
    }

    private suspend fun fetchPosts() {
        try {
            var query = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE.toLong())

            if (lastDocument != null) query = query.startAfter(lastDocument!!)

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull {
                it.toObject(Post::class.java)?.copy(id = it.id)
            }

            lastDocument = snapshot.documents.lastOrNull()

            val postModels = posts.map { post ->
                val user = userCache.getOrPut(post.userId) {
                    userRepository.getUserById(post.userId).getOrNull() ?: User()
                }
                post.toPostModel(user)
            }

            allPosts.addAll(postModels)
            prefetchVideos(postModels) // ← this is the only addition

            _uiState.value = UiState.Success(
                posts = allPosts.toList(),
                hasMore = posts.size == PAGE_SIZE,
                isLoadingMore = false
            )
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Failed to load feed")
        }
    }

    private fun prefetchVideos(postModels: List<PostModel>) {
        postModels.forEach { post ->
            post.imageUrls.filter { post.isVideoUrl(it) }.forEach { videoUrl ->
                // Prefetch Cloudinary thumbnail for instant display
                val thumbnailUrl = com.apollo.socially.utils.CloudinaryUtils
                    .videoToThumbnailUrl(videoUrl)
                Glide.with(getApplication<android.app.Application>())
                    .load(thumbnailUrl)
                    .preload()
            }
        }
    }

    private val _showSpamWarning = MutableStateFlow<String?>(null)
    val showSpamWarning: StateFlow<String?> = _showSpamWarning
    
    fun toggleLike(post: PostModel) {
        viewModelScope.launch {
            val result = postRepository.toggleLike(post.id, post.isLiked)
            result.onSuccess { newLikedState ->
                // Update the post in the list
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    val updatedPosts = currentState.posts.map { p ->
                        if (p.id == post.id) {
                            p.copy(
                                isLiked = newLikedState,
                                likeCount = if (newLikedState) p.likeCount + 1 else p.likeCount - 1
                            )
                        } else {
                            p
                        }
                    }
                    // Find and update in allPosts as well
                    val index = allPosts.indexOfFirst { it.id == post.id }
                    if (index != -1) {
                        allPosts[index] = updatedPosts.first { it.id == post.id }
                    }
                    _uiState.value = currentState.copy(posts = updatedPosts)
                }
                // Refresh notification badge since we created/deleted a notification
                checkUnreadNotifications()
            }.onFailure { error ->
                _showSpamWarning.value = error.message
            }
        }
    }
    
    fun clearSpamWarning() {
        _showSpamWarning.value = null
    }

}