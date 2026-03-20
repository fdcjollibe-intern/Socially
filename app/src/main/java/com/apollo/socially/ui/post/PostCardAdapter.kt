package com.apollo.socially.ui.post

import android.os.SystemClock
import android.text.SpannableString
import android.text.Spannable
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemPostCardBinding
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide

class PostCardAdapter(
    private val onLikeClick: (PostModel) -> Unit = {},
    private val onCommentClick: (PostModel) -> Unit = {}
) : ListAdapter<PostModel, PostCardAdapter.PostViewHolder>(PostDiffCallback()) {

    private var isMuted = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.pauseVideo()
    }


    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        // Full release when view leaves the window
        holder.releasePlayer()
    }

    inner class PostViewHolder(
        private val binding: ItemPostCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentAdapter: PostMediaPagerAdapter? = null
        private var lastTapTime = 0L
        private var isCaptionExpanded = false

        fun bind(post: PostModel) {
            isCaptionExpanded = false

            // ── Header avatar ─────────────────────────────────────
            binding.postCardUsername.text = post.username

            if (!post.userAvatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(post.userAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user_profile_placeholder_avatar)
                    .into(binding.postCardAvatar)
            } else {
                binding.postCardAvatar.setImageResource(
                    post.userAvatarRes ?: R.drawable.user_profile_placeholder_avatar
                )
            }

            // Display name under username in header
            if (post.displayName.isNotBlank() && post.displayName != post.username) {
                binding.postCardDisplayName.visibility = View.VISIBLE
                binding.postCardDisplayName.text = post.displayName
            } else {
                binding.postCardDisplayName.visibility = View.GONE
            }

            binding.postCardVerified.visibility =
                if (post.isVerified) View.VISIBLE else View.GONE

            if (!post.musicLabel.isNullOrBlank()) {
                binding.postCardMusicRow.visibility = View.VISIBLE
                binding.postCardMusicLabel.text = post.musicLabel
            } else {
                binding.postCardMusicRow.visibility = View.GONE
            }

            // ── Caption area avatar ───────────────────────────────
            if (!post.userAvatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(post.userAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user_profile_placeholder_avatar)
                    .into(binding.postCardCaptionAvatar)
            } else {
                binding.postCardCaptionAvatar.setImageResource(
                    post.userAvatarRes ?: R.drawable.user_profile_placeholder_avatar
                )
            }

            // ── Caption with bold username prefix ─────────────────
            // Build: "username caption text" where username is bold
            val captionText = when {
                post.username.isNotBlank() && post.caption.isNotBlank() ->
                    "${post.username} ${post.caption}"
                post.username.isNotBlank() ->
                    post.username
                post.caption.isNotBlank() ->
                    post.caption
                else -> ""
            }

            binding.postCardCaption.maxLines = 2
            if (captionText.isNotBlank()) {
                val spannable = applyMentionSpans(captionText)
                // Bold the username prefix
                if (post.username.isNotBlank() && captionText.startsWith(post.username)) {
                    spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        post.username.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                binding.postCardCaption.text = spannable
            } else {
                binding.postCardCaption.text = ""
            }

            binding.postCardTimeAgo.text = post.timeAgo
            binding.postCardLikeCount.text = formatCount(post.likeCount) + " Liked"

            // See more — only visible if caption exceeds 2 lines
            binding.postCardSeeMore.visibility = View.GONE
            binding.postCardSeeMore.text = "see more"
            binding.postCardCaption.post {
                val layout = binding.postCardCaption.layout ?: return@post
                binding.postCardSeeMore.visibility =
                    if (layout.lineCount > 2) View.VISIBLE else View.GONE
            }

            binding.postCardSeeMore.setOnClickListener {
                isCaptionExpanded = !isCaptionExpanded
                if (isCaptionExpanded) {
                    binding.postCardCaption.maxLines = Int.MAX_VALUE
                    binding.postCardSeeMore.text = "see less"
                } else {
                    binding.postCardCaption.maxLines = 2
                    binding.postCardSeeMore.text = "see more"
                }
            }

            // ── Like state ────────────────────────────────────────
            updateLikeButton(post.isLiked)

            // ── Media ─────────────────────────────────────────────
            val urls = post.imageUrls
            val hasVideo = urls.any { post.isVideoUrl(it) }

            binding.postCardBtnMute.visibility =
                if (hasVideo) View.VISIBLE else View.GONE
            updateMuteButton()

            currentAdapter?.releaseAll()
            currentAdapter = null
            binding.postCardViewPager.adapter = null

            when {
                urls.size >= 2 -> {
                    binding.postCardSingleContainer.visibility = View.GONE
                    binding.postCardPagerContainer.visibility = View.VISIBLE

                    val adapter = PostMediaPagerAdapter(urls = urls, isMuted = isMuted)
                    currentAdapter = adapter
                    binding.postCardViewPager.adapter = adapter
                    binding.postCardViewPager.offscreenPageLimit = 1

                    setupDots(urls.size, 0)
                    binding.postCardViewPager.registerOnPageChangeCallback(
                        object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageSelected(position: Int) {
                                setupDots(urls.size, position)
                                adapter.onPageSelected(position)
                            }
                        }
                    )
                }

                urls.size == 1 -> {
                    binding.postCardDotsContainer.visibility = View.GONE
                    if (hasVideo) {
                        binding.postCardSingleContainer.visibility = View.GONE
                        binding.postCardPagerContainer.visibility = View.VISIBLE
                        val adapter = PostMediaPagerAdapter(urls = urls, isMuted = isMuted)
                        currentAdapter = adapter
                        binding.postCardViewPager.adapter = adapter
                    } else {
                        binding.postCardSingleContainer.visibility = View.VISIBLE
                        binding.postCardPagerContainer.visibility = View.GONE
                        Glide.with(binding.root.context)
                            .load(urls[0])
                            .centerCrop()
                            .placeholder(R.drawable.sample_photo)
                            .error(R.drawable.sample_photo)
                            .into(binding.postCardImage)
                    }
                }

                else -> {
                    binding.postCardSingleContainer.visibility = View.VISIBLE
                    binding.postCardPagerContainer.visibility = View.GONE
                    binding.postCardDotsContainer.visibility = View.GONE
                    post.images.firstOrNull()
                        ?.let { binding.postCardImage.setImageResource(it) }
                }
            }

            // ── Click listeners ───────────────────────────────────
            binding.postCardBtnComment.setOnClickListener { onCommentClick(post) }

            binding.postCardBtnLike.setOnClickListener {
                updateLikeButton(!post.isLiked)
                onLikeClick(post)
            }

            binding.postCardBtnMute.setOnClickListener {
                isMuted = !isMuted
                updateMuteButton()
                currentAdapter?.setMuted(isMuted)
            }

            val tapTarget = if (urls.size >= 2 || hasVideo)
                binding.postCardPagerContainer
            else
                binding.postCardSingleContainer

            tapTarget.setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (now - lastTapTime < 300) triggerDoubleTapLike(post)
                lastTapTime = now
            }
        }

        fun pauseVideo() {
            currentAdapter?.pauseAll()
        }

        fun resumeVideo() {
            currentAdapter?.resumeActive()
        }

        fun releasePlayer() {
            currentAdapter?.releaseAll()
            currentAdapter = null
        }

        private fun triggerDoubleTapLike(post: PostModel) {
            if (!post.isLiked) {
                updateLikeButton(true)
                onLikeClick(post)
            }
            binding.postCardDoubleTapHeart.apply {
                visibility = View.VISIBLE
                alpha = 1f
                scaleX = 0f
                scaleY = 0f
                animate()
                    .scaleX(1.3f).scaleY(1.3f)
                    .setDuration(180)
                    .withEndAction {
                        animate()
                            .alpha(0f)
                            .scaleX(1.6f).scaleY(1.6f)
                            .setDuration(280)
                            .withEndAction { visibility = View.GONE }
                            .start()
                    }.start()
            }
        }

        fun updateLikeButton(isLiked: Boolean) {
            binding.postCardBtnLike.setColorFilter(
                binding.root.context.getColor(
                    if (isLiked) android.R.color.holo_red_light
                    else android.R.color.white
                )
            )
        }

        private fun updateMuteButton() {
            binding.postCardBtnMute.setImageResource(
                if (isMuted) R.drawable.ic_volume_off else R.drawable.ic_volume_on
            )
        }

        private fun setupDots(totalCount: Int, currentPosition: Int) {
            binding.postCardDotsContainer.removeAllViews()
            binding.postCardDotsContainer.visibility = View.VISIBLE

            val dotCount = minOf(totalCount, 3)
            val activeDot = when {
                totalCount <= 3 -> currentPosition
                currentPosition == 0 -> 0
                currentPosition == totalCount - 1 -> 2
                else -> 1
            }

            val context = binding.root.context
            val dotSize = context.resources.getDimensionPixelSize(R.dimen.dot_size)
            val dotMargin = context.resources.getDimensionPixelSize(R.dimen.dot_margin)

            for (i in 0 until dotCount) {
                val dot = ImageView(context).apply {
                    setImageResource(
                        if (i == activeDot) R.drawable.dot_active
                        else R.drawable.dot_inactive
                    )
                    val params = ViewGroup.MarginLayoutParams(dotSize, dotSize)
                    params.setMargins(dotMargin, 0, dotMargin, 0)
                    layoutParams = params
                }
                binding.postCardDotsContainer.addView(dot)
            }
        }

        private fun applyMentionSpans(text: String): SpannableString {
            val spannable = SpannableString(text)
            Regex("([#@][\\w]+)").findAll(text).forEach { match ->
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    match.range.first,
                    match.range.last + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }
    }

    private fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fk", count / 1_000f)
        else -> count.toString()
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(old: PostModel, new: PostModel) = old.id == new.id
        override fun areContentsTheSame(old: PostModel, new: PostModel) = old == new
    }
}