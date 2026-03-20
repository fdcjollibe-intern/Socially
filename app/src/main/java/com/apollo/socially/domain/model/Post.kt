package com.apollo.socially.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val mediaUrls: List<String> = emptyList(),
    val caption: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @ServerTimestamp val createdAt: Date? = null
)
