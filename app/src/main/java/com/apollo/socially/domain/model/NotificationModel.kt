package com.apollo.socially.model

data class NotificationModel(
    val id: String,
    val avatarRes: Int?,
    val message: String,
    val type: NotifType,
    val thumbnailRes: Int? = null
)

enum class NotifType {
    POST,       // shows thumbnail on right
    FOLLOW,     // shows "Follow back" button
    LIKE,       // shows thumbnail
    GENERAL     // no right action
}
