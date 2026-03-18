package com.apollo.socially.data.repository

import android.app.Activity
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Simple repository that wraps FirebaseAuth and GoogleSignIn where needed.
 * Methods are suspendable so ViewModel can call them from viewModelScope.
 */
class AuthRepository(private val auth: FirebaseAuth) {

    suspend fun registerWithEmail(email: String, password: String) =
        auth.createUserWithEmailAndPassword(email, password).await()

    suspend fun signInWithEmail(email: String, password: String) =
        auth.signInWithEmailAndPassword(email, password).await()

    suspend fun signInWithGoogle(idToken: String) =
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()

    fun signOut() {
        auth.signOut()
    }
}

