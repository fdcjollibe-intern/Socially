package com.apollo.socially.model

data class StoryModel(
    val id: String,
    val username: String,
    val avatarRes: Int?,
    val isMyStory: Boolean = false,
    val hasUnseenStory: Boolean = true
)
