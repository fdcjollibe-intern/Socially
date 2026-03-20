package com.apollo.socially.ui.auth.register

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.apollo.socially.MainActivity
import com.apollo.socially.R
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.AuthRepositoryImpl
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.domain.model.User
import com.apollo.socially.utils.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var viewModel: RegisterViewModel
    private lateinit var sessionRepository: SessionRepositoryImpl
    private lateinit var etDisplayName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var ivTogglePassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvLogin: TextView
    private lateinit var llEmail: LinearLayout
    private lateinit var llUsername: LinearLayout
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val repository = AuthRepositoryImpl(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance())
        val database = AppDatabase.getInstance(this)
        sessionRepository = SessionRepositoryImpl(database.userSessionDao())
        viewModel = ViewModelProvider(this, ViewModelFactory(repository))[RegisterViewModel::class.java]
        initializeViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        etDisplayName       = findViewById(R.id.etDisplayName)
        etUsername          = findViewById(R.id.etUsername)
        etEmail             = findViewById(R.id.etEmail)
        etPassword          = findViewById(R.id.etPassword)
        etConfirmPassword   = findViewById(R.id.etConfirmPassword)
        btnRegister         = findViewById(R.id.btnRegister)
        ivTogglePassword    = findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvLogin             = findViewById(R.id.tvLogin)
        llEmail             = findViewById(R.id.llEmail)
        llUsername          = findViewById(R.id.llUsername)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            resetFieldBorders()
            viewModel.register(
                displayName     = etDisplayName.text.toString().trim(),
                username        = etUsername.text.toString().trim(),
                email           = etEmail.text.toString().trim(),
                password        = etPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString()
            )
        }
        ivTogglePassword.setOnClickListener { togglePasswordVisibility() }
        ivToggleConfirmPassword.setOnClickListener { toggleConfirmPasswordVisibility() }
        tvLogin.setOnClickListener { finish() }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        etPassword.inputType = if (isPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etPassword.setSelection(etPassword.text.length)
    }

    private fun toggleConfirmPasswordVisibility() {
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        etConfirmPassword.inputType = if (isConfirmPasswordVisible)
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        etConfirmPassword.setSelection(etConfirmPassword.text.length)
    }

    private fun setFieldBorderColor(layout: LinearLayout, color: Int) {
        (layout.background as? GradientDrawable)?.setStroke(3, ContextCompat.getColor(this, color))
    }

    private fun resetFieldBorders() {
        setFieldBorderColor(llEmail, R.color.gray_border)
        setFieldBorderColor(llUsername, R.color.gray_border)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is RegisterState.Idle -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                    }
                    is RegisterState.Loading -> {
                        btnRegister.isEnabled = false
                        btnRegister.text = "Creating account..."
                    }
                    is RegisterState.Success -> {
                        btnRegister.isEnabled = true
                        Toast.makeText(this@RegisterActivity, "Registration successful!", Toast.LENGTH_SHORT).show()
                        saveSessionAndNavigate(state.user)
                    }
                    is RegisterState.EmailTaken -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        setFieldBorderColor(llEmail, R.color.red)
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    is RegisterState.UsernameTaken -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        setFieldBorderColor(llUsername, R.color.red)
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    is RegisterState.Error -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun saveSessionAndNavigate(user: User) {
        lifecycleScope.launch {
            sessionRepository.saveUserSession(user)
            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
