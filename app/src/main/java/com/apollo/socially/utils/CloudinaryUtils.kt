package com.apollo.socially.utils

object CloudinaryUtils {

    /**
     * Converts a Cloudinary video URL to a thumbnail image URL.
     * Example:
     * .../video/upload/v123/posts/video.mp4
     * becomes:
     * .../video/upload/so_0/v123/posts/video.jpg
     *
     * so_0 = snapshot at 0 seconds (first frame)
     */
    fun videoToThumbnailUrl(videoUrl: String): String {
        if (!videoUrl.contains("/video/upload/")) return videoUrl

        return videoUrl
            .replace("/video/upload/", "/video/upload/so_0/")
            .replaceLastExtension("jpg")
    }

    private fun String.replaceLastExtension(newExt: String): String {
        val dotIndex = lastIndexOf('.')
        return if (dotIndex != -1) substring(0, dotIndex + 1) + newExt
        else "$this.$newExt"
    }
}