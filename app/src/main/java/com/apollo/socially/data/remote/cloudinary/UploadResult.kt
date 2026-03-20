package com.apollo.socially.data.remote.cloudinary


sealed class UploadResult {
    data class Success(
        val url: String,
        val publicId: String,
        val resourceType: String  // "image" or "video"
    ) : UploadResult()

    data class Error(val exception: Exception) : UploadResult()
}
