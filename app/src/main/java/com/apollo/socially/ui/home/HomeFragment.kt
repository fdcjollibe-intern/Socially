package com.apollo.socially.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.data.upload.PostUploadService
import com.apollo.socially.data.upload.UploadStateHolder
import com.apollo.socially.databinding.FragmentHomeBinding
import com.apollo.socially.ui.post.CommentsBottomSheet
import com.apollo.socially.ui.post.PostCardAdapter
import com.apollo.socially.ui.post.VideoFocusManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var postAdapter: PostCardAdapter
    private lateinit var videoFocusManager: VideoFocusManager

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
        setupFeed()
        observeFeed()
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
            val serviceIntent = android.content.Intent(
                requireContext(), PostUploadService::class.java
            )
            requireContext().stopService(serviceIntent)
            UploadStateHolder.reset()
            UploadStateHolder.clearUploadIntent()
            hideRetryBanner()
        }

        binding.homeSuccessDismiss.setOnClickListener {
            hideSuccessBanner()
        }
    }

    private fun setupFeed() {
        postAdapter = PostCardAdapter(
            onLikeClick = { post ->
                // wire to like repository later
            },
            onCommentClick = { post ->
                CommentsBottomSheet
                    .newInstance(post.id, post.likeCount, post.isLiked)
                    .show(childFragmentManager, "comments")
            }
        )

        binding.homeFeedRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(rv, dx, dy)
                    val lm = layoutManager as LinearLayoutManager
                    val visible = lm.childCount
                    val total = lm.itemCount
                    val first = lm.findFirstVisibleItemPosition()
                    val state = viewModel.uiState.value
                    if (state is HomeViewModel.UiState.Success
                        && !state.isLoadingMore
                        && state.hasMore
                        && (visible + first) >= total - 2
                    ) {
                        viewModel.loadMore()
                    }
                }
            })
        }

        videoFocusManager = VideoFocusManager(
            recyclerView = binding.homeFeedRv,
            getAdapter = { postAdapter }
        )
        videoFocusManager.attach()
        viewLifecycleOwner.lifecycle.addObserver(videoFocusManager)
    }

    private fun observeFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is HomeViewModel.UiState.Loading -> { /* show shimmer later */ }
                    is HomeViewModel.UiState.Success -> {
                        postAdapter.submitList(state.posts)
                        binding.homeFeedRv.post {
                            videoFocusManager.updateFocus()
                        }
                    }
                    is HomeViewModel.UiState.Error -> { /* show error later */ }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoFocusManager.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        videoFocusManager.updateFocus()
    }

    override fun onDestroyView() {
        videoFocusManager.detach()
        super.onDestroyView()
        _binding = null
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
                        viewLifecycleOwner.lifecycleScope.launch {
                            kotlinx.coroutines.delay(3000)
                            if (UploadStateHolder.state.value
                                        is UploadStateHolder.UploadState.Success
                            ) UploadStateHolder.reset()
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

    private fun setupAppBarFade() {
        binding.homeAppbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
                val total = appBar.totalScrollRange
                if (total == 0) return@OnOffsetChangedListener
                val ratio = abs(verticalOffset).toFloat() / total.toFloat()
                binding.homeAppName.alpha = 1f - ratio
                binding.homeBtnNotifications.alpha = 1f - ratio
            }
        )
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
}