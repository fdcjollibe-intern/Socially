package com.apollo.socially.ui.auth.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.apollo.socially.MainActivity
import com.apollo.socially.R
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.AuthRepositoryImpl
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.domain.model.User
import com.apollo.socially.ui.auth.register.RegisterActivity
import com.apollo.socially.utils.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var sessionRepository: SessionRepositoryImpl
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvJoinNow: TextView
    private lateinit var btnGoogle: CardView
    private var isPasswordVisible = false

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.loginWithGoogle(token)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViewModel()
        initializeViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViewModel() {
        val repository = AuthRepositoryImpl(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        val database = AppDatabase.getInstance(this)
        sessionRepository = SessionRepositoryImpl(database.userSessionDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvJoinNow = findViewById(R.id.tvJoinNow)
        btnGoogle = findViewById(R.id.btnGoogle)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            viewModel.login(email, password)
        }

        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password coming soon", Toast.LENGTH_SHORT).show()
        }

        tvJoinNow.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnGoogle.setOnClickListener {
            initiateGoogleSignIn()
        }

        findViewById<CardView>(R.id.btnApple).setOnClickListener {
            Toast.makeText(this, "Apple sign-in coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        etPassword.setSelection(etPassword.text.length)
    }

    private fun initiateGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is LoginState.Idle -> {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Log In →"
                    }
                    is LoginState.Loading -> {
                        btnLogin.isEnabled = false
                        btnLogin.text = "Logging in..."
                    }
                    is LoginState.Success -> {
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        saveUserSessionAndNavigate(state.user)
                    }
                    is LoginState.Error -> {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Log In →"
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
    
    private fun saveUserSessionAndNavigate(user: User) {
        lifecycleScope.launch {
            // Save user session to Room database
            sessionRepository.saveUserSession(user)
            
            // Navigate to main screen
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

