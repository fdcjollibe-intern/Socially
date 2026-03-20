package com.apollo.socially.data.repository

import android.content.Context
import android.net.Uri
import com.apollo.socially.data.remote.cloudinary.CloudinaryUploader
import com.apollo.socially.data.remote.cloudinary.UploadResult
import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection("users")

    suspend fun getCurrentUser(): Result<User> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        usersCollection.document(uid).get().await()
            .toObject(User::class.java) ?: error("User not found")
    }

    suspend fun getUserById(userId: String): Result<User> = runCatching {
        usersCollection.document(userId).get().await()
            .toObject(User::class.java) ?: error("User not found")
    }

    // Check if username is taken by someone else (ignores current user)
    suspend fun isUsernameTaken(username: String): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val snapshot = usersCollection
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            // Taken only if found AND it belongs to a different user
            snapshot.documents.any { it.id != uid }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        usersCollection.document(uid).update(
            mapOf(
                "displayName" to displayName,
                "username"    to username,
                "bio"         to bio,
                "updatedAt"   to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun updateAvatar(
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        when (val result = CloudinaryUploader.uploadImage(
            context, uri, "socially/avatars/$uid", onProgress)
        ) {
            is UploadResult.Success -> {
                usersCollection.document(uid).update(
                    "profileImageUrl", result.url,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
                result.url
            }
            is UploadResult.Error -> throw result.exception
        }
    }

    suspend fun updateCoverPhoto(
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not logged in")
        when (val result = CloudinaryUploader.uploadImage(
            context, uri, "socially/covers/$uid", onProgress)
        ) {
            is UploadResult.Success -> {
                usersCollection.document(uid).update(
                    "profileCoverUrl", result.url,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
                result.url
            }
            is UploadResult.Error -> throw result.exception
        }
    }
}