package com.apollo.socially.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentHomeBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.model.StoryModel
import com.apollo.socially.ui.post.PostCardAdapter

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
        setupStories()
        setupFeed()
        binding.homeBtnNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notifications)
        }
    }

    private fun setupStories() {
        val storyAdapter = StoryAdapter(
            onAddStoryClick = {
                // Opens story viewer for "my story"
                findNavController().navigate(R.id.action_home_to_storyViewer)
            },
            onStoryClick = {
                // Opens story viewer for other users (pass story id when real data)
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
                likeCount = 107_000, caption = "soft hues, slow days, and a heart full of stillness 🌿✨",
                timeAgo = "2 minutes ago"
            ),
            PostModel(
                id = "p2", userId = "u2", username = "mooddreamlms", userHandle = "@mooddreamlms",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar, isVerified = false,
                imageResList = listOf(R.drawable.sample_photo, R.drawable.sample_photo_2),
                likeCount = 45_200, caption = "Golden hour hits different ☀️🎞️",
                timeAgo = "1 hour ago"
            ),
            PostModel(
                id = "p3", userId = "u3", username = "sunsetvibes", userHandle = "@sunsetvibes",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar, isVerified = false,
                imageRes = R.drawable.sample_photo_2,
                likeCount = 12_800, caption = "Chasing light and loving every second 🌅",
                timeAgo = "3 hours ago"
            ),
        ))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}