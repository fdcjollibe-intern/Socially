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
    val imageResList: List<Int> = emptyList(),
    val imageUrlList: List<String> = emptyList(),
    val musicLabel: String? = null,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val likedByAvatars: List<Int> = emptyList(),
    val caption: String = "",
    val timeAgo: String = "12h ago",
    val isPlaceholder: Boolean = false,
    val displayName: String = "" // ADD — shown small under username in header
) : Parcelable {

    val imageUrls: List<String>
        get() = when {
            imageUrlList.isNotEmpty() -> imageUrlList
            imageUrl != null -> listOf(imageUrl)
            else -> emptyList()
        }

    val images: List<Int>
        get() = when {
            imageResList.isNotEmpty() -> imageResList
            imageRes != null -> listOf(imageRes)
            else -> emptyList()
        }

    // Detect if a URL is a video by extension or Cloudinary resource type
    fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("/video/") ||
                lower.endsWith(".mp4") ||
                lower.endsWith(".mov") ||
                lower.endsWith(".webm")
    }

    fun getMediaType(url: String): MediaType =
        if (isVideoUrl(url)) MediaType.VIDEO else MediaType.IMAGE

    enum class MediaType { IMAGE, VIDEO }
}