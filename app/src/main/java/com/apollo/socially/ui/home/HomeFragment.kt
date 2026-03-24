package com.apollo.socially.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.data.upload.PostUploadService
import com.apollo.socially.data.upload.StoryUploadService
import com.apollo.socially.data.upload.StoryUploadStateHolder
import com.apollo.socially.data.upload.UploadStateHolder
import com.apollo.socially.databinding.FragmentHomeBinding
import com.apollo.socially.ui.post.CommentsBottomSheet
import com.apollo.socially.ui.post.PostCardAdapter
import com.apollo.socially.ui.post.VideoFocusManager
import com.apollo.socially.ui.story.StoryAdapter
import com.apollo.socially.ui.story.StoryRowViewModel
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val storyRowViewModel: StoryRowViewModel by viewModels()

    private lateinit var postAdapter: PostCardAdapter
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var videoFocusManager: VideoFocusManager

    private val storyMediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val isVideo = requireContext().contentResolver
            .getType(uri)?.startsWith("video") == true
        val intent = StoryUploadService.buildIntent(
            requireContext(), uri, isVideo, ""
        )
        requireContext().startForegroundService(intent)
    }

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
        setupStories()
        observeFeed()
        observeStories()
        observeUploadState()
        observeStoryUpload()
        observeNotificationBadge()

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

    private fun observeNotificationBadge() {
        viewLifecycleOwner.lifecycleScope.launch {
            val notificationRepo = com.apollo.socially.data.repository.NotificationRepository()
            try {
                val notifications = notificationRepo.getUserNotifications(limit = 50).getOrNull() ?: emptyList()
                val hasUnread = notifications.any { !it.isRead }
                binding.homeNotificationBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
            } catch (_: Exception) {
                // Ignore errors
            }
        }
    }


    // ── Feed ──────────────────────────────────────────────────

    private fun setupFeed() {
        postAdapter = PostCardAdapter(
            onLikeClick = { post ->
                viewModel.toggleLike(post)
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
                    is HomeViewModel.UiState.Loading -> {}
                    is HomeViewModel.UiState.Success -> {
                        postAdapter.submitList(state.posts)
                        binding.homeFeedRv.post {
                            videoFocusManager.updateFocus()
                        }
                    }
                    is HomeViewModel.UiState.Error -> {}
                }
            }
        }

        // Observe spam warnings
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSpamWarning.collect { message ->
                message?.let {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Please wait")
                        .setMessage(it)
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            viewModel.clearSpamWarning()
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        }
    }

    // ── Stories ───────────────────────────────────────────────

    private fun setupStories() {
        storyAdapter = StoryAdapter(
            onMyStoryClick = { item ->
                if (item.hasStory) {
                    val bundle = Bundle().apply { putString("userId", item.userId) }
                    findNavController().navigate(R.id.action_home_to_storyViewer, bundle)
                } else {
                    // No story yet — open media picker
                    storyMediaPickerLauncher.launch(arrayOf("image/*", "video/*"))
                }
            },
            onStoryClick = { item ->
                val bundle = Bundle().apply { putString("userId", item.userId) }
                findNavController().navigate(R.id.action_home_to_storyViewer, bundle)
            }
        )

//        binding.homeStoriesRv.apply {
//            layoutManager = LinearLayoutManager(
//                requireContext(), LinearLayoutManager.HORIZONTAL, false
//            )
//            adapter = storyAdapter
//        }


    }

    private fun observeStories() {
        viewLifecycleOwner.lifecycleScope.launch {
            storyRowViewModel.uiState.collect { state ->
                if (state is StoryRowViewModel.UiState.Success) {
                    storyAdapter.submitList(state.rows)
                }
            }
        }
    }

    private fun observeStoryUpload() {
        viewLifecycleOwner.lifecycleScope.launch {
            StoryUploadStateHolder.state.collect { state ->
                when (state) {
                    is StoryUploadStateHolder.State.Success -> {
                        storyRowViewModel.loadStories()
                        StoryUploadStateHolder.reset()
                    }
                    is StoryUploadStateHolder.State.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Story upload failed",
                            Toast.LENGTH_SHORT
                        ).show()
                        StoryUploadStateHolder.reset()
                    }
                    else -> {}
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        videoFocusManager.pauseAll()
    }

    override fun onResume() {
        super.onResume()
        videoFocusManager.updateFocus()
        // Refresh notification badge when returning to home
        viewModel.checkUnreadNotifications()
    }

    override fun onDestroyView() {
        videoFocusManager.detach()
        super.onDestroyView()
        _binding = null
    }

    // ── Upload banners ────────────────────────────────────────

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