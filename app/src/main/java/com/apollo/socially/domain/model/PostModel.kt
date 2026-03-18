package com.apollo.socially.model

data class PostModel(
    val id: String,
    val userId: String,
    val username: String,
    val userHandle: String,
    val userAvatarRes: Int? = null,
    val userAvatarUrl: String? = null,
    val isVerified: Boolean = false,
    val imageRes: Int? = null,
    val imageUrl: String? = null,
    val imageResList: List<Int> = emptyList(),
    val musicLabel: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likedByAvatars: List<Int> = emptyList(),
    val caption: String = "",
    val timeAgo: String = "12h ago"
) {
    // Always use this in adapters — unifies single and multi-image
    val images: List<Int>
        get() = when {
            imageResList.isNotEmpty() -> imageResList
            imageRes != null -> listOf(imageRes)
            else -> emptyList()
        }
}