package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.Comment
import com.apollo.socially.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val postsCollection = firestore.collection("posts")
    private val notificationRepository = NotificationRepository(auth, firestore)
    
    // Spam prevention
    private val likeToggleHistory = mutableMapOf<String, MutableList<Long>>()
    private val cooldownPosts = mutableSetOf<String>()
    
    companion object {
        const val PAGE_SIZE = 15
        const val COMMENTS_PAGE_SIZE = 8
        const val MAX_TOGGLES_PER_MINUTE = 3
        const val COOLDOWN_DURATION_MS = 5000L // 5 seconds
    }

    data class PostPage(
        val posts: List<Post>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )

    data class CommentPage(
        val comments: List<Comment>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )

    // ── Posts ──────────────────────────────────────────────────

    suspend fun getUserPosts(
        userId: String,
        limit: Int = PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PostPage> = runCatching {
        var query = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        if (lastDocument != null) query = query.startAfter(lastDocument)
        val snapshot = query.get().await()
        val posts = snapshot.documents.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
        PostPage(
            posts = posts,
            lastDocument = snapshot.documents.lastOrNull(),
            hasMore = posts.size == limit
        )
    }

    suspend fun getCurrentUserPosts(
        limit: Int = PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PostPage> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        getUserPosts(uid, limit, lastDocument).getOrThrow()
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).delete().await()
    }

    // ── Likes ──────────────────────────────────────────────────

    suspend fun isLiked(postId: String): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        postsCollection.document(postId)
            .collection("likes")
            .document(uid)
            .get().await()
            .exists()
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        
        // Check if post is in cooldown
        if (cooldownPosts.contains(postId)) {
            error("Please wait before liking again")
        }
        
        // Check spam prevention
        val now = System.currentTimeMillis()
        val history = likeToggleHistory.getOrPut(postId) { mutableListOf() }
        
        // Remove entries older than 1 minute
        history.removeAll { now - it > 60_000 }
        
        // Check if exceeded max toggles
        if (history.size >= MAX_TOGGLES_PER_MINUTE) {
            // Put in cooldown
            cooldownPosts.add(postId)
            kotlinx.coroutines.delay(COOLDOWN_DURATION_MS)
            cooldownPosts.remove(postId)
            error("Too many toggles. Please wait 5 seconds before trying again.")
        }
        
        // Add current toggle to history
        history.add(now)
        
        val likeRef = postsCollection.document(postId).collection("likes").document(uid)
        val postRef = postsCollection.document(postId)

        if (currentlyLiked) {
            // Unlike - delete the like document
            val likeDoc = likeRef.get().await()
            if (likeDoc.exists()) {
                likeRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            }
            
            // Delete the notification
            notificationRepository.deleteLikeNotification(postId)
            
            false
        } else {
            // Like - delete
            val existingLike = likeRef.get().await()
            if (existingLike.exists()) {
                likeRef.delete().await()
            }
            
            // Create new like document
            likeRef.set(mapOf("userId" to uid, "likedAt" to FieldValue.serverTimestamp())).await()
            postRef.update("likesCount", FieldValue.increment(1)).await()
            
            // Create notification
            val postDoc = postRef.get().await()
            val postOwnerId = postDoc.getString("userId") ?: ""
            val thumbnailUrl = postDoc.get("mediaUrls")?.let { 
                @Suppress("UNCHECKED_CAST")
                (it as? List<String>)?.firstOrNull()
            }
            
            notificationRepository.createLikeNotification(
                postId = postId,
                postOwnerId = postOwnerId,
                postThumbnailUrl = thumbnailUrl
            )
            
            true
        }
    }

    // ── Comments ───────────────────────────────────────────────

    suspend fun getComments(
        postId: String,
        lastDocument: DocumentSnapshot? = null
    ): Result<CommentPage> = runCatching {
        var query = postsCollection.document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(COMMENTS_PAGE_SIZE.toLong())
        if (lastDocument != null) query = query.startAfter(lastDocument)
        val snapshot = query.get().await()
        val comments = snapshot.documents.mapNotNull {
            it.toObject(Comment::class.java)?.copy(id = it.id)
        }
        CommentPage(
            comments = comments,
            lastDocument = snapshot.documents.lastOrNull(),
            hasMore = comments.size == COMMENTS_PAGE_SIZE
        )
    }

    suspend fun addComment(postId: String, text: String): Result<Comment> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val userDoc = firestore.collection("users").document(uid).get().await()
        val username = userDoc.getString("username") ?: "user"
        val avatarUrl = userDoc.getString("profileImageUrl")

        val data = mapOf(
            "userId" to uid,
            "username" to username,
            "userAvatarUrl" to avatarUrl,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        val ref = postsCollection.document(postId).collection("comments").add(data).await()
        postsCollection.document(postId).update("commentsCount", FieldValue.increment(1)).await()

        // Create notification
        val postDoc = postsCollection.document(postId).get().await()
        val postOwnerId = postDoc.getString("userId") ?: ""
        val thumbnailUrl = postDoc.get("mediaUrls")?.let { 
            @Suppress("UNCHECKED_CAST")
            (it as? List<String>)?.firstOrNull()
        }
        
        notificationRepository.createCommentNotification(
            postId = postId,
            postOwnerId = postOwnerId,
            commentId = ref.id,
            commentText = text,
            postThumbnailUrl = thumbnailUrl
        )

        Comment(
            id = ref.id,
            userId = uid,
            username = username,
            userAvatarUrl = avatarUrl,
            text = text
        )
    }
    
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = runCatching {
        postsCollection.document(postId).collection("comments").document(commentId).delete().await()
        postsCollection.document(postId).update("commentsCount", FieldValue.increment(-1)).await()
        
        // Delete the notification
        notificationRepository.deleteCommentNotification(commentId)
    }
}