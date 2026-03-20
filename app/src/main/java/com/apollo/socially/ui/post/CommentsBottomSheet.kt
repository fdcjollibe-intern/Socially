package com.apollo.socially.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.databinding.BottomSheetCommentsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val commentAdapter = CommentAdapter()

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

        setupCommentsList()
        setupInput()
        observeViewModel()
    }

    private fun setupCommentsList() {
        binding.commentsRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }

        binding.commentsLoadMore.setOnClickListener {
            viewModel.loadMoreComments()
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