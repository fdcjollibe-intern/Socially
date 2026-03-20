package com.apollo.socially.ui.story

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemStoryBinding
import com.bumptech.glide.Glide

class StoryAdapter(
    private val onMyStoryClick: (StoryRowViewModel.StoryRowItem) -> Unit,
    private val onStoryClick: (StoryRowViewModel.StoryRowItem) -> Unit
) : ListAdapter<StoryRowViewModel.StoryRowItem, StoryAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class StoryViewHolder(private val binding: ItemStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StoryRowViewModel.StoryRowItem) {
            binding.storyUsername.text = if (item.isMyStory) "Your Story" else item.username

            // Avatar
            if (!item.avatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(item.avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user_profile_placeholder_avatar)
                    .into(binding.storyAvatar)
            } else {
                binding.storyAvatar.setImageResource(R.drawable.user_profile_placeholder_avatar)
            }

            // Ring visibility — show ring only if has story
            binding.storyRing.visibility =
                if (item.hasStory && !item.isMyStory) View.VISIBLE else View.INVISIBLE

            // Ring color — gray if all seen, accent if unseen
            if (item.hasUnseenStory) {
                binding.storyRing.setBackgroundResource(R.drawable.story_ring_bg)
            } else {
                binding.storyRing.setBackgroundResource(R.drawable.story_ring_seen_bg)
            }

            // Add icon — only on my story
            binding.storyAddIcon.visibility =
                if (item.isMyStory) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                if (item.isMyStory) onMyStoryClick(item)
                else onStoryClick(item)
            }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<StoryRowViewModel.StoryRowItem>() {
        override fun areItemsTheSame(
            old: StoryRowViewModel.StoryRowItem,
            new: StoryRowViewModel.StoryRowItem
        ) = old.userId == new.userId
        override fun areContentsTheSame(
            old: StoryRowViewModel.StoryRowItem,
            new: StoryRowViewModel.StoryRowItem
        ) = old == new
    }
}