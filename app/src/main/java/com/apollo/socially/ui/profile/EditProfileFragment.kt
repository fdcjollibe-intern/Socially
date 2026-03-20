package com.apollo.socially.ui.profile

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.data.cache.ProfileCache
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.databinding.FragmentEditProfileBinding
import com.bumptech.glide.Glide
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    private var originalDisplayName = ""
    private var originalUsername = ""
    private var originalBio = ""
    private var hasNewAvatar = false

    // ── Pick image from gallery ────────────────────────────────
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        launchCropper(uri)
    }

    // ── Receive cropped result from uCrop ──────────────────────
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val croppedUri = UCrop.getOutput(data) ?: return@registerForActivityResult
            // Show preview immediately
            Glide.with(this).load(croppedUri).circleCrop().into(binding.editProfileAvatar)
            hasNewAvatar = true
            updateSaveButtonState()
            // Upload to Cloudinary
            viewModel.uploadAvatar(croppedUri)
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(data!!)
            Toast.makeText(requireContext(), "Crop failed: ${error?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editProfileBtnSave.isEnabled = false

        viewModel.loadUser()
        observeViewModel()
        setupButtons()
        setupChangeDetection()
    }

    // ── uCrop launcher ─────────────────────────────────────────
    private fun launchCropper(sourceUri: Uri) {
        val destFile = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)          // circular preview
            setShowCropGrid(false)
            setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
            setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))
            setCompressionQuality(90)
        }

        val intent = UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f)            // square crop
            .withMaxResultSize(800, 800)
            .withOptions(options)
            .getIntent(requireContext())

        cropLauncher.launch(intent)
    }

    // ── Observe ────────────────────────────────────────────────
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.user.collect { user ->
                user ?: return@collect
                originalDisplayName = user.displayName
                originalUsername    = user.username
                originalBio         = user.bio

                binding.editProfileName.setText(user.displayName)
                binding.editProfileUsername.setText(user.username)
                binding.editProfileBio.setText(user.bio)
                binding.editProfileBtnSave.isEnabled = false

                if (!user.profileImageUrl.isNullOrBlank()) {
                    Glide.with(this@EditProfileFragment)
                        .load(user.profileImageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.user_profile_placeholder_avatar)
                        .into(binding.editProfileAvatar)
                }
            }
        }

        // UiState
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is EditProfileViewModel.UiState.Loading -> {
                        binding.editProfileBtnSave.isEnabled = false
                        binding.editProfileBtnSave.text = "Saving..."
                    }
                    is EditProfileViewModel.UiState.Success -> {
                        binding.editProfileBtnSave.isEnabled = false
                        binding.editProfileBtnSave.text = "Save"
                        
                        // Invalidate cache so profile reloads with new data
                        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                            ProfileCache.invalidateCacheForUser(uid)
                        }
                        
                        // Save to Room cache
                        saveToRoom()
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                        findNavController().popBackStack()
                    }
                    is EditProfileViewModel.UiState.Error -> {
                        binding.editProfileBtnSave.isEnabled = true
                        binding.editProfileBtnSave.text = "Save"
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    else -> { /* idle */ }
                }
            }
        }

        // Upload progress
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uploadProgress.collect { progress ->
                if (progress > 0) {
                    binding.editProfileAvatarProgressOverlay.visibility = View.VISIBLE
                    binding.editProfileUploadPercent.visibility = View.VISIBLE
                    binding.editProfileUploadPercent.text = "Uploading $progress%"
                } else {
                    binding.editProfileAvatarProgressOverlay.visibility = View.GONE
                    binding.editProfileUploadPercent.visibility = View.GONE
                }
            }
        }

        // Username availability
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.usernameState.collect { state ->
                when (state) {
                    EditProfileViewModel.UsernameState.IDLE,
                    EditProfileViewModel.UsernameState.SAME -> {
                        binding.editProfileUsernameStatus.visibility = View.GONE
                        binding.editProfileUsernameIcon.visibility = View.GONE
                    }
                    EditProfileViewModel.UsernameState.CHECKING -> {
                        binding.editProfileUsernameStatus.visibility = View.VISIBLE
                        binding.editProfileUsernameStatus.text = "Checking..."
                        binding.editProfileUsernameStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.gray)
                        )
                        binding.editProfileUsernameIcon.visibility = View.GONE
                    }
                    EditProfileViewModel.UsernameState.AVAILABLE -> {
                        binding.editProfileUsernameStatus.visibility = View.VISIBLE
                        binding.editProfileUsernameStatus.text = "Username available ✓"
                        binding.editProfileUsernameStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.accent)
                        )
                        binding.editProfileUsernameIcon.visibility = View.GONE
                        updateSaveButtonState()
                    }
                    EditProfileViewModel.UsernameState.TAKEN -> {
                        binding.editProfileUsernameStatus.visibility = View.VISIBLE
                        binding.editProfileUsernameStatus.text = "Username already taken"
                        binding.editProfileUsernameStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.red)
                        )
                        binding.editProfileUsernameIcon.visibility = View.GONE
                        binding.editProfileBtnSave.isEnabled = false
                    }
                    EditProfileViewModel.UsernameState.INVALID -> {
                        binding.editProfileUsernameStatus.visibility = View.VISIBLE
                        binding.editProfileUsernameStatus.text = "Letters, numbers and _ only (min 3)"
                        binding.editProfileUsernameStatus.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.red)
                        )
                        binding.editProfileUsernameIcon.visibility = View.GONE
                        binding.editProfileBtnSave.isEnabled = false
                    }
                }
            }
        }
    }

    // ── Change detection ───────────────────────────────────────
    private fun setupChangeDetection() {
        val genericWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = updateSaveButtonState()
        }

        binding.editProfileName.addTextChangedListener(genericWatcher)
        binding.editProfileBio.addTextChangedListener(genericWatcher)

        // Bio char counter
        binding.editProfileBio.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                binding.editProfileBioCount.text = "$count / 150"
                binding.editProfileBioCount.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (count > 150) R.color.red else R.color.gray
                    )
                )
            }
        })

        // Username — debounced check
        binding.editProfileUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString() ?: ""
                viewModel.checkUsername(input)
                updateSaveButtonState()
            }
        })
    }

    private fun updateSaveButtonState() {
        val nameChanged     = binding.editProfileName.text?.toString()?.trim() != originalDisplayName
        val usernameChanged = binding.editProfileUsername.text?.toString()?.trim() != originalUsername
        val bioChanged      = binding.editProfileBio.text?.toString()?.trim() != originalBio

        val usState = viewModel.usernameState.value
        val usernameOk = usState == EditProfileViewModel.UsernameState.AVAILABLE ||
                usState == EditProfileViewModel.UsernameState.SAME ||
                usState == EditProfileViewModel.UsernameState.IDLE

        val hasChanges = nameChanged || usernameChanged || bioChanged || hasNewAvatar
        binding.editProfileBtnSave.isEnabled = hasChanges && usernameOk
    }

    // ── Buttons ────────────────────────────────────────────────
    private fun setupButtons() {
        binding.editProfileBtnBack.setOnClickListener { findNavController().popBackStack() }

        binding.editProfileAvatarContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.editProfileChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.editProfileBtnSave.setOnClickListener {
            viewModel.saveProfile(
                displayName = binding.editProfileName.text?.toString()?.trim() ?: "",
                username    = binding.editProfileUsername.text?.toString()?.trim() ?: "",
                bio         = binding.editProfileBio.text?.toString()?.trim() ?: ""
            )
        }
    }

    // ── Save to Room cache ─────────────────────────────────────
    private fun saveToRoom() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = viewModel.user.value ?: return@launch
            val db = AppDatabase.getInstance(requireContext())
            val sessionRepo = SessionRepositoryImpl(db.userSessionDao())
            // Update with latest values from fields
            val updated = user.copy(
                displayName = binding.editProfileName.text?.toString()?.trim() ?: user.displayName,
                username    = binding.editProfileUsername.text?.toString()?.trim() ?: user.username,
                bio         = binding.editProfileBio.text?.toString()?.trim() ?: user.bio
            )
            sessionRepo.saveUserSession(updated)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}