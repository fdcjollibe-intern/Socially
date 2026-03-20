package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SearchRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val usersCollection = firestore.collection("users")
    
    companion object {
        const val PAGE_SIZE = 8
    }

    data class SearchResult(
        val users: List<User>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )

    suspend fun searchUsers(
        query: String,
        limit: Int = PAGE_SIZE,
        lastDocument: DocumentSnapshot? = null
    ): Result<SearchResult> = runCatching {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        // Search by username (startsWith for better performance)
        val searchQuery = query.lowercase().trim()
        
        var firestoreQuery = usersCollection
            .whereGreaterThanOrEqualTo("username", searchQuery)
            .whereLessThan("username", searchQuery + "\uf8ff")
            .limit(limit.toLong())
        
        // Add cursor for pagination
        if (lastDocument != null) {
            firestoreQuery = firestoreQuery.startAfter(lastDocument)
        }
        
        val snapshot = firestoreQuery.get().await()
        
        val users = snapshot.documents
            .mapNotNull { it.toObject(User::class.java) }
            .filter { it.uid != currentUserId } // Exclude current user
        
        val last = if (snapshot.documents.isNotEmpty()) {
            snapshot.documents.last()
        } else null
        
        SearchResult(
            users = users,
            lastDocument = last,
            hasMore = users.size == limit
        )
    }
}

