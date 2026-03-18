package com.apollo.socially.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.apollo.socially.MainActivity
import com.apollo.socially.R
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.ui.auth.login.LoginActivity
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    
    private lateinit var viewModel: SplashViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize ViewModel
        val database = AppDatabase.getInstance(this)
        val sessionRepository = SessionRepositoryImpl(database.userSessionDao())
        viewModel = SplashViewModel(sessionRepository)
        
        observeNavigationState()
    }
    
    private fun observeNavigationState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { state ->
                    when (state) {
                        is SplashNavigationState.Loading -> {
                            // Stay on splash screen
                        }
                        is SplashNavigationState.NavigateToLogin -> {
                            navigateToLogin()
                        }
                        is SplashNavigationState.NavigateToMain -> {
                            navigateToMain()
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

