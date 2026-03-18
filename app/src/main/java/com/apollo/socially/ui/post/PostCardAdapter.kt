package com.apollo.socially.ui.post

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

class PostCardAdapter(
    private val onSeeMoreClick: (PostModel) -> Unit,
    private val onLikeClick: (PostModel) -> Unit = {}
) : ListAdapter<PostModel, PostCardAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) = holder.bind(getItem(position))

    inner class PostViewHolder(private val binding: ItemPostCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel) {
            binding.postCardUsername.text = post.username
            binding.postCardHeaderTime.text = post.timeAgo
            binding.postCardCaption.text = post.caption
            binding.postCardTimeAgo.text = post.timeAgo
            binding.postCardLikeCount.text = formatCount(post.likeCount) + " Liked"

            binding.postCardVerified.visibility =
                if (post.isVerified) View.VISIBLE else View.GONE

            post.userAvatarRes?.let { binding.postCardAvatar.setImageResource(it) }

            if (!post.musicLabel.isNullOrBlank()) {
                binding.postCardMusicRow.visibility = View.VISIBLE
                binding.postCardMusicLabel.text = post.musicLabel
            } else {
                binding.postCardMusicRow.visibility = View.GONE
            }

            val images = post.images

            if (images.size >= 2) {
                // ── MULTI-IMAGE: show ViewPager2, hide single ImageView ──
                binding.postCardSingleContainer.visibility = View.GONE
                binding.postCardPagerContainer.visibility = View.VISIBLE

                val pagerAdapter = PostImagePagerAdapter(images)
                binding.postCardViewPager.adapter = pagerAdapter

                // Build dot indicators
                setupDots(images.size, 0)
                binding.postCardViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        setupDots(images.size, position)
                    }
                })

            } else {
                // ── SINGLE IMAGE ──
                binding.postCardSingleContainer.visibility = View.VISIBLE
                binding.postCardPagerContainer.visibility = View.GONE
                binding.postCardDotsContainer.visibility = View.GONE
                images.firstOrNull()?.let { binding.postCardImage.setImageResource(it) }
            }

            binding.postCardSeeMore.setOnClickListener { onSeeMoreClick(post) }
            binding.postCardBtnLike.setOnClickListener { onLikeClick(post) }
        }

        private fun setupDots(count: Int, selectedIndex: Int) {
            binding.postCardDotsContainer.removeAllViews()
            binding.postCardDotsContainer.visibility = View.VISIBLE

            val context = binding.root.context
            val dotSize = context.resources.getDimensionPixelSize(R.dimen.dot_size)
            val dotMargin = context.resources.getDimensionPixelSize(R.dimen.dot_margin)

            for (i in 0 until count) {
                val dot = ImageView(context).apply {
                    setImageResource(
                        if (i == selectedIndex) R.drawable.dot_active else R.drawable.dot_inactive
                    )
                    val params = ViewGroup.MarginLayoutParams(dotSize, dotSize)
                    params.setMargins(dotMargin, 0, dotMargin, 0)
                    layoutParams = params
                }
                binding.postCardDotsContainer.addView(dot)
            }
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
