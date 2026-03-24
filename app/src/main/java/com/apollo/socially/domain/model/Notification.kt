package com.apollo.socially.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Notification(
    val id: String = "",
    val userId: String = "", // Who receives this notification
    val actorId: String = "", // Who performed the action
    val actorUsername: String = "",
    val actorAvatarUrl: String? = null,
    val type: NotificationType = NotificationType.LIKE,
    val postId: String? = null,
    val commentId: String? = null,
    val postThumbnailUrl: String? = null,
    val commentText: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null
)

enum class NotificationType {
    LIKE,
    COMMENT,
    FOLLOW
}

