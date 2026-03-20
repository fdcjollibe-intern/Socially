package com.apollo.socially.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String? = null,
    val text: String = "",
    @ServerTimestamp val createdAt: Date? = null
)