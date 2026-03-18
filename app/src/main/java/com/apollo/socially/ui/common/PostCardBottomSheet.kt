package com.apollo.socially.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentPostCardBinding
import com.apollo.socially.model.PostModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PostCardBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentPostCardBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "PostCardBottomSheet"
        private const val ARG_POST_ID = "arg_post_id"
        private val postCache = mutableMapOf<String, PostModel>()

        fun show(fragmentManager: androidx.fragment.app.FragmentManager, post: PostModel) {
            postCache[post.id] = post
            val sheet = PostCardBottomSheet().apply {
                arguments = Bundle().apply { putString(ARG_POST_ID, post.id) }
            }
            sheet.show(fragmentManager, TAG)
        }
    }

    override fun getTheme() = R.style.PostCardBottomSheetTheme

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val postId = arguments?.getString(ARG_POST_ID) ?: return
        val post = postCache[postId] ?: return
        bindPost(post)
    }

    private fun bindPost(post: PostModel) {
        binding.postCardUsername.text = post.username
        binding.postCardTimeAgo.text = post.timeAgo
        binding.postCardCaption.text = post.caption
        binding.postCardLikeCount.text = formatCount(post.likeCount) + " Liked"
        binding.postCardVerified.visibility = if (post.isVerified) View.VISIBLE else View.GONE
        if (!post.musicLabel.isNullOrBlank()) {
            binding.postCardMusicRow.visibility = View.VISIBLE
            binding.postCardMusicLabel.text = post.musicLabel
        }
        post.imageRes?.let { binding.postCardImage.setImageResource(it) }
        post.userAvatarRes?.let { binding.postCardAvatar.setImageResource(it) }
        binding.postCardBtnLike.setOnClickListener { /* TODO: toggle like */ }
        binding.postCardOptions.setOnClickListener { /* TODO: options menu */ }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
