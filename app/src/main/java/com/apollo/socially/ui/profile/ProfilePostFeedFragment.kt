package com.apollo.socially.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.databinding.FragmentProfilePostFeedBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.ui.post.CommentsBottomSheet
import com.apollo.socially.ui.post.PostCardAdapter
import com.apollo.socially.ui.post.VideoFocusManager

class ProfilePostFeedFragment : Fragment() {

    private var _binding: FragmentProfilePostFeedBinding? = null
    private val binding get() = _binding!!

    private var startIndex: Int = 0
    private var posts: MutableList<PostModel> = mutableListOf()
    private lateinit var adapter: PostCardAdapter
    
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startIndex = arguments?.getInt("startIndex", 0) ?: 0
        @Suppress("DEPRECATION")
        posts = (arguments?.getParcelableArrayList<PostModel>("posts") ?: emptyList()).toMutableList()
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
        adapter = PostCardAdapter(
            onLikeClick = { post ->
                viewModel.toggleLike(post.id, post.isLiked) { newLikedState, likeCountDelta ->
                    // Update the post in the list
                    val index = posts.indexOfFirst { it.id == post.id }
                    if (index != -1) {
                        posts[index] = posts[index].copy(
                            isLiked = newLikedState,
                            likeCount = posts[index].likeCount + likeCountDelta
                        )
                        adapter.submitList(posts.toList())
                    }
                }
            },
            onCommentClick = { post ->
                CommentsBottomSheet
                    .newInstance(post.id, post.likeCount, post.isLiked)
                    .show(childFragmentManager, "comments")
            }
        )

        binding.profileFeedRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@ProfilePostFeedFragment.adapter
        }

        // Video focus for profile feed too
        val focusManager = VideoFocusManager(
            recyclerView = binding.profileFeedRv,
            getAdapter = { adapter }
        )
        focusManager.attach()
        viewLifecycleOwner.lifecycle.addObserver(focusManager)

        if (posts.isNotEmpty()) {
            adapter.submitList(posts)
            binding.profileFeedRv.post {
                binding.profileFeedRv.scrollToPosition(startIndex)
                focusManager.updateFocus()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}