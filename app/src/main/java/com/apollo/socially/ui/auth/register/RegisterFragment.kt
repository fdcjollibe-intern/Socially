package com.apollo.socially.ui.auth.register

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val viewModel: RegisterViewModel by viewModels {
        val repository = AuthRepositoryImpl(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
        ViewModelFactory(repository)
    }

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val database = AppDatabase.getInstance(requireContext())
        sessionRepository = SessionRepositoryImpl(database.userSessionDao())
        initializeViews(view)
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        etDisplayName       = view.findViewById(R.id.etDisplayName)
        etUsername          = view.findViewById(R.id.etUsername)
        etEmail             = view.findViewById(R.id.etEmail)
        etPassword          = view.findViewById(R.id.etPassword)
        etConfirmPassword   = view.findViewById(R.id.etConfirmPassword)
        btnRegister         = view.findViewById(R.id.btnRegister)
        ivTogglePassword    = view.findViewById(R.id.ivTogglePassword)
        ivToggleConfirmPassword = view.findViewById(R.id.ivToggleConfirmPassword)
        tvLogin             = view.findViewById(R.id.tvLogin)
        llEmail             = view.findViewById(R.id.llEmail)
        llUsername          = view.findViewById(R.id.llUsername)
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
        tvLogin.setOnClickListener { findNavController().navigateUp() }
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
        (layout.background as? GradientDrawable)?.setStroke(3, ContextCompat.getColor(requireContext(), color))
    }

    private fun resetFieldBorders() {
        setFieldBorderColor(llEmail, R.color.gray_border)
        setFieldBorderColor(llUsername, R.color.gray_border)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                        Toast.makeText(requireContext(), "Registration successful!", Toast.LENGTH_SHORT).show()
                        saveSessionAndNavigate(state.user)
                    }
                    is RegisterState.EmailTaken -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        setFieldBorderColor(llEmail, R.color.red)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    is RegisterState.UsernameTaken -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        setFieldBorderColor(llUsername, R.color.red)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    is RegisterState.Error -> {
                        btnRegister.isEnabled = true
                        btnRegister.text = "Create Account →"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun saveSessionAndNavigate(user: User) {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionRepository.saveUserSession(user)
            val intent = Intent(requireContext(), com.apollo.socially.ui.main.MainContainerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
