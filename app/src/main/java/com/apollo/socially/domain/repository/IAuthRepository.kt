package com.apollo.socially.domain.repository

import com.apollo.socially.domain.model.User
import com.google.firebase.auth.AuthResult

interface IAuthRepository {
    suspend fun registerWithEmail(email: String, password: String, firstName: String, lastName: String, username: String): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signInWithUsername(username: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun saveUserToFirestore(user: User)
    suspend fun getUserFromFirestore(uid: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun isEmailTaken(email: String): Boolean
    suspend fun isUsernameTaken(username: String): Boolean
    fun getCurrentUser(): User?
    fun signOut()
}

