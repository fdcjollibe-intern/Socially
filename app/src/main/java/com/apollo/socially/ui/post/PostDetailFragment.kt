package com.apollo.socially.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.databinding.FragmentPostDetailBinding
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private val commentAdapter = CommentAdapter()
    private var post: PostModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        post = arguments?.getParcelable("post")
    }

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

        post?.let { viewModel.init(it) }

        setupPostImage()
        setupComments()
        setupCommentInput()
        observeViewModel()
    }

    private fun setupPostImage() {
        val p = post ?: return
        val urls = p.imageUrls

        if (urls.isEmpty()) return

        if (urls.size >= 2) {
            // Multi-image: use pager inside detail card
            binding.postDetailCard.postCardSingleContainer.visibility = View.GONE
            binding.postDetailCard.postCardPagerContainer.visibility = View.VISIBLE
            val pagerAdapter = PostImageUrlPagerAdapter(urls)
            binding.postDetailCard.postCardViewPager.adapter = pagerAdapter
        } else {
            // Single image
            binding.postDetailCard.postCardSingleContainer.visibility = View.VISIBLE
            binding.postDetailCard.postCardPagerContainer.visibility = View.GONE
            Glide.with(this)
                .load(urls[0])
                .centerCrop()
                .into(binding.postDetailCard.postCardImage)
        }

        // Hide the see more and caption from the included card
        // since PostDetailFragment shows full caption separately
        binding.postDetailCard.postCardSeeMore.visibility = View.GONE
        binding.postDetailCard.postCardCaption.visibility = View.GONE
        binding.postDetailCard.postCardTimeAgo.visibility = View.GONE
    }

    private fun setupComments() {
        binding.postDetailCommentsRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
            isNestedScrollingEnabled = false
        }

        binding.postDetailLoadMore.setOnClickListener {
            viewModel.loadMoreComments()
        }
    }

    private fun setupCommentInput() {
        binding.postDetailBtnSendComment.setOnClickListener {
            val text = binding.postDetailCommentInput.text?.toString()?.trim() ?: return@setOnClickListener
            if (text.isEmpty()) return@setOnClickListener
            viewModel.sendComment(text)
            binding.postDetailCommentInput.text?.clear()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLiked.collect { liked ->
                if (_binding == null) return@collect
                binding.postDetailCard.postCardBtnLike.setColorFilter(
                    requireContext().getColor(
                        if (liked) android.R.color.holo_red_light else android.R.color.white
                    )
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.likeCount.collect { count ->
                if (_binding == null) return@collect
                binding.postDetailCard.postCardLikeCount.text =
                    formatCount(count) + " Liked"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.commentsState.collect { state ->
                if (_binding == null) return@collect
                when (state) {
                    is PostDetailViewModel.CommentsState.Loading -> {
                        binding.postDetailLoadMore.visibility = View.GONE
                    }
                    is PostDetailViewModel.CommentsState.Success -> {
                        commentAdapter.submitList(state.comments.toList())
                        binding.postDetailLoadMore.visibility =
                            if (state.hasMore && !state.isLoadingMore) View.VISIBLE else View.GONE
                    }
                    is PostDetailViewModel.CommentsState.Error -> {
                        binding.postDetailLoadMore.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}