package com.apollo.socially.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemNotificationBinding
import com.apollo.socially.domain.model.Notification
import com.apollo.socially.domain.model.NotificationType
import com.bumptech.glide.Glide

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotifViewHolder>(NotifDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) = holder.bind(getItem(position))

    inner class NotifViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notif: Notification) {
            // Build message
            val message = when (notif.type) {
                NotificationType.LIKE -> "${notif.actorUsername} liked your post. ${notif.createdAt.toTimeAgo()}"
                NotificationType.COMMENT -> "${notif.actorUsername} commented: \"${notif.commentText?.take(50)}${if ((notif.commentText?.length ?: 0) > 50) "..." else ""}\". ${notif.createdAt.toTimeAgo()}"
                NotificationType.FOLLOW -> "${notif.actorUsername} started following you. ${notif.createdAt.toTimeAgo()}"
            }
            binding.notifMessage.text = message
            
            // Load avatar
            if (!notif.actorAvatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(notif.actorAvatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user_profile_placeholder_avatar)
                    .into(binding.notifAvatar)
            } else {
                binding.notifAvatar.setImageResource(R.drawable.user_profile_placeholder_avatar)
            }

            // Handle different types
            when (notif.type) {
                NotificationType.LIKE, NotificationType.COMMENT -> {
                    binding.notifThumbnail.visibility = View.VISIBLE
                    binding.notifBtnFollowBack.visibility = View.GONE
                    if (!notif.postThumbnailUrl.isNullOrBlank()) {
                        Glide.with(binding.root.context)
                            .load(notif.postThumbnailUrl)
                            .centerCrop()
                            .placeholder(R.drawable.sample_photo)
                            .into(binding.notifThumbnail)
                    } else {
                        binding.notifThumbnail.setImageResource(R.drawable.sample_photo)
                    }
                }
                NotificationType.FOLLOW -> {
                    binding.notifThumbnail.visibility = View.GONE
                    binding.notifBtnFollowBack.visibility = View.VISIBLE
                    binding.notifBtnFollowBack.setOnClickListener { /* TODO: follow back */ }
                }
            }
            
            // Set background if unread
            if (!notif.isRead) {
                binding.root.setBackgroundResource(R.color.notif_unread_bg)
            } else {
                binding.root.setBackgroundResource(android.R.color.transparent)
            }
            
            // Handle click
            binding.root.setOnClickListener {
                onNotificationClick(notif)
            }
        }
        
        private fun java.util.Date?.toTimeAgo(): String {
            if (this == null) return "just now"
            val diff = System.currentTimeMillis() - time
            val minutes = diff / 60_000
            val hours = minutes / 60
            val days = hours / 24
            return when {
                days > 7 -> "${days / 7}w ago"
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                minutes > 0 -> "${minutes}m ago"
                else -> "just now"
            }
        }
    }

    class NotifDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(old: Notification, new: Notification) = old.id == new.id
        override fun areContentsTheSame(old: Notification, new: Notification) = old == new
    }
}
