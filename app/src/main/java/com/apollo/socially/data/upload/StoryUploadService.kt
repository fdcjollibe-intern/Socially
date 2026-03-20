package com.apollo.socially.data.upload

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.apollo.socially.R
import com.apollo.socially.data.repository.StoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class StoryUploadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storyRepository = StoryRepository()

    companion object {
        const val CHANNEL_ID = "story_upload_channel"
        const val NOTIF_ID = 2001
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_IS_VIDEO = "extra_is_video"
        const val EXTRA_CAPTION = "extra_caption"

        fun buildIntent(
            context: Context,
            uri: Uri,
            isVideo: Boolean,
            caption: String
        ): Intent = Intent(context, StoryUploadService::class.java).apply {
            putExtra(EXTRA_URI, uri)
            putExtra(EXTRA_IS_VIDEO, isVideo)
            putExtra(EXTRA_CAPTION, caption)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Preparing story…", 0))

        val uri = intent?.getParcelableExtra<Uri>(EXTRA_URI) ?: return START_NOT_STICKY
        val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""

        scope.launch {
            runUpload(uri, isVideo, caption)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun runUpload(uri: Uri, isVideo: Boolean, caption: String) {
        // Upload media
        val mediaResult = storyRepository.uploadStoryMedia(
            context = this,
            uri = uri,
            isVideo = isVideo,
            onProgress = { pct ->
                StoryUploadStateHolder.setState(
                    StoryUploadStateHolder.State.Uploading(pct)
                )
                updateNotification("Uploading story… $pct%", pct)
            }
        )

        if (mediaResult.isFailure) {
            Log.e("StoryUploadService", "Upload failed: ${mediaResult.exceptionOrNull()?.message}")
            StoryUploadStateHolder.setState(
                StoryUploadStateHolder.State.Error("Upload failed")
            )
            updateNotification("Story upload failed", 0)
            return
        }

        val mediaUrl = mediaResult.getOrThrow()
        val mediaType = if (isVideo) "video" else "image"

        // Save to Firestore
        storyRepository.createStory(mediaUrl, mediaType, caption)
            .onSuccess {
                StoryUploadStateHolder.setState(StoryUploadStateHolder.State.Success)
                updateNotification("Story shared!", 100)
                Log.d("StoryUploadService", "Story created: $it")
            }
            .onFailure {
                StoryUploadStateHolder.setState(
                    StoryUploadStateHolder.State.Error(it.message ?: "Failed to save story")
                )
                updateNotification("Failed to save story", 0)
            }
    }

    private fun buildNotification(text: String, progress: Int) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Socially")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_logo)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String, progress: Int) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Story Upload", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows story upload progress" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}