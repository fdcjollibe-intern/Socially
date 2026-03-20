package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FollowRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection("users")

    suspend fun getFollowers(userId: String): Result<List<User>> = runCatching {
        val followersSnapshot = firestore.collection("followers")
            .document(userId)
            .collection("userFollowers")
            .get()
            .await()

        val followerIds = followersSnapshot.documents.map { it.id }
        
        if (followerIds.isEmpty()) {
            return@runCatching emptyList()
        }

        // Fetch user details for each follower
        followerIds.chunked(10).flatMap { chunk ->
            usersCollection
                .whereIn("uid", chunk)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
        }
    }

    suspend fun getFollowing(userId: String): Result<List<User>> = runCatching {
        val followingSnapshot = firestore.collection("following")
            .document(userId)
            .collection("userFollowing")
            .get()
            .await()

        val followingIds = followingSnapshot.documents.map { it.id }
        
        if (followingIds.isEmpty()) {
            return@runCatching emptyList()
        }

        // Fetch user details for each following
        followingIds.chunked(10).flatMap { chunk ->
            usersCollection
                .whereIn("uid", chunk)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(User::class.java) }
        }
    }

    suspend fun followUser(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        // Add to current user's following
        firestore.collection("following")
            .document(currentUserId)
            .collection("userFollowing")
            .document(targetUserId)
            .set(mapOf(
                "userId" to targetUserId,
                "followedAt" to FieldValue.serverTimestamp()
            ))
            .await()

        // Add to target user's followers
        firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUserId)
            .set(mapOf(
                "userId" to currentUserId,
                "followedAt" to FieldValue.serverTimestamp()
            ))
            .await()

        // Update counts
        usersCollection.document(currentUserId)
            .update("followingCount", FieldValue.increment(1))
            .await()
        
        usersCollection.document(targetUserId)
            .update("followersCount", FieldValue.increment(1))
            .await()
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")

        // Remove from current user's following
        firestore.collection("following")
            .document(currentUserId)
            .collection("userFollowing")
            .document(targetUserId)
            .delete()
            .await()

        // Remove from target user's followers
        firestore.collection("followers")
            .document(targetUserId)
            .collection("userFollowers")
            .document(currentUserId)
            .delete()
            .await()

        // Decrement counts but never below 0
        val currentUserDoc = usersCollection.document(currentUserId).get().await()
        val targetUserDoc = usersCollection.document(targetUserId).get().await()

        val currentFollowingCount = currentUserDoc.getLong("followingCount")?.toInt() ?: 0
        val targetFollowersCount = targetUserDoc.getLong("followersCount")?.toInt() ?: 0

        if (currentFollowingCount > 0) {
            usersCollection.document(currentUserId)
                .update("followingCount", FieldValue.increment(-1))
                .await()
        }

        if (targetFollowersCount > 0) {
            usersCollection.document(targetUserId)
                .update("followersCount", FieldValue.increment(-1))
                .await()
        }
    }

    suspend fun isFollowing(targetUserId: String): Result<Boolean> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
        
        val doc = firestore.collection("following")
            .document(currentUserId)
            .collection("userFollowing")
            .document(targetUserId)
            .get()
            .await()
        
        doc.exists()
    }
}

