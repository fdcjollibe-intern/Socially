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
import com.apollo.socially.data.cache.ProfileCache
import com.apollo.socially.data.remote.cloudinary.CloudinaryUploader
import com.apollo.socially.data.remote.cloudinary.UploadResult
import com.apollo.socially.data.upload.UploadStateHolder.UploadState
import com.apollo.socially.model.MediaType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PostUploadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val CHANNEL_ID    = "post_upload_channel"
        const val NOTIF_ID      = 1001
        const val EXTRA_URIS    = "extra_uris"
        const val EXTRA_TYPES   = "extra_types"
        const val EXTRA_CAPTION = "extra_caption"

        fun buildIntent(
            context: Context,
            uris: List<Uri>,
            types: List<String>,
            caption: String
        ): Intent = Intent(context, PostUploadService::class.java).apply {
            putParcelableArrayListExtra(EXTRA_URIS, ArrayList(uris))
            putStringArrayListExtra(EXTRA_TYPES, ArrayList(types))
            putExtra(EXTRA_CAPTION, caption)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Preparing upload…", 0))

        val uris    = intent?.getParcelableArrayListExtra<Uri>(EXTRA_URIS) ?: return START_NOT_STICKY
        val types   = intent.getStringArrayListExtra(EXTRA_TYPES) ?: return START_NOT_STICKY
        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""

        scope.launch {
            runUpload(uris, types, caption)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun runUpload(uris: List<Uri>, types: List<String>, caption: String) {
        val uid = auth.currentUser?.uid ?: run {
            UploadStateHolder.setState(UploadState.Error("Not logged in"))
            return
        }

        val total = uris.size
        val uploadedUrls = mutableListOf<String>()

        uris.forEachIndexed { index, uri ->
            val isVideo = types.getOrNull(index) == "video"
            val folder  = if (isVideo) "socially/posts/videos/$uid" else "socially/posts/images/$uid"

            val result = if (isVideo) {
                CloudinaryUploader.uploadVideo(this, uri, folder) { pct ->
                    val overall = ((index * 100) + pct) / total
                    UploadStateHolder.setState(UploadState.Uploading(overall, index + 1, total))
                    updateNotification("Uploading ${index + 1}/$total · $overall%", overall)
                }
            } else {
                CloudinaryUploader.uploadImage(this, uri, folder) { pct ->
                    val overall = ((index * 100) + pct) / total
                    UploadStateHolder.setState(UploadState.Uploading(overall, index + 1, total))
                    updateNotification("Uploading ${index + 1}/$total · $overall%", overall)
                }
            }

            when (result) {
                is UploadResult.Success -> uploadedUrls.add(result.url)
                is UploadResult.Error   -> {
                    Log.e("PostUploadService", "Upload failed: ${result.exception.message}")
                    UploadStateHolder.setState(UploadState.Error(result.exception.message ?: "Upload failed"))
                    updateNotification("Upload failed", 0)
                    return
                }
            }
        }

        try {
            val post = hashMapOf(
                "userId"        to uid,
                "mediaUrls"     to uploadedUrls,
                "caption"       to caption,
                "likesCount"    to 0,
                "commentsCount" to 0,
                "createdAt"     to FieldValue.serverTimestamp()
            )
            val docRef = firestore.collection("posts").add(post).await()
            firestore.collection("users").document(uid)
                .update("postsCount", FieldValue.increment(1)).await()

            // Invalidate profile cache so it reloads with new post
            ProfileCache.invalidateCacheForUser(uid)
            
            UploadStateHolder.setState(UploadState.Success(docRef.id))
            updateNotification("Post shared!", 100)
            Log.d("PostUploadService", "Post created: ${docRef.id}")

        } catch (e: Exception) {
            Log.e("PostUploadService", "Firestore failed: ${e.message}", e)
            UploadStateHolder.setState(UploadState.Error(e.message ?: "Failed to save post"))
            updateNotification("Failed to save post", 0)
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
            CHANNEL_ID, "Post Upload", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shows post upload progress" }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
