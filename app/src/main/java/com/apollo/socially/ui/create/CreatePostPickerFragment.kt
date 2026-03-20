package com.apollo.socially.ui.create

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentCreatePostPickerBinding
import com.apollo.socially.model.MediaItem
import com.apollo.socially.utils.MediaLoader
import com.apollo.socially.utils.MediaPermissionHelper
import com.apollo.socially.utils.PostDraftStore
import kotlinx.coroutines.launch

class CreatePostPickerFragment : Fragment() {

    private var _binding: FragmentCreatePostPickerBinding? = null
    private val binding get() = _binding!!

    private lateinit var gridAdapter: MediaGridAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        MediaPermissionHelper.handleResult(this, results) { loadMedia() }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Navigate to caption with the captured photo
            // cameraUri is set before launch
            cameraUri?.let { uri ->
                val item = MediaItem(
                    System.currentTimeMillis(), 
                    uri,
                    com.apollo.socially.model.MediaType.IMAGE,
                    sizeBytes = 0L // Size will be checked during upload
                )
                navigateToCaption(listOf(item))
            }
        }
    }

    private var cameraUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGrid()
        setupButtons()
        checkPermissionsAndLoad()
    }

    private fun setupGrid() {
        gridAdapter = MediaGridAdapter(
            onCameraClick = { openCamera() },
            onMediaClick = { item ->
                // Single select — immediately enable Next
                updateNextButton()
            }
        )

        binding.pickerGridRv.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
            addItemDecoration(com.apollo.socially.ui.profile.GridSpacingDecoration(3, 2))
        }
    }

    private fun setupButtons() {
        binding.pickerBtnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.pickerBtnMulti.setOnClickListener {
            gridAdapter.toggleMultiSelectMode()
            val isMulti = gridAdapter.isMultiSelectMode
            binding.pickerBtnMulti.text = if (isMulti) "Single" else "Select multiple"
            updateNextButton()
        }

        binding.pickerBtnNext.setOnClickListener {
            val selected = gridAdapter.getSelectedItems()
            if (selected.isNotEmpty()) navigateToCaption(selected)
        }
    }

    private fun updateNextButton() {
        val hasSelection = gridAdapter.getSelectedItems().isNotEmpty()
        binding.pickerBtnNext.visibility = if (hasSelection) View.VISIBLE else View.GONE
    }

    private fun checkPermissionsAndLoad() {
        MediaPermissionHelper.checkAndRequest(this, permissionLauncher) { loadMedia() }
    }

    private fun loadMedia() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = MediaLoader.loadAll(requireContext())
            gridAdapter.submitList(items)
        }
    }

    private fun openCamera() {
        val photoUri = createCameraUri()
        cameraUri = photoUri
        cameraLauncher.launch(photoUri)
    }

    private fun createCameraUri(): Uri {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "socially_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        )!!
    }

    private fun navigateToCaption(items: List<MediaItem>) {
        PostDraftStore.saveDraft(items, "")
        findNavController().navigate(R.id.action_picker_to_caption)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
