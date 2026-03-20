package com.apollo.socially.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemSearchUserBinding
import com.apollo.socially.model.UiUserModel
import com.bumptech.glide.Glide

class SearchUserAdapter(
    private val onUserClick: (UiUserModel) -> Unit,
    private val onFollowClick: (UiUserModel, Boolean) -> Unit = { _, _ -> },
    private val showFollowButton: Boolean = true
) : ListAdapter<UiUserModel, SearchUserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemSearchUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) =
        holder.bind(getItem(position))

    override fun onBindViewHolder(holder: UserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Handle partial update
            val user = getItem(position)
            for (payload in payloads) {
                if (payload == PAYLOAD_FOLLOW_STATE) {
                    holder.updateFollowButton(user.isFollowing)
                }
            }
        }
    }

    inner class UserViewHolder(private val binding: ItemSearchUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UiUserModel) {
            binding.searchUserFullname.text = user.fullName
            binding.searchUserUsername.text = "@${user.username}"
            
            // Load avatar from Firebase URL or use resource fallback
            if (!user.avatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(user.avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user_profile_placeholder_avatar)
                    .error(R.drawable.user_profile_placeholder_avatar)
                    .into(binding.searchUserAvatar)
            } else if (user.avatarRes != null) {
                binding.searchUserAvatar.setImageResource(user.avatarRes)
            } else {
                binding.searchUserAvatar.setImageResource(R.drawable.user_profile_placeholder_avatar)
            }

            binding.searchUserVerified.visibility =
                if (user.isVerified) View.VISIBLE else View.GONE


            binding.searchUserFollowBtn.visibility =
                if (showFollowButton) View.VISIBLE else View.GONE

            updateFollowButton(user.isFollowing)

            binding.searchUserFollowBtn.setOnClickListener {
                // Trigger backend update
                onFollowClick(user, user.isFollowing)
            }

            binding.root.setOnClickListener { onUserClick(user) }
        }

        fun updateFollowButton(isFollowing: Boolean) {
            if (isFollowing) {
                binding.searchUserFollowBtn.text = "Following"
                binding.searchUserFollowBtn.setTextColor(
                    binding.root.context.getColor(R.color.gray)
                )
                binding.searchUserFollowBtn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(android.R.color.transparent)
                    )
                binding.searchUserFollowBtn.strokeWidth = 2
                binding.searchUserFollowBtn.strokeColor =
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(R.color.gray_border)
                    )
            } else {
                binding.searchUserFollowBtn.text = "Follow"
                binding.searchUserFollowBtn.setTextColor(
                    binding.root.context.getColor(R.color.white)
                )
                binding.searchUserFollowBtn.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        binding.root.context.getColor(R.color.accent)
                    )
                binding.searchUserFollowBtn.strokeWidth = 0
            }
        }
    }

    companion object {
        private const val PAYLOAD_FOLLOW_STATE = "follow_state"
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UiUserModel>() {
        override fun areItemsTheSame(old: UiUserModel, new: UiUserModel) = old.id == new.id
        override fun areContentsTheSame(old: UiUserModel, new: UiUserModel) = old == new
        
        override fun getChangePayload(oldItem: UiUserModel, newItem: UiUserModel): Any? {
            return if (oldItem.isFollowing != newItem.isFollowing) {
                PAYLOAD_FOLLOW_STATE
            } else {
                null
            }
        }
    }
}
