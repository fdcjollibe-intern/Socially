package com.apollo.socially.data.repository

import com.apollo.socially.domain.model.User
import com.apollo.socially.domain.repository.IAuthRepository
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : IAuthRepository {

    override suspend fun registerWithEmail(email: String, password: String, firstName: String, lastName: String, username: String): AuthResult {
        val result = auth.createUserWithEmailAndPassword(email, password).await()

        result.user?.let { firebaseUser ->
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                firstName = firstName,
                lastName = lastName,
                username = username
            )
            saveUserToFirestore(user)
        }
        
        return result
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signInWithUsername(username: String, password: String): AuthResult {
        // Get user by username first
        val user = getUserByUsername(username)
            ?: throw Exception("Username not found")
        
        // Use their email to sign in
        return auth.signInWithEmailAndPassword(user.email, password).await()
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        
        // Check if user exists in Firestore, if not create one
        result.user?.let { firebaseUser ->
            val existingUser = getUserFromFirestore(firebaseUser.uid)
            if (existingUser == null) {
                val displayName = firebaseUser.displayName ?: ""
                val nameParts = displayName.split(" ", limit = 2)
                val firstName = nameParts.getOrNull(0) ?: ""
                val lastName = nameParts.getOrNull(1) ?: ""
                
                val user = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    firstName = firstName,
                    lastName = lastName,
                    username = firebaseUser.email?.substringBefore("@") ?: "",
                    profileImageUrl = firebaseUser.photoUrl?.toString()
                )
                saveUserToFirestore(user)
            }
        }
        
        return result
    }

    override suspend fun saveUserToFirestore(user: User) {
        firestore.collection("users")
            .document(user.uid)
            .set(user)
            .await()
    }

    override suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserByEmail(email: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getUserByUsername(username: String): User? {
        return try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
            snapshot.documents.firstOrNull()?.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun isEmailTaken(email: String): Boolean {
        return getUserByEmail(email) != null
    }

    override suspend fun isUsernameTaken(username: String): Boolean {
        return getUserByUsername(username) != null
    }

    override fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return firebaseUser?.let {
            User(
                uid = it.uid,
                email = it.email ?: "",
                username = it.displayName ?: "",
                profileImageUrl = it.photoUrl?.toString()
            )
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}

