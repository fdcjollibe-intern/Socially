package com.apollo.socially.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemProfileGridPostBinding
import com.apollo.socially.model.PostModel
import com.bumptech.glide.Glide

class ProfileGridAdapter(
    private val onPostClick: (post: PostModel, index: Int) -> Unit
) : ListAdapter<PostModel, ProfileGridAdapter.GridViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemProfileGridPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        val itemSize = parent.measuredWidth / 3
        binding.root.layoutParams = RecyclerView.LayoutParams(itemSize, itemSize)
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GridViewHolder(
        private val binding: ItemProfileGridPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel, index: Int) {
            // Show gray placeholder for new posts
            if (post.isPlaceholder) {
                binding.gridPostImage.setImageDrawable(null)
                binding.gridPostImage.setBackgroundColor(
                    binding.root.context.getColor(android.R.color.darker_gray)
                )
                binding.root.isClickable = false
                return
            }
            
            // Reset background for non-placeholders
            binding.gridPostImage.background = null
            binding.root.isClickable = true
            
            // Load actual Firebase image URL or fall back to static resource
            if (!post.imageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(post.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.sample_photo)
                    .error(R.drawable.sample_photo)
                    .into(binding.gridPostImage)
            } else if (post.imageRes != null) {
                binding.gridPostImage.setImageResource(post.imageRes)
            } else {
                binding.gridPostImage.setImageResource(R.drawable.sample_photo)
            }
            
            binding.root.setOnClickListener {
                onPostClick(post, index)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(old: PostModel, new: PostModel) = old.id == new.id
        override fun areContentsTheSame(old: PostModel, new: PostModel) = old == new
    }
}
