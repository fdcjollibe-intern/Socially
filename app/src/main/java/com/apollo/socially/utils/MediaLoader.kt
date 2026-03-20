package com.apollo.socially.utils

import android.content.Context
import android.provider.MediaStore
import com.apollo.socially.model.MediaItem
import com.apollo.socially.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaLoader {

    suspend fun loadAll(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()

        // Load images
        val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE
        )
        context.contentResolver.query(
            imageUri, imageProjection, null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val size = cursor.getLong(sizeCol)
                val uri = android.content.ContentUris.withAppendedId(imageUri, id)
                items.add(MediaItem(id, uri, MediaType.IMAGE, sizeBytes = size))
            }
        }

        // Load videos
        val videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        context.contentResolver.query(
            videoUri, videoProjection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dur = cursor.getLong(durCol)
                val size = cursor.getLong(sizeCol)
                val uri = android.content.ContentUris.withAppendedId(videoUri, id)
                items.add(MediaItem(id, uri, MediaType.VIDEO, dur, size))
            }
        }

        // Sort all by date added (images + videos mixed)
        items.sortedByDescending { it.id }
    }
}
