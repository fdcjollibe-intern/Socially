package com.apollo.socially.ui.auth.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.AuthRepositoryImpl
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.domain.model.User
import com.apollo.socially.utils.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels {
        val repository = AuthRepositoryImpl(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        ViewModelFactory(repository)
    }
    
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
                Toast.makeText(requireContext(), "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeSessionRepository()
        initializeViews(view)
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeSessionRepository() {
        val database = AppDatabase.getInstance(requireContext())
        sessionRepository = SessionRepositoryImpl(database.userSessionDao())
    }

    private fun initializeViews(view: View) {
        etEmail = view.findViewById(R.id.etEmail)
        etPassword = view.findViewById(R.id.etPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        ivTogglePassword = view.findViewById(R.id.ivTogglePassword)
        tvForgotPassword = view.findViewById(R.id.tvForgotPassword)
        tvJoinNow = view.findViewById(R.id.tvJoinNow)
        btnGoogle = view.findViewById(R.id.btnGoogle)
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
            Toast.makeText(requireContext(), "Forgot Password coming soon", Toast.LENGTH_SHORT).show()
        }

        tvJoinNow.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        btnGoogle.setOnClickListener {
            initiateGoogleSignIn()
        }

        view?.findViewById<CardView>(R.id.btnApple)?.setOnClickListener {
            Toast.makeText(requireContext(), "Apple sign-in coming soon", Toast.LENGTH_SHORT).show()
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

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        saveUserSessionAndNavigate(state.user)
                    }
                    is LoginState.Error -> {
                        btnLogin.isEnabled = true
                        btnLogin.text = "Log In →"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
    
    private fun saveUserSessionAndNavigate(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Save user session to Room database
            sessionRepository.saveUserSession(user)
            
            // Navigate to main container with bottom navigation
            val intent = Intent(requireContext(), com.apollo.socially.ui.main.MainContainerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}

