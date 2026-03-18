package com.apollo.socially.domain.model

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val profileCoverUrl: String? = null,
    val isVerified: Boolean = false,
    val isOnline: Boolean? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)