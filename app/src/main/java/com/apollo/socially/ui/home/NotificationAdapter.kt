package com.apollo.socially.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.databinding.ItemNotificationBinding
import com.apollo.socially.model.NotificationModel
import com.apollo.socially.model.NotifType

class NotificationAdapter : ListAdapter<NotificationModel, NotificationAdapter.NotifViewHolder>(NotifDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotifViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) = holder.bind(getItem(position))

    inner class NotifViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notif: NotificationModel) {
            binding.notifMessage.text = notif.message
            notif.avatarRes?.let { binding.notifAvatar.setImageResource(it) }

            when (notif.type) {
                NotifType.POST, NotifType.LIKE -> {
                    binding.notifThumbnail.visibility = View.VISIBLE
                    binding.notifBtnFollowBack.visibility = View.GONE
                    notif.thumbnailRes?.let { binding.notifThumbnail.setImageResource(it) }
                }
                NotifType.FOLLOW -> {
                    binding.notifThumbnail.visibility = View.GONE
                    binding.notifBtnFollowBack.visibility = View.VISIBLE
                    binding.notifBtnFollowBack.setOnClickListener { /* TODO: follow back */ }
                }
                NotifType.GENERAL -> {
                    binding.notifThumbnail.visibility = View.GONE
                    binding.notifBtnFollowBack.visibility = View.GONE
                }
            }
        }
    }

    class NotifDiffCallback : DiffUtil.ItemCallback<NotificationModel>() {
        override fun areItemsTheSame(old: NotificationModel, new: NotificationModel) = old.id == new.id
        override fun areContentsTheSame(old: NotificationModel, new: NotificationModel) = old == new
    }
}
