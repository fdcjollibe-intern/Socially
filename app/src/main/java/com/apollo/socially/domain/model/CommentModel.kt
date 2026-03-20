package com.apollo.socially.model

data class CommentModel(
    val id: String,
    val userId: String,
    val username: String,
    val avatarRes: Int? = null,
    val avatarUrl: String? = null,
    val text: String,
    val timeAgo: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isOwnComment: Boolean = false
)