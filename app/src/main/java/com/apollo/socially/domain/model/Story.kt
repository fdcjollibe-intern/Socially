package com.apollo.socially.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Story(
    val id: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "image", // "image" or "video"
    val caption: String = "",
    val viewerIds: List<String> = emptyList(),
    val isExpired: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val expiresAt: Date? = null
) {
    fun isVideo() = mediaType == "video"
    fun hasBeenViewedBy(uid: String) = viewerIds.contains(uid)
}