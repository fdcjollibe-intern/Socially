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

    companion object {
        const val PAGE_SIZE = 15
        const val COMMENTS_PAGE_SIZE = 10
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
        val likeRef = postsCollection.document(postId).collection("likes").document(uid)
        val postRef = postsCollection.document(postId)

        if (currentlyLiked) {
            likeRef.delete().await()
            postRef.update("likesCount", FieldValue.increment(-1)).await()
            false
        } else {
            likeRef.set(mapOf("userId" to uid, "likedAt" to FieldValue.serverTimestamp())).await()
            postRef.update("likesCount", FieldValue.increment(1)).await()
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

        Comment(
            id = ref.id,
            userId = uid,
            username = username,
            userAvatarUrl = avatarUrl,
            text = text
        )
    }
}