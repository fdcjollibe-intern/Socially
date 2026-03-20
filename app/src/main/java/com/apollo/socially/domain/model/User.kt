package com.apollo.socially.domain.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val profileImageUrl: String? = null,
    val profileCoverUrl: String? = null,

    @get:PropertyName("isVerified")
    @set:PropertyName("isVerified")
    var isVerified: Boolean = false,

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean? = null,

    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,

    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)