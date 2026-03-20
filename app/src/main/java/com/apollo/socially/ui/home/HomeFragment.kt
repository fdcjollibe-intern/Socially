package com.apollo.socially.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.data.upload.PostUploadService
import com.apollo.socially.data.upload.UploadStateHolder
import com.apollo.socially.databinding.FragmentHomeBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.model.StoryModel
import com.apollo.socially.ui.post.PostCardAdapter
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppBarFade()
        setupStories()
        setupFeed()
        observeUploadState()

        binding.homeBtnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifications)
        }

        binding.homeRetryBtn.setOnClickListener {
            UploadStateHolder.getLastUploadIntent()?.let { intent ->
                UploadStateHolder.reset()
                requireContext().startForegroundService(intent)
            }
        }

        binding.homeRetryDismiss.setOnClickListener {
            // Stop any ongoing upload service
            val serviceIntent = android.content.Intent(requireContext(), PostUploadService::class.java)
            requireContext().stopService(serviceIntent)
            
            UploadStateHolder.reset()
            UploadStateHolder.clearUploadIntent()
            hideRetryBanner()
        }

        binding.homeSuccessDismiss.setOnClickListener {
            hideSuccessBanner()
        }
    }

    private fun observeUploadState() {
        viewLifecycleOwner.lifecycleScope.launch {
            UploadStateHolder.state.collect { state ->
                when (state) {
                    is UploadStateHolder.UploadState.Idle -> {
                        hideUploadBanner()
                        hideRetryBanner()
                        hideSuccessBanner()
                    }
                    is UploadStateHolder.UploadState.Uploading -> {
                        hideRetryBanner()
                        hideSuccessBanner()
                        showUploadBanner(state.percent, state.current, state.total)
                    }
                    is UploadStateHolder.UploadState.Success -> {
                        hideUploadBanner()
                        hideRetryBanner()
                        showSuccessBanner()
                        // Auto-hide success banner after 3 seconds
                        viewLifecycleOwner.lifecycleScope.launch {
                            kotlinx.coroutines.delay(3000)
                            if (UploadStateHolder.state.value is UploadStateHolder.UploadState.Success) {
                                UploadStateHolder.reset()
                            }
                        }
                    }
                    is UploadStateHolder.UploadState.Error -> {
                        hideUploadBanner()
                        hideSuccessBanner()
                        showRetryBanner()
                    }
                }
            }
        }
    }

    private fun showUploadBanner(percent: Int, current: Int, total: Int) {
        binding.homeUploadBanner.visibility = View.VISIBLE
        binding.homeUploadProgress.progress = percent
        binding.homeUploadPercent.text = "$percent%"
        binding.homeUploadLabel.text = if (total > 1)
            "Uploading photo $current of $total…"
        else
            "Uploading post…"
    }

    private fun hideUploadBanner() {
        binding.homeUploadBanner.visibility = View.GONE
        binding.homeUploadProgress.progress = 0
    }

    private fun showSuccessBanner() {
        binding.homeSuccessBanner.visibility = View.VISIBLE
    }

    private fun hideSuccessBanner() {
        binding.homeSuccessBanner.visibility = View.GONE
    }

    private fun showRetryBanner() {
        binding.homeRetryBanner.visibility = View.VISIBLE
    }

    private fun hideRetryBanner() {
        binding.homeRetryBanner.visibility = View.GONE
    }

    private fun setupAppBarFade() {
        binding.homeAppbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
                val totalScrollRange = appBar.totalScrollRange
                if (totalScrollRange == 0) return@OnOffsetChangedListener
                val ratio = abs(verticalOffset).toFloat() / totalScrollRange.toFloat()
                binding.homeAppName.alpha = 1f - ratio
                binding.homeBtnNotifications.alpha = 1f - ratio
            }
        )
    }

    private fun setupStories() {
        val storyAdapter = StoryAdapter(
            onAddStoryClick = {
                findNavController().navigate(R.id.action_home_to_storyViewer)
            },
            onStoryClick = {
                findNavController().navigate(R.id.action_home_to_storyViewer)
            }
        )
        binding.homeStoriesRv.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }
        storyAdapter.submitList(listOf(
            StoryModel("my", "Your Story", R.drawable.user_profile_placeholder_avatar, isMyStory = true),
            StoryModel("s1", "vibeteller", R.drawable.user_profile_placeholder_avatar, hasUnseenStory = true),
            StoryModel("s2", "mooddream", R.drawable.user_profile_placeholder_avatar, hasUnseenStory = true),
            StoryModel("s3", "sunsetvibes", R.drawable.user_profile_placeholder_avatar, hasUnseenStory = false),
            StoryModel("s4", "calmwaves", R.drawable.user_profile_placeholder_avatar, hasUnseenStory = true),
        ))
    }

    private fun setupFeed() {
        val postAdapter = PostCardAdapter(
            onSeeMoreClick = { findNavController().navigate(R.id.action_home_to_postDetail) },
            onLikeClick = {}
        )
        binding.homeFeedRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
            isNestedScrollingEnabled = false
        }
        postAdapter.submitList(listOf(
            PostModel(
                id = "p1", userId = "u1", username = "vibeteller", userHandle = "@vibeteller",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar, isVerified = true,
                imageRes = R.drawable.sample_photo,
                musicLabel = "Imam Majboor, Neha Nair, Kinavu Kondu",
                likeCount = 107_000,
                caption = "soft hues, slow days, and a heart full of stillness 🌿✨",
                timeAgo = "2 minutes ago"
            ),
            PostModel(
                id = "p2", userId = "u2", username = "mooddreamlms", userHandle = "@mooddreamlms",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar, isVerified = false,
                imageResList = listOf(R.drawable.sample_photo, R.drawable.sample_photo_2),
                likeCount = 45_200,
                caption = "Golden hour hits different when you're in the right headspace ☀️🎞️",
                timeAgo = "1 hour ago"
            ),
            PostModel(
                id = "p3", userId = "u3", username = "sunsetvibes", userHandle = "@sunsetvibes",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar, isVerified = false,
                imageRes = R.drawable.sample_photo_2,
                likeCount = 12_800,
                caption = "Chasing light and loving every second of it 🌅",
                timeAgo = "3 hours ago"
            ),
        ))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

