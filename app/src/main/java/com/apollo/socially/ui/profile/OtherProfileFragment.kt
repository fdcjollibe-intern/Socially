package com.apollo.socially.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentOtherProfileBinding
import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import com.apollo.socially.data.cache.OtherProfileCache
import java.util.Date

class OtherProfileFragment : Fragment() {

    private var _binding: FragmentOtherProfileBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OtherProfileViewModel by viewModels()
    private lateinit var postsAdapter: ProfileGridAdapter
    private var isFollowing = false
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = arguments?.getString("userId") ?: ""
        
        setupGrid()
        setupSwipeRefresh()
        setupButtons()
        observeViewModel()
        
        currentUserId?.let { viewModel.loadUserProfile(it) }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is OtherProfileViewModel.UiState.Loading -> {
                        // Show skeleton loading
                        binding.otherProfileSkeletonLoading.root.visibility = View.VISIBLE
                        binding.otherProfileScrollView.visibility = View.GONE
                    }
                    is OtherProfileViewModel.UiState.Success -> {
                        // Hide skeleton, show content
                        binding.otherProfileSkeletonLoading.root.visibility = View.GONE
                        binding.otherProfileScrollView.visibility = View.VISIBLE
                        binding.otherProfileSwipeRefresh.isRefreshing = false
                        
                        bindUser(state.user)
                        displayPosts(state.posts, state.hasMore)
                        isFollowing = state.isFollowing
                        updateFollowButton()
                    }
                    is OtherProfileViewModel.UiState.Error -> {
                        // Hide skeleton, show content with error
                        binding.otherProfileSkeletonLoading.root.visibility = View.GONE
                        binding.otherProfileScrollView.visibility = View.VISIBLE
                        binding.otherProfileSwipeRefresh.isRefreshing = false
                        
                        // Show error state
                        binding.otherProfilePostsGrid.visibility = View.GONE
                        binding.otherProfileEmptyState.visibility = View.VISIBLE
                    }
                }
            }
        }
    }


    private fun startSkeletonShimmer() {
        startShimmerOnView(binding.otherProfileSkeletonLoading.root)
    }

    private fun startShimmerOnView(view: View) {
        val bg = view.background
        if (bg is android.graphics.drawable.AnimationDrawable) {
            bg.start()
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                startShimmerOnView(view.getChildAt(i))
            }
        }
    }


    private fun bindUser(user: User) {
        binding.otherProfileDisplayName.text = user.displayName.ifBlank { user.username }
        binding.otherProfileUsername.text = "@${user.username}"
        
        binding.otherProfileVerifiedBadge.visibility =
            if (user.isVerified) View.VISIBLE else View.GONE

        if (user.bio.isNotBlank()) {
            binding.otherProfileBio.visibility = View.VISIBLE
            binding.otherProfileBio.text = user.bio
        } else {
            binding.otherProfileBio.visibility = View.GONE
        }

        binding.otherProfilePostCount.text = formatCount(user.postsCount)
        binding.otherProfileFollowerCount.text = formatCount(user.followersCount)
        binding.otherProfileFollowingCount.text = formatCount(user.followingCount)

        if (!user.profileImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.user_profile_placeholder_avatar)
                .error(R.drawable.user_profile_placeholder_avatar)
                .into(binding.otherProfileAvatar)
        }

        if (!user.profileCoverUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(user.profileCoverUrl)
                .centerCrop()
                .placeholder(R.color.main)
                .into(binding.otherProfileCoverPhoto)
        }
    }

    private fun displayPosts(posts: List<Post>, hasMore: Boolean) {
        if (posts.isEmpty()) {
            binding.otherProfilePostsGrid.visibility = View.GONE
            binding.otherProfileEmptyState.visibility = View.VISIBLE
        } else {
            binding.otherProfilePostsGrid.visibility = View.VISIBLE
            binding.otherProfileEmptyState.visibility = View.GONE
            
            // Convert Post to PostModel with actual Firebase thumbnails
            val postModels = posts.map { post ->
                PostModel(
                    id = post.id,
                    userId = post.userId,
                    username = "",
                    userHandle = "",
                    userAvatarRes = R.drawable.user_profile_placeholder_avatar,
                    imageUrl = post.mediaUrls.firstOrNull(),
                    imageUrlList = post.mediaUrls,
                    caption = post.caption,
                    likeCount = post.likesCount,
                    timeAgo = formatTimeAgo(post.createdAt)
                )
            }
            postsAdapter.submitList(postModels)
        }
    }

    private fun formatTimeAgo(date: Date?): String {
        if (date == null) return "just now"
        val now = System.currentTimeMillis()
        val diff = now - date.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> "${days / 7}w ago"
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "just now"
        }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }

    private fun setupButtons() {
        binding.otherProfileBtnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.otherProfileBtnFollow.setOnClickListener {
            currentUserId?.let { userId ->
                val wasFollowing = isFollowing

                // Optimistic UI update
                isFollowing = !wasFollowing
                updateFollowButton()

                viewModel.toggleFollow(userId, wasFollowing) { success ->
                    if (!isAdded || _binding == null) return@toggleFollow
                    if (!success) {
                        // Revert on failure
                        isFollowing = wasFollowing
                        updateFollowButton()
                    }
                }
            }
        }

        binding.otherProfileBtnMessage.setOnClickListener {
            // TODO: open messages
        }

        binding.otherProfileFollowersBtn.setOnClickListener {
            currentUserId?.let { userId ->
                findNavController().navigate(
                    R.id.action_otherProfile_to_followersFollowing,
                    bundleOf(
                        "startTab" to 0,
                        "userId" to userId,
                        "username" to binding.otherProfileUsername.text.toString()
                    )
                )
            }
        }

        binding.otherProfileFollowingBtn.setOnClickListener {
            currentUserId?.let { userId ->
                findNavController().navigate(
                    R.id.action_otherProfile_to_followersFollowing,
                    bundleOf(
                        "startTab" to 1,
                        "userId" to userId,
                        "username" to binding.otherProfileUsername.text.toString()
                    )
                )
            }
        }
    }

    private fun updateFollowButton() {
        if (isFollowing) {
            binding.otherProfileBtnFollow.text = "Following"
            binding.otherProfileBtnFollow.setTextColor(requireContext().getColor(R.color.gray))
            binding.otherProfileBtnFollow.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(android.R.color.transparent)
                )
            binding.otherProfileBtnFollow.strokeWidth = 2
            binding.otherProfileBtnFollow.strokeColor =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.gray_border)
                )
        } else {
            binding.otherProfileBtnFollow.text = "Follow"
            binding.otherProfileBtnFollow.setTextColor(requireContext().getColor(R.color.white))
            binding.otherProfileBtnFollow.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.accent)
                )
            binding.otherProfileBtnFollow.strokeWidth = 0
        }
    }

    private fun setupGrid() {
        postsAdapter = ProfileGridAdapter { _, index ->
            findNavController().navigate(
                R.id.action_otherProfile_to_profilePostFeed,
                bundleOf("startIndex" to index)
            )
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        
        binding.otherProfilePostsGrid.apply {
            layoutManager = gridLayoutManager
            adapter = postsAdapter
            if (itemDecorationCount == 0) {
                addItemDecoration(GridSpacingDecoration(3, 2))
            }
            
            // Add scroll listener for pagination
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = gridLayoutManager.childCount
                    val totalItemCount = gridLayoutManager.itemCount
                    val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()
                    
                    val currentState = viewModel.uiState.value
                    
                    // Load more when scrolled to near bottom
                    if (currentState is OtherProfileViewModel.UiState.Success 
                        && !currentState.isLoadingMore 
                        && currentState.hasMore
                        && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 6
                        && firstVisibleItemPosition >= 0) {
                        
                        viewModel.loadMorePosts()
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupSwipeRefresh() {
        binding.otherProfileSwipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.accent)
        )
        binding.otherProfileSwipeRefresh.setOnRefreshListener {
            currentUserId?.let { userId ->
                OtherProfileCache.invalidate(userId)
                viewModel.loadUserProfile(userId)
            }
        }
    }

}
