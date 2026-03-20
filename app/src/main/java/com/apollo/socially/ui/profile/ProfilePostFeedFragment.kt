package com.apollo.socially.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentProfilePostFeedBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.ui.post.PostCardAdapter

class ProfilePostFeedFragment : Fragment() {

    private var _binding: FragmentProfilePostFeedBinding? = null
    private val binding get() = _binding!!

    // Passed from ProfileFragment — which post index was tapped
    private var startIndex: Int = 0
    private var posts: List<PostModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startIndex = arguments?.getInt("startIndex", 0) ?: 0
        @Suppress("DEPRECATION")
        posts = arguments?.getParcelableArrayList<PostModel>("posts") ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePostFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.profileFeedBtnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupFeed()
    }

    private fun setupFeed() {
        val adapter = PostCardAdapter(
            onSeeMoreClick = { post ->
                // Navigate to post detail (comments, full caption)
                findNavController().navigate(R.id.action_profileFeed_to_postDetail)
            },
            onLikeClick = { /* TODO */ }
        )

        binding.profileFeedRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        // Use actual posts passed from ProfileFragment
        if (posts.isNotEmpty()) {
            adapter.submitList(posts)
            
            // Scroll to the tapped post immediately (post at startIndex position)
            binding.profileFeedRv.post {
                binding.profileFeedRv.scrollToPosition(startIndex)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
