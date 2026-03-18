package com.apollo.socially.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentPostDetailBinding
import com.apollo.socially.model.CommentModel

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val commentAdapter = CommentAdapter()
    private var visibleCommentCount = 8
    private val allComments = mutableListOf<CommentModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.postDetailBtnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupComments()
        setupLoadMore()
        setupCommentInput()
    }

    private fun setupComments() {
        binding.postDetailCommentsRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
            isNestedScrollingEnabled = false
        }

        allComments.addAll(listOf(
            CommentModel("c0", "me", "you", R.drawable.user_profile_placeholder_avatar,
                "This is my comment! Love this post 🔥", "just now", 0, false, true),
            CommentModel("c1", "u1", "vibeteller", R.drawable.user_profile_placeholder_avatar,
                "This is such a beautiful shot! 🌸", "2h ago", 24),
            CommentModel("c2", "u2", "mooddreamlms", R.drawable.user_profile_placeholder_avatar,
                "Absolutely stunning 😍", "3h ago", 15),
            CommentModel("c3", "u3", "sunsetvibes", R.drawable.user_profile_placeholder_avatar,
                "The lighting is everything ✨", "4h ago", 8),
            CommentModel("c4", "u4", "calmwaves", R.drawable.user_profile_placeholder_avatar,
                "Slow days are the best days 🌿", "5h ago", 32),
            CommentModel("c5", "u5", "driftingclouds", R.drawable.user_profile_placeholder_avatar,
                "I felt this in my soul 💙", "6h ago", 19),
            CommentModel("c6", "u6", "quietmoments", R.drawable.user_profile_placeholder_avatar,
                "Perfect caption for this 🤍", "7h ago", 11),
            CommentModel("c7", "u7", "goldenhour_", R.drawable.user_profile_placeholder_avatar,
                "Your feed is a whole mood 🎞️", "8h ago", 45),
            CommentModel("c8", "u8", "innerpeace99", R.drawable.user_profile_placeholder_avatar,
                "This made my day better 🌻", "9h ago", 7),
            CommentModel("c9", "u9", "softaesthetic", R.drawable.user_profile_placeholder_avatar,
                "Saving this forever 🔖", "10h ago", 28),
            CommentModel("c10", "u10", "dreamy.lens", R.drawable.user_profile_placeholder_avatar,
                "Art. Pure art. 🎨", "11h ago", 56),
            CommentModel("c11", "u11", "wanderlust.k", R.drawable.user_profile_placeholder_avatar,
                "Where was this taken?? 😮", "12h ago", 13),
        ))

        submitVisibleComments()
    }

    private fun submitVisibleComments() {
        commentAdapter.submitList(allComments.take(visibleCommentCount).toList())
        binding.postDetailLoadMore.visibility =
            if (visibleCommentCount < allComments.size) View.VISIBLE else View.GONE
    }

    private fun setupLoadMore() {
        binding.postDetailLoadMore.setOnClickListener {
            visibleCommentCount = minOf(visibleCommentCount + 8, allComments.size)
            submitVisibleComments()
        }
    }

    private fun setupCommentInput() {
        binding.postDetailBtnSendComment.setOnClickListener {
            val text = binding.postDetailCommentInput.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isEmpty()) return@setOnClickListener

            val newComment = CommentModel(
                id = "new_${System.currentTimeMillis()}",
                userId = "me",
                username = "you",
                avatarRes = R.drawable.user_profile_placeholder_avatar,
                text = text,
                timeAgo = "just now",
                likeCount = 0,
                isOwnComment = true
            )
            allComments.add(0, newComment)
            if (visibleCommentCount < allComments.size) visibleCommentCount++
            submitVisibleComments()
            binding.postDetailCommentInput.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
