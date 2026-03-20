package com.apollo.socially.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val durationMs: Long = 0L, // for video only
    val sizeBytes: Long = 0L   // file size in bytes
)

enum class MediaType { IMAGE, VIDEO }
