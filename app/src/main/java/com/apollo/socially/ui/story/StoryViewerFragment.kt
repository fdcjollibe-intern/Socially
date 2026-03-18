package com.apollo.socially.ui.story

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentStoryViewerBinding
import kotlin.math.abs

class StoryViewerFragment : Fragment() {

    private var _binding: FragmentStoryViewerBinding? = null
    private val binding get() = _binding!!

    // Story duration in ms — 12 seconds for image
    private val STORY_DURATION_MS = 12_000L

    private var progressAnimator: ValueAnimator? = null
    private var isPaused = false

    // Sample stories list — swap imageRes per story when real data is wired
    private val stories = listOf(
        StoryItem("my", "yourusername", R.drawable.user_profile_placeholder_avatar,
            R.drawable.sample_photo_mystory, "Just now"),
    )
    private var currentIndex = 0

    // For swipe-down-to-close gesture
    private var initialTouchY = 0f
    private var initialRootY = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindStory(currentIndex)
        setupTouchHandling()
    }

    // ── Bind current story ──
    private fun bindStory(index: Int) {
        val story = stories[index]
        binding.storyViewerImage.setImageResource(story.imageRes)
        binding.storyViewerUsername.text = story.username
        binding.storyViewerTime.text = story.timeAgo
        story.avatarRes?.let { binding.storyViewerAvatar.setImageResource(it) }
        startProgress()
    }

    // ── Animated progress bar ──
    private fun startProgress() {
        progressAnimator?.cancel()
        binding.storyProgressBar.progress = 0

        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = STORY_DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { binding.storyProgressBar.progress = it.animatedValue as Int }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (!isPaused) goNext()
                }
            })
            start()
        }
    }

    private fun pauseProgress() {
        progressAnimator?.pause()
        isPaused = true
    }

    private fun resumeProgress() {
        progressAnimator?.resume()
        isPaused = false
    }

    // ── Navigation ──
    private fun goNext() {
        if (currentIndex < stories.size - 1) {
            currentIndex++
            bindStory(currentIndex)
        } else {
            closeViewer()
        }
    }

    private fun goPrevious() {
        if (currentIndex > 0) {
            currentIndex--
            bindStory(currentIndex)
        }
        // If already first story, do nothing on left tap
    }

    private fun closeViewer() {
        progressAnimator?.cancel()
        findNavController().popBackStack()
    }

    // ── Slide-down-to-close animation ──
    private fun animateSlideDownClose() {
        binding.storyViewerRoot.animate()
            .translationY(binding.storyViewerRoot.height.toFloat())
            .alpha(0f)
            .setDuration(280)
            .withEndAction { closeViewer() }
            .start()
    }

    // ── All touch handling ──
    @Suppress("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        binding.storyViewerClose.setOnClickListener { closeViewer() }

        val gestureDetector = GestureDetectorCompat(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val screenWidth = binding.storyViewerRoot.width
                    if (e.x < screenWidth * 0.4f) goPrevious() else goNext()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    val deltaY = e2.y - (e1?.y ?: 0f)
                    // Swipe DOWN to close
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
                    initialRootY = binding.storyViewerRoot.translationY

                    // Long press detection via postDelayed
                    binding.storyViewerRoot.postDelayed({
                        if (!isPaused) {
                            pauseProgress()
                            // Fade out overlay
                            binding.storyViewerTopOverlay.animate()
                                .alpha(0f).setDuration(200).start()
                        }
                    }, 300)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialTouchY
                    if (deltaY > 0) {
                        // Follow finger downward
                        binding.storyViewerRoot.translationY = deltaY
                        binding.storyViewerRoot.alpha = 1f - (deltaY / 600f).coerceIn(0f, 1f)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = event.rawY - initialTouchY

                    if (isPaused) {
                        // Was long pressed — resume
                        resumeProgress()
                        binding.storyViewerTopOverlay.animate()
                            .alpha(1f).setDuration(200).start()
                    }

                    if (deltaY > 200) {
                        // Swiped down far enough — close
                        animateSlideDownClose()
                    } else {
                        // Snap back
                        binding.storyViewerRoot.animate()
                            .translationY(0f).alpha(1f).setDuration(200).start()
                    }

                    binding.storyViewerRoot.removeCallbacks(null)
                }
            }
            gestureDetector.onTouchEvent(event)
            true
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
        super.onDestroyView()
        _binding = null
    }

    // Simple data class for a story item
    data class StoryItem(
        val id: String,
        val username: String,
        val avatarRes: Int?,
        val imageRes: Int,
        val timeAgo: String
    )
}
