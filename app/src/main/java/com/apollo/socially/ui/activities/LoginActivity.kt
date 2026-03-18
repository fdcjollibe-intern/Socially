package com.apollo.socially.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.apollo.socially.data.repository.AuthRepository
import com.apollo.socially.ui.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth

/**
 * Minimal Activity that demonstrates how to trigger Google Sign-In and observe AuthViewModel.
 * Replace layout and UI wiring with your xml later. This file focuses on wiring and MVVM.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    authViewModel.loginWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                // handle error
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.apollo.socially.R.layout.activity_login)

        // Initialize ViewModel with FirebaseAuth-backed repository
        val repo = AuthRepository(FirebaseAuth.getInstance())
        authViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(repo) as T
            }
        }).get(AuthViewModel::class.java)

        // Setup Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.apollo.socially.R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)

        // Wire your login/register buttons here; example for Google button:
        // findViewById<Button>(R.id.button_google).setOnClickListener {
        //     val signInIntent = googleClient.signInIntent
        //     googleLauncher.launch(signInIntent)
        // }

        // Observe ViewModel state (update UI accordingly)
        // lifecycleScope.launchWhenStarted {
        //    authViewModel.state.collect { state -> ... }
        // }
    }
}

