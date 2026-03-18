package com.apollo.socially.ui.post

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemCommentBinding
import com.apollo.socially.model.CommentModel

class CommentAdapter : ListAdapter<CommentModel, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) = holder.bind(getItem(position))

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: CommentModel) {
            binding.commentUsername.text = comment.username
            binding.commentText.text = comment.text
            binding.commentTime.text = comment.timeAgo
            binding.commentLikeNumber.text = if (comment.likeCount > 0) comment.likeCount.toString() else ""
            comment.avatarRes?.let { binding.commentAvatar.setImageResource(it) }

            // Own comment username shown in accent color
            binding.commentUsername.setTextColor(
                binding.root.context.getColor(
                    if (comment.isOwnComment) R.color.accent else R.color.black
                )
            )

            binding.commentBtnLike.setOnClickListener {
                // TODO: toggle like via ViewModel
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<CommentModel>() {
        override fun areItemsTheSame(old: CommentModel, new: CommentModel) = old.id == new.id
        override fun areContentsTheSame(old: CommentModel, new: CommentModel) = old == new
    }
}
