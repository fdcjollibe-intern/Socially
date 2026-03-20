package com.apollo.socially.data.repository

import android.content.Context
import android.net.Uri
import com.apollo.socially.data.remote.cloudinary.CloudinaryUploader
import com.apollo.socially.data.remote.cloudinary.UploadResult
import com.apollo.socially.domain.model.Story
import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class StoryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val storiesCollection = firestore.collection("stories")
    private val usersCollection = firestore.collection("users")

    // ── Upload story media to Cloudinary ──────────────────────

    suspend fun uploadStoryMedia(
        context: Context,
        uri: Uri,
        isVideo: Boolean,
        onProgress: (Int) -> Unit
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        val folder = if (isVideo) "socially/stories/videos/$uid"
                     else "socially/stories/images/$uid"

        return when (val result = if (isVideo) {
            CloudinaryUploader.uploadVideo(context, uri, folder, onProgress)
        } else {
            CloudinaryUploader.uploadImage(context, uri, folder, onProgress)
        }) {
            is UploadResult.Success -> Result.success(result.url)
            is UploadResult.Error -> Result.failure(result.exception)
        }
    }

    // ── Save story to Firestore ───────────────────────────────

    suspend fun createStory(
        mediaUrl: String,
        mediaType: String,
        caption: String
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val now = Date()
        val expiresAt = Date(now.time + 24 * 60 * 60 * 1000L) // +24 hours

        val data = hashMapOf(
            "userId" to uid,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "caption" to caption,
            "viewerIds" to emptyList<String>(),
            "isExpired" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "expiresAt" to com.google.firebase.Timestamp(expiresAt)
        )

        val ref = storiesCollection.add(data).await()
        ref.id
    }

    // ── Fetch active stories for home row ─────────────────────
    // Returns map of userId -> list of their active stories

    suspend fun getActiveStoriesWithUsers(): Result<List<UserWithStories>> = runCatching {
        val now = Date()

        val snapshot = storiesCollection
            .whereEqualTo("isExpired", false)
            .get().await()

        val stories = snapshot.documents.mapNotNull { doc ->
            val story = doc.toObject(Story::class.java)?.copy(id = doc.id) ?: return@mapNotNull null
            // Filter client-side for expired (expiresAt < now)
            if (story.expiresAt != null && story.expiresAt.before(now)) {
                // Mark as expired silently in background
                doc.reference.update("isExpired", true)
                return@mapNotNull null
            }
            story
        }

        // Group by userId
        val grouped = stories.groupBy { it.userId }

        // Fetch user info
        val result = mutableListOf<UserWithStories>()
        for ((userId, userStories) in grouped) {
            val userDoc = usersCollection.document(userId).get().await()
            val user = userDoc.toObject(User::class.java) ?: continue
            result.add(UserWithStories(user, userStories.sortedBy { it.createdAt }))
        }

        // Sort: current user first, then by most recent story
        val currentUid = auth.currentUser?.uid
        result.sortWith(compareByDescending<UserWithStories> {
            it.user.uid == currentUid
        }.thenByDescending {
            it.stories.lastOrNull()?.createdAt
        })

        result
    }

    // ── Get own stories ───────────────────────────────────────

    suspend fun getMyStories(): Result<List<Story>> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        val now = Date()

        val snapshot = storiesCollection
            .whereEqualTo("userId", uid)
            .whereEqualTo("isExpired", false)
            .get().await()

        snapshot.documents.mapNotNull { doc ->
            val story = doc.toObject(Story::class.java)?.copy(id = doc.id) ?: return@mapNotNull null
            if (story.expiresAt != null && story.expiresAt.before(now)) {
                doc.reference.update("isExpired", true)
                return@mapNotNull null
            }
            story
        }.sortedBy { it.createdAt }
    }

    // ── Mark story as viewed ──────────────────────────────────

    suspend fun markViewed(storyId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        storiesCollection.document(storyId)
            .update("viewerIds", FieldValue.arrayUnion(uid)).await()
    }

    // ── Delete story ──────────────────────────────────────────

    suspend fun deleteStory(storyId: String): Result<Unit> = runCatching {
        storiesCollection.document(storyId).delete().await()
    }

    // ── Data class ────────────────────────────────────────────

    data class UserWithStories(
        val user: User,
        val stories: List<Story>
    ) {
        fun hasUnseenStory(currentUid: String) =
            stories.any { !it.hasBeenViewedBy(currentUid) }
    }
}