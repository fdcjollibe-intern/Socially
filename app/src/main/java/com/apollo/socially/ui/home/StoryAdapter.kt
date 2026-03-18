package com.apollo.socially.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemStoryBinding
import com.apollo.socially.model.StoryModel
import com.apollo.socially.R

class StoryAdapter(
    private val onAddStoryClick: () -> Unit,
    private val onStoryClick: (StoryModel) -> Unit
) : ListAdapter<StoryModel, StoryAdapter.StoryViewHolder>(StoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val binding = ItemStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class StoryViewHolder(private val binding: ItemStoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(story: StoryModel) {
            binding.storyUsername.text = if (story.isMyStory) "Your Story" else story.username
            story.avatarRes?.let { binding.storyAvatar.setImageResource(it) }

            if (story.isMyStory) {
                // Add story style — gradient ring, + badge visible, no teal ring
                binding.storyRing.setBackgroundResource(R.drawable.story_add_ring_bg)
                binding.storyAddIcon.visibility = View.VISIBLE
                binding.root.setOnClickListener { onAddStoryClick() }
            } else {
                // Regular story — teal ring if unseen
                binding.storyRing.setBackgroundResource(
                    if (story.hasUnseenStory) R.drawable.story_ring_bg
                    else R.drawable.story_ring_seen_bg
                )
                binding.storyAddIcon.visibility = View.GONE
                binding.root.setOnClickListener { onStoryClick(story) }
            }
        }
    }

    class StoryDiffCallback : DiffUtil.ItemCallback<StoryModel>() {
        override fun areItemsTheSame(old: StoryModel, new: StoryModel) = old.id == new.id
        override fun areContentsTheSame(old: StoryModel, new: StoryModel) = old == new
    }
}
