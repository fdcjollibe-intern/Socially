package com.apollo.socially.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
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
    val imageResList: List<Int> = emptyList(), // For static sample data (backward compat)
    val imageUrlList: List<String> = emptyList(), // For real Firebase URLs
    val musicLabel: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likedByAvatars: List<Int> = emptyList(),
    val caption: String = "",
    val timeAgo: String = "12h ago",
    val isPlaceholder: Boolean = false // For showing gray box when new post detected
) : Parcelable {
    // Get image URLs as strings - prioritize URL list over single URL
    val imageUrls: List<String>
        get() = when {
            imageUrlList.isNotEmpty() -> imageUrlList
            imageUrl != null -> listOf(imageUrl)
            else -> emptyList()
        }
    
    // Legacy: Get images as Int resources (backward compat)
    val images: List<Int>
        get() = when {
            imageResList.isNotEmpty() -> imageResList
            imageRes != null -> listOf(imageRes)
            else -> emptyList()
        }
}