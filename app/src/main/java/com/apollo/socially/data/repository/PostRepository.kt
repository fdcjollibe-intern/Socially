package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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
    }

    data class PostPage(
        val posts: List<Post>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )

    suspend fun getUserPosts(
        userId: String,
        limit: Int = PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<PostPage> = runCatching {
        var query = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
        
        // If we have a last document, start after it for pagination
        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }
        
        val snapshot = query.get().await()
        
        val posts = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Post::class.java)?.copy(id = doc.id)
        }
        
        val last = if (snapshot.documents.isNotEmpty()) {
            snapshot.documents.last()
        } else null
        
        PostPage(
            posts = posts,
            lastDocument = last,
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
}

