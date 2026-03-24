package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.Notification
import com.apollo.socially.domain.model.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val notificationsCollection = firestore.collection("notifications")

    companion object {
        const val PAGE_SIZE = 20
    }

    // ── Create Notification ────────────────────────────────────

    suspend fun createLikeNotification(
        postId: String,
        postOwnerId: String,
        postThumbnailUrl: String?
    ): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        // Don't create notification if liking own post
        if (currentUserId == postOwnerId) return@runCatching
        
        val userDoc = firestore.collection("users").document(currentUserId).get().await()
        val username = userDoc.getString("username") ?: "someone"
        val avatarUrl = userDoc.getString("profileImageUrl")

        val data = mapOf(
            "userId" to postOwnerId,
            "actorId" to currentUserId,
            "actorUsername" to username,
            "actorAvatarUrl" to avatarUrl,
            "type" to NotificationType.LIKE.name,
            "postId" to postId,
            "postThumbnailUrl" to postThumbnailUrl,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        notificationsCollection.add(data).await()
    }

    suspend fun createCommentNotification(
        postId: String,
        postOwnerId: String,
        commentId: String,
        commentText: String,
        postThumbnailUrl: String?
    ): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        // Don't create notification if commenting on own post
        if (currentUserId == postOwnerId) return@runCatching
        
        val userDoc = firestore.collection("users").document(currentUserId).get().await()
        val username = userDoc.getString("username") ?: "someone"
        val avatarUrl = userDoc.getString("profileImageUrl")

        val data = mapOf(
            "userId" to postOwnerId,
            "actorId" to currentUserId,
            "actorUsername" to username,
            "actorAvatarUrl" to avatarUrl,
            "type" to NotificationType.COMMENT.name,
            "postId" to postId,
            "commentId" to commentId,
            "commentText" to commentText,
            "postThumbnailUrl" to postThumbnailUrl,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )

        notificationsCollection.add(data).await()
    }

    // ── Delete Notification ────────────────────────────────────

    suspend fun deleteLikeNotification(postId: String): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        val query = notificationsCollection
            .whereEqualTo("actorId", currentUserId)
            .whereEqualTo("postId", postId)
            .whereEqualTo("type", NotificationType.LIKE.name)
            .get().await()

        query.documents.forEach { it.reference.delete().await() }
    }

    suspend fun deleteCommentNotification(commentId: String): Result<Unit> = runCatching {
        val query = notificationsCollection
            .whereEqualTo("commentId", commentId)
            .get().await()

        query.documents.forEach { it.reference.delete().await() }
    }

    // ── Fetch Notifications ────────────────────────────────────

    suspend fun getUserNotifications(limit: Int = PAGE_SIZE): Result<List<Notification>> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        val snapshot = notificationsCollection
            .whereEqualTo("userId", currentUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get().await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Notification::class.java)?.copy(id = doc.id)
        }
    }

    // ── Mark as Read ───────────────────────────────────────────

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        notificationsCollection.document(notificationId)
            .update("isRead", true).await()
    }

    suspend fun markAllAsRead(): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        val query = notificationsCollection
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("isRead", false)
            .get().await()

        query.documents.forEach {
            it.reference.update("isRead", true).await()
        }
    }
}

