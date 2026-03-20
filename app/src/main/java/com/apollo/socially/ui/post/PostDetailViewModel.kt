package com.apollo.socially.ui.post

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apollo.socially.data.repository.PostRepository
import com.apollo.socially.domain.model.Comment
import com.apollo.socially.model.CommentModel
import com.apollo.socially.model.PostModel
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PostDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val postRepository = PostRepository()

    // ── Like state ─────────────────────────────────────────────
    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount

    private var likeDebounceJob: Job? = null
    private var isLikeInProgress = false

    // ── Comments ───────────────────────────────────────────────
    sealed class CommentsState {
        object Loading : CommentsState()
        data class Success(
            val comments: List<CommentModel>,
            val hasMore: Boolean,
            val isLoadingMore: Boolean = false
        ) : CommentsState()
        data class Error(val message: String) : CommentsState()
    }

    private val _commentsState = MutableStateFlow<CommentsState>(CommentsState.Loading)
    val commentsState: StateFlow<CommentsState> = _commentsState

    private val allComments = mutableListOf<CommentModel>()
    private var lastCommentDoc: DocumentSnapshot? = null
    private var currentPostId: String? = null

    // ── Init ───────────────────────────────────────────────────

    fun init(post: PostModel) {
        currentPostId = post.id
        _likeCount.value = post.likeCount
        _isLiked.value = post.isLiked

        // Check actual liked state from Firestore
        viewModelScope.launch {
            postRepository.isLiked(post.id)
                .onSuccess { _isLiked.value = it }
        }

        loadComments()
    }

    // ── Like toggle with debounce + anti-spam ──────────────────

    fun toggleLike() {
        if (isLikeInProgress) return
        val postId = currentPostId ?: return

        // Optimistic update
        val wasLiked = _isLiked.value
        _isLiked.value = !wasLiked
        _likeCount.value = (_likeCount.value + if (!wasLiked) 1 else -1).coerceAtLeast(0)

        likeDebounceJob?.cancel()
        likeDebounceJob = viewModelScope.launch {
            isLikeInProgress = true
            delay(300) // debounce rapid taps
            postRepository.toggleLike(postId, wasLiked)
                .onFailure {
                    // Revert on failure
                    _isLiked.value = wasLiked
                    _likeCount.value = (_likeCount.value + if (wasLiked) 1 else -1).coerceAtLeast(0)
                }
            isLikeInProgress = false
        }
    }

    // ── Comments ───────────────────────────────────────────────

    fun loadComments() {
        val postId = currentPostId ?: return
        allComments.clear()
        lastCommentDoc = null

        viewModelScope.launch {
            _commentsState.value = CommentsState.Loading
            fetchComments(postId)
        }
    }

    fun loadMoreComments() {
        val postId = currentPostId ?: return
        val state = _commentsState.value
        if (state is CommentsState.Success && (state.isLoadingMore || !state.hasMore)) return

        viewModelScope.launch {
            if (state is CommentsState.Success) {
                _commentsState.value = state.copy(isLoadingMore = true)
            }
            fetchComments(postId)
        }
    }

    private suspend fun fetchComments(postId: String) {
        postRepository.getComments(postId, lastCommentDoc)
            .onSuccess { page ->
                lastCommentDoc = page.lastDocument
                val newModels = page.comments.map { it.toModel() }
                allComments.addAll(newModels)
                _commentsState.value = CommentsState.Success(
                    comments = allComments.toList(),
                    hasMore = page.hasMore
                )
            }
            .onFailure {
                _commentsState.value = CommentsState.Error(it.message ?: "Failed to load comments")
            }
    }

    fun sendComment(text: String) {
        val postId = currentPostId ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            postRepository.addComment(postId, text)
                .onSuccess { comment ->
                    val model = comment.toModel(isOwn = true)
                    allComments.add(0, model) // show at top temporarily
                    val state = _commentsState.value
                    if (state is CommentsState.Success) {
                        _commentsState.value = state.copy(
                            comments = allComments.toList()
                        )
                    } else {
                        _commentsState.value = CommentsState.Success(
                            comments = allComments.toList(),
                            hasMore = false
                        )
                    }
                }
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun Comment.toModel(isOwn: Boolean = false): CommentModel {
        return CommentModel(
            id = id,
            userId = userId,
            username = username,
            avatarUrl = userAvatarUrl,
            text = text,
            timeAgo = createdAt?.toTimeAgo() ?: "just now",
            isOwnComment = isOwn
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
}