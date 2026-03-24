package com.apollo.socially.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.BottomSheetCommentsBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val commentAdapter = CommentAdapter()
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        fun newInstance(postId: String, likeCount: Int, isLiked: Boolean): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("postId", postId)
                    putInt("likeCount", likeCount)
                    putBoolean("isLiked", isLiked)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val postId = arguments?.getString("postId") ?: return
        val likeCount = arguments?.getInt("likeCount") ?: 0
        val isLiked = arguments?.getBoolean("isLiked") ?: false

        // Create a minimal PostModel just to init the ViewModel
        val post = com.apollo.socially.model.PostModel(
            id = postId,
            userId = "",
            username = "",
            userHandle = "",
            likeCount = likeCount,
            isLiked = isLiked
        )
        viewModel.init(post)

        loadCurrentUserAvatar()
        setupCommentsList()
        setupInput()
        observeViewModel()
    }
    
    private fun loadCurrentUserAvatar() {
        val uid = auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userDoc = firestore.collection("users").document(uid).get().await()
                val avatarUrl = userDoc.getString("profileImageUrl")
                if (!avatarUrl.isNullOrBlank()) {
                    Glide.with(this@CommentsBottomSheet)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.user_profile_placeholder_avatar)
                        .into(binding.commentsInputAvatar)
                }
            } catch (e: Exception) {
                // Keep placeholder
            }
        }
    }

    private fun setupCommentsList() {
        val highlightCommentId = arguments?.getString("highlightCommentId")
        
        binding.commentsRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

        binding.commentsLoadMore.setOnClickListener {
            viewModel.loadMoreComments()
        }
        
        // Scroll to and highlight comment if specified
        if (highlightCommentId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                // Wait for comments to load
                viewModel.commentsState.collect { state ->
                    if (state is PostDetailViewModel.CommentsState.Success) {
                        val index = state.comments.indexOfFirst { it.id == highlightCommentId }
                        if (index != -1) {
                            binding.commentsRv.post {
                                binding.commentsRv.scrollToPosition(index)
                                // Highlight with fade animation
                                highlightCommentAtPosition(index)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun highlightCommentAtPosition(position: Int) {
        binding.commentsRv.post {
            val viewHolder = binding.commentsRv.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                view.setBackgroundColor(requireContext().getColor(R.color.accent))
                view.alpha = 0.3f
                view.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .withEndAction {
                        view.animate()
                            .alpha(0f)
                            .setDuration(2000)
                            .withEndAction {
                                view.setBackgroundColor(requireContext().getColor(android.R.color.transparent))
                                view.alpha = 1f
                            }
                            .start()
                    }
                    .start()
            }
        }
    }

    private fun setupInput() {
        binding.commentsSendBtn.setOnClickListener {
            val text = binding.commentsInput.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isEmpty()) return@setOnClickListener
            viewModel.sendComment(text)
            binding.commentsInput.text?.clear()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.commentsState.collect { state ->
                if (_binding == null) return@collect
                when (state) {
                    is PostDetailViewModel.CommentsState.Loading -> {
                        binding.commentsLoadMore.visibility = View.GONE
                    }
                    is PostDetailViewModel.CommentsState.Success -> {
                        commentAdapter.submitList(state.comments.toList())
                        binding.commentsLoadMore.visibility =
                            if (state.hasMore && !state.isLoadingMore) View.VISIBLE
                            else View.GONE
                    }
                    is PostDetailViewModel.CommentsState.Error -> {
                        binding.commentsLoadMore.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}