package com.apollo.socially.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemProfileGridPostBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.ui.common.PostCardBottomSheet

class ProfileGridAdapter(
    private val fragmentManager: FragmentManager
) : ListAdapter<PostModel, ProfileGridAdapter.GridViewHolder>(PostDiffCallback()) {

    private var itemSize = 0

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        // Calculate square size once when attached — always accurate
        recyclerView.post {
            itemSize = recyclerView.measuredWidth / 3
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemProfileGridPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        // Apply square dimensions at bind time — measuredWidth is ready here
        if (itemSize == 0) itemSize = holder.itemView.resources.displayMetrics.widthPixels / 3
        holder.itemView.layoutParams = RecyclerView.LayoutParams(itemSize, itemSize)
        holder.bind(getItem(position))
    }

    inner class GridViewHolder(
        private val binding: ItemProfileGridPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: PostModel) {
            post.imageRes?.let { binding.gridPostImage.setImageResource(it) }
            binding.root.setOnClickListener {
                PostCardBottomSheet.show(fragmentManager, post)
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostModel>() {
        override fun areItemsTheSame(old: PostModel, new: PostModel) = old.id == new.id
        override fun areContentsTheSame(old: PostModel, new: PostModel) = old == new
    }
}