package com.apollo.socially.ui.create

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.data.remote.cloudinary.CloudinaryUploader
import com.apollo.socially.data.upload.PostUploadService
import com.apollo.socially.data.upload.UploadStateHolder
import com.apollo.socially.databinding.FragmentCreatePostCaptionBinding
import com.apollo.socially.model.MediaType
import com.apollo.socially.utils.HashMentionTextWatcher
import com.apollo.socially.utils.PostDraftStore

class CreatePostCaptionFragment : Fragment() {

    private var _binding: FragmentCreatePostCaptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostCaptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val selectedMedia = PostDraftStore.getDraft()?.mediaItems ?: emptyList()

        // Media preview
        if (selectedMedia.isNotEmpty()) {
            binding.captionMediaPreviewRv.apply {
                layoutManager = LinearLayoutManager(
                    requireContext(), LinearLayoutManager.HORIZONTAL, false
                )
                adapter = CaptionMediaPreviewAdapter(selectedMedia)
            }
        }

        // Caption input
        binding.captionInput.addTextChangedListener(HashMentionTextWatcher())
        binding.captionInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                binding.captionCharCount.text = "$count / 2200"
                binding.captionCharCount.setTextColor(
                    if (count > 2200) requireContext().getColor(R.color.red)
                    else requireContext().getColor(R.color.gray)
                )
            }
        })

        binding.captionBtnBack.setOnClickListener { findNavController().popBackStack() }

        binding.captionBtnPost.setOnClickListener {
            if (selectedMedia.isEmpty()) return@setOnClickListener

            val caption = binding.captionInput.text?.toString()?.trim() ?: ""
            
            // Validate file sizes before starting upload
            var hasInvalidSize = false
            var totalSize = 0L
            
            for (media in selectedMedia) {
                val fileSize = CloudinaryUploader.getFileSize(requireContext(), media.uri)
                totalSize += fileSize
                
                if (fileSize > CloudinaryUploader.MAX_FILE_SIZE) {
                    Toast.makeText(
                        requireContext(),
                        "File too large: ${CloudinaryUploader.formatFileSize(fileSize)}. Max 100MB per file.",
                        Toast.LENGTH_LONG
                    ).show()
                    hasInvalidSize = true
                    break
                }
                
                if (fileSize <= 0) {
                    Toast.makeText(
                        requireContext(),
                        "Cannot read file size. Please try a different file.",
                        Toast.LENGTH_LONG
                    ).show()
                    hasInvalidSize = true
                    break
                }
            }
            
            if (hasInvalidSize) return@setOnClickListener
            
            // Show warning for large uploads
            if (totalSize > 20 * 1024 * 1024) { // 20MB+
                Toast.makeText(
                    requireContext(),
                    "Uploading ${CloudinaryUploader.formatFileSize(totalSize)}. This may take a while.",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Reset previous state
            UploadStateHolder.reset()

            val uris  = selectedMedia.map { it.uri }
            val types = selectedMedia.map { if (it.type == MediaType.VIDEO) "video" else "image" }

            val uploadIntent = PostUploadService.buildIntent(requireContext(), uris, types, caption)
            
            // Store the intent for retry functionality
            UploadStateHolder.setLastUploadIntent(uploadIntent)
            
            // Start the foreground service
            requireContext().startForegroundService(uploadIntent)

            PostDraftStore.clearDraft()

            // Navigate home immediately — upload runs in background
            findNavController().navigate(
                R.id.navigation_home, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph_main, false)
                    .build()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
