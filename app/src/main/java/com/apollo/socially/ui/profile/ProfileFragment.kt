package com.apollo.socially.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentUserProfileBinding
import com.apollo.socially.model.PostModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPostsGrid()

        binding.userProfileBtnMenu.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }
        binding.userProfileBtnEdit.setOnClickListener { /* TODO */ }
        binding.userProfileBtnShare.setOnClickListener { /* TODO */ }
    }

    private fun setupPostsGrid() {
        val adapter = ProfileGridAdapter(parentFragmentManager)

        binding.userProfilePostsGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            this.adapter = adapter
            if (itemDecorationCount == 0) {
                addItemDecoration(GridSpacingDecoration(spanCount = 3, spacingPx = 2))
            }
        }

        val samplePosts = (1..12).map { index ->
            PostModel(
                id = "post_$index",
                userId = "user_1",
                username = "Jollibe Got The Sauce",
                userHandle = "@jshawttyyy_",
                userAvatarRes = R.drawable.user_profile_placeholder_avatar,
                isVerified = index % 3 == 0,
                imageRes = R.drawable.sample_photo,
                musicLabel = if (index % 2 == 0) "Imam Majboor, Neha Nair" else null,
                likeCount = 1000 * index,
                caption = "soft hues, slow days, and a heart full of stillness 🌿✨",
                timeAgo = "${index * 2}h ago"
            )
        }
        adapter.submitList(samplePosts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}