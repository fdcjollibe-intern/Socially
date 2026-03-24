package com.apollo.socially.ui.profile

import android.os.Bundle
import android.util.Log
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
import com.apollo.socially.databinding.FragmentUserProfileBinding
import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.util.Date

class ProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var postsAdapter: ProfileGridAdapter
    
    // Store current posts for passing to feed
    private var currentPosts: List<Post> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPostsGrid()
        observeViewModel()
        setupSwipeRefresh()

        binding.userProfileBtnMenu.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.userProfileBtnEdit.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }
        binding.userProfileBtnShare.setOnClickListener { /* TODO */ }
    }

    // Reload when returning from EditProfileFragment
    override fun onResume() {
        super.onResume()
        viewModel.loadUser()
    }

    private fun setupSwipeRefresh() {
        binding.userProfileSwipeRefresh.setColorSchemeColors(
            requireContext().getColor(R.color.accent)
        )
        binding.userProfileSwipeRefresh.setOnRefreshListener {
            // Force refresh bypasses cache
            viewModel.loadUser(forceRefresh = true)
        }
    }


    private fun startSkeletonShimmer() {
        val skeletonRoot = binding.userProfileSkeletonLoading.root
        startShimmerOnView(skeletonRoot)
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                Log.d("ProfileFragment", "State: $state")
                when (state) {
                    is ProfileViewModel.UiState.Loading -> {
                        // Show skeleton loading
                        binding.userProfileSkeletonLoading.root.visibility = View.VISIBLE
                        binding.userProfileSwipeRefresh.visibility = View.GONE
                        startSkeletonShimmer()
                    }
                    is ProfileViewModel.UiState.Success -> {
                        // Hide skeleton, show content
                        binding.userProfileSkeletonLoading.root.visibility = View.GONE
                        binding.userProfileSwipeRefresh.visibility = View.VISIBLE
                        binding.userProfileSwipeRefresh.isRefreshing = false
                        
                        bindUser(state.user)
                        displayPosts(state.posts, state.hasMore)
                    }
                    is ProfileViewModel.UiState.Error -> {
                        // Hide skeleton, show content (with error state)
                        binding.userProfileSkeletonLoading.root.visibility = View.GONE
                        binding.userProfileSwipeRefresh.visibility = View.VISIBLE
                        binding.userProfileSwipeRefresh.isRefreshing = false
                        
                        Log.e("ProfileFragment", "Error: ${state.message}")
                        // Show empty state or error message
                        binding.userProfilePostsGrid.visibility = View.GONE
                        binding.userProfileEmptyState.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun displayPosts(posts: List<Post>, hasMore: Boolean) {
        currentPosts = posts // Store for navigation
        
        if (posts.isEmpty()) {
            binding.userProfilePostsGrid.visibility = View.GONE
            binding.userProfileEmptyState.visibility = View.VISIBLE
        } else {
            binding.userProfilePostsGrid.visibility = View.VISIBLE
            binding.userProfileEmptyState.visibility = View.GONE

            val currentUser = (viewModel.uiState.value as? ProfileViewModel.UiState.Success)?.user

            // Fetch liked states and convert to PostModels
            viewLifecycleOwner.lifecycleScope.launch {
                val postModels = posts.map { post ->
                    val isPlaceholder = post.mediaUrls.firstOrNull() == "placeholder"
                    val isLiked = if (!isPlaceholder) {
                        viewModel.isPostLiked(post.id)
                    } else {
                        false
                    }
                    
                    PostModel(
                        id = post.id,
                        userId = post.userId,
                        username = currentUser?.username ?: "",
                        userHandle = "@${currentUser?.username ?: ""}",
                        displayName = currentUser?.displayName ?: "",
                        userAvatarUrl = currentUser?.profileImageUrl,
                        userAvatarRes = R.drawable.user_profile_placeholder_avatar,
                        imageUrl = if (isPlaceholder) null else post.mediaUrls.firstOrNull(),
                        imageUrlList = if (isPlaceholder) emptyList() else post.mediaUrls,
                        isPlaceholder = isPlaceholder,
                        caption = post.caption,
                        likeCount = post.likesCount,
                        isLiked = isLiked,
                        timeAgo = formatTimeAgo(post.createdAt)
                    )
                }
                postsAdapter.submitList(postModels)
            }
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

    private fun bindUser(user: User) {
        binding.userProfileDisplayName.text = user.displayName.ifBlank { user.username }
        binding.userProfileUsername.text = "@${user.username}"

        binding.userProfileVerifiedBadge.visibility =
            if (user.isVerified) View.VISIBLE else View.GONE

        if (user.bio.isNotBlank()) {
            binding.userProfileBio.visibility = View.VISIBLE
            binding.userProfileBio.text = user.bio
        } else {
            binding.userProfileBio.visibility = View.GONE
        }

        binding.userProfilePostCount.text      = formatCount(user.postsCount)
        binding.userProfileFollowerCount.text  = formatCount(user.followersCount)
        binding.userProfileFollowingCount.text = formatCount(user.followingCount)

        if (!user.profileImageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.user_profile_placeholder_avatar)
                .error(R.drawable.user_profile_placeholder_avatar)
                .into(binding.userProfileAvatar)
        }

        if (!user.profileCoverUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(user.profileCoverUrl)
                .centerCrop()
                .placeholder(R.color.main)
                .into(binding.userProfileCoverPhoto)
        }

        binding.userProfileFollowersBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_profile_to_followersFollowing,
                bundleOf("startTab" to 0, "username" to user.username)
            )
        }
        binding.userProfileFollowingBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_profile_to_followersFollowing,
                bundleOf("startTab" to 1, "username" to user.username)
            )
        }
    }

    private fun setupPostsGrid() {

        postsAdapter = ProfileGridAdapter { _, index ->
            // Pass the actual posts as parcelable array
            viewLifecycleOwner.lifecycleScope.launch {
                val bundle = Bundle().apply {
                    putInt("startIndex", index)
                    // Convert posts to PostModel array for passing
                    val currentUser = (viewModel.uiState.value as? ProfileViewModel.UiState.Success)?.user
                    val postModels = currentPosts.map { post ->
                        val isLiked = viewModel.isPostLiked(post.id)
                        PostModel(
                            id = post.id,
                            userId = post.userId,
                            username = currentUser?.username ?: "",
                            userHandle = "@${currentUser?.username ?: ""}",
                            displayName = currentUser?.displayName ?: "",
                            userAvatarUrl = currentUser?.profileImageUrl,
                            userAvatarRes = R.drawable.user_profile_placeholder_avatar,
                            imageUrl = post.mediaUrls.firstOrNull(),
                            imageUrlList = post.mediaUrls,
                            caption = post.caption,
                            likeCount = post.likesCount,
                            isLiked = isLiked,
                            timeAgo = formatTimeAgo(post.createdAt)
                        )
                    }
                    putParcelableArrayList("posts", ArrayList(postModels))
                }
                
                findNavController().navigate(
                    R.id.action_profile_to_profilePostFeed,
                    bundle
                )
            }
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        
        binding.userProfilePostsGrid.apply {
            layoutManager = gridLayoutManager
            adapter = postsAdapter
            if (itemDecorationCount == 0) {
                addItemDecoration(GridSpacingDecoration(spanCount = 3, spacingPx = 2))
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
                    if (currentState is ProfileViewModel.UiState.Success 
                        && !currentState.isLoadingMore 
                        && currentState.hasMore
                        && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 6
                        && firstVisibleItemPosition >= 0) {
                        
                        Log.d("ProfileFragment", "Loading more posts...")
                        viewModel.loadMorePosts()
                    }
                }
            })
        }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000     -> String.format("%.1fk", count / 1_000f)
        else               -> count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}