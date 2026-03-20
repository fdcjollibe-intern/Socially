package com.apollo.socially.ui.story

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.data.repository.StoryRepository
import com.apollo.socially.data.upload.StoryUploadService
import com.apollo.socially.databinding.FragmentStoryViewerBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlin.math.abs

class StoryViewerFragment : Fragment() {

    private var _binding: FragmentStoryViewerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StoryViewModel by viewModels()

    private val STORY_DURATION_MS = 5_000L
    private val VIDEO_MAX_DURATION_MS = 15_000L

    private var progressAnimator: ValueAnimator? = null
    private var isPaused = false

    // Navigation state
    private var userGroups: List<StoryRepository.UserWithStories> = emptyList()
    private var currentGroupIndex = 0
    private var currentStoryIndex = 0

    private var initialTouchY = 0f

    // Media picker for adding story
    private val mediaPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        val isVideo = requireContext().contentResolver
            .getType(uri)?.startsWith("video") == true
        uploadStory(uri, isVideo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetUserId = arguments?.getString("userId")
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: return

        observeViewModel()
        viewModel.loadForUser(targetUserId)
        setupTouchHandling()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (_binding == null) return@collect
                when (state) {
                    is StoryViewModel.UiState.Loading -> { /* show loading */ }
                    is StoryViewModel.UiState.Ready -> {
                        userGroups = state.userGroups
                        currentGroupIndex = state.startGroupIndex
                        currentStoryIndex = 0

                        if (userGroups.isEmpty()) {
                            // No stories — show add story UI
                            showAddStoryPrompt()
                        } else {
                            bindCurrentStory()
                        }
                    }
                    is StoryViewModel.UiState.Error -> closeViewer()
                }
            }
        }
    }

    private fun bindCurrentStory() {
        val group = userGroups.getOrNull(currentGroupIndex) ?: run {
            closeViewer(); return
        }
        val story = group.stories.getOrNull(currentStoryIndex) ?: run {
            goNextGroup(); return
        }

        // Mark as viewed
        viewModel.markStoryViewed(story.id)

        // Bind user info
        binding.storyViewerUsername.text = group.user.username
        binding.storyViewerTime.text = story.createdAt?.let { timeAgo(it) } ?: "just now"

        if (!group.user.profileImageUrl.isNullOrBlank()) {
            Glide.with(this).load(group.user.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.user_profile_placeholder_avatar)
                .into(binding.storyViewerAvatar)
        }

        // Show delete button only for own stories
        val isOwn = group.user.uid == viewModel.currentUid
        binding.storyViewerDelete.visibility = if (isOwn) View.VISIBLE else View.GONE

        binding.storyViewerDelete.setOnClickListener {
            viewModel.deleteStory(story.id) {
                // Remove from local list and advance
                val updatedStories = group.stories.toMutableList()
                updatedStories.removeAt(currentStoryIndex)
                if (updatedStories.isEmpty()) {
                    goNextGroup()
                } else {
                    currentStoryIndex = currentStoryIndex.coerceAtMost(updatedStories.size - 1)
                    bindCurrentStory()
                }
            }
        }

        // Load media
        if (story.isVideo()) {
            binding.storyViewerImage.visibility = View.GONE
            binding.storyViewerVideo.visibility = View.VISIBLE
            binding.storyViewerVideo.apply {
                setVideoURI(Uri.parse(story.mediaUrl))
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.start()
                    val duration = minOf(mp.duration.toLong(), VIDEO_MAX_DURATION_MS)
                    startProgress(duration)
                }
            }
        } else {
            binding.storyViewerVideo.visibility = View.GONE
            binding.storyViewerImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(story.mediaUrl)
                .centerCrop()
                .into(binding.storyViewerImage)
            startProgress(STORY_DURATION_MS)
        }
    }

    private fun showAddStoryPrompt() {
        // If viewing own story slot but no stories exist, open picker
        val isOwnSlot = arguments?.getString("userId") == viewModel.currentUid ||
                arguments?.getString("userId") == null
        if (isOwnSlot) {
            mediaPickerLauncher.launch("image/* video/*")
        } else {
            closeViewer()
        }
    }

    private fun uploadStory(uri: Uri, isVideo: Boolean) {
        val intent = StoryUploadService.buildIntent(
            requireContext(), uri, isVideo, ""
        )
        requireContext().startForegroundService(intent)
        closeViewer()
    }

    // ── Progress ──────────────────────────────────────────────

    private fun startProgress(durationMs: Long) {
        progressAnimator?.cancel()
        binding.storyProgressBar.progress = 0

        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener {
                binding.storyProgressBar.progress = it.animatedValue as Int
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(anim: android.animation.Animator) {
                    if (!isPaused) goNextStory()
                }
            })
            start()
        }
    }

    private fun pauseProgress() {
        progressAnimator?.pause()
        isPaused = true
        binding.storyViewerVideo.pause()
    }

    private fun resumeProgress() {
        progressAnimator?.resume()
        isPaused = false
        binding.storyViewerVideo.start()
    }

    // ── Navigation ────────────────────────────────────────────

    private fun goNextStory() {
        val group = userGroups.getOrNull(currentGroupIndex) ?: return
        if (currentStoryIndex < group.stories.size - 1) {
            currentStoryIndex++
            bindCurrentStory()
        } else {
            goNextGroup()
        }
    }

    private fun goPreviousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            bindCurrentStory()
        } else {
            goPreviousGroup()
        }
    }

    private fun goNextGroup() {
        if (currentGroupIndex < userGroups.size - 1) {
            currentGroupIndex++
            currentStoryIndex = 0
            bindCurrentStory()
        } else {
            closeViewer()
        }
    }

    private fun goPreviousGroup() {
        if (currentGroupIndex > 0) {
            currentGroupIndex--
            currentStoryIndex = 0
            bindCurrentStory()
        }
    }

    private fun closeViewer() {
        progressAnimator?.cancel()
        binding.storyViewerVideo.stopPlayback()
        findNavController().popBackStack()
    }

    // ── Touch handling ────────────────────────────────────────

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        binding.storyViewerClose.setOnClickListener { closeViewer() }

        val gestureDetector = GestureDetectorCompat(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val screenWidth = binding.storyViewerRoot.width
                    if (e.x < screenWidth * 0.4f) goPreviousStory()
                    else goNextStory()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val deltaY = e2.y - (e1?.y ?: 0f)
                    if (deltaY > 150 && abs(velocityY) > abs(velocityX)) {
                        animateSlideDownClose()
                        return true
                    }
                    return false
                }
            })

        binding.storyViewerRoot.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialTouchY = event.rawY
                    binding.storyViewerRoot.postDelayed({
                        if (!isPaused) {
                            pauseProgress()
                            binding.storyViewerTopOverlay.animate()
                                .alpha(0f).setDuration(200).start()
                        }
                    }, 300)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialTouchY
                    if (deltaY > 0) {
                        binding.storyViewerRoot.translationY = deltaY
                        binding.storyViewerRoot.alpha =
                            1f - (deltaY / 600f).coerceIn(0f, 1f)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = event.rawY - initialTouchY
                    if (isPaused) {
                        resumeProgress()
                        binding.storyViewerTopOverlay.animate()
                            .alpha(1f).setDuration(200).start()
                    }
                    if (deltaY > 200) animateSlideDownClose()
                    else binding.storyViewerRoot.animate()
                        .translationY(0f).alpha(1f).setDuration(200).start()
                    binding.storyViewerRoot.removeCallbacks(null)
                }
            }
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun animateSlideDownClose() {
        binding.storyViewerRoot.animate()
            .translationY(binding.storyViewerRoot.height.toFloat())
            .alpha(0f).setDuration(280)
            .withEndAction { closeViewer() }.start()
    }

    private fun timeAgo(date: java.util.Date): String {
        val diff = System.currentTimeMillis() - date.time
        val minutes = diff / 60_000
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "just now"
        }
    }

    override fun onPause() {
        super.onPause()
        pauseProgress()
    }

    override fun onResume() {
        super.onResume()
        if (isPaused) resumeProgress()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        _binding?.storyViewerVideo?.stopPlayback()
        super.onDestroyView()
        _binding = null
    }
}