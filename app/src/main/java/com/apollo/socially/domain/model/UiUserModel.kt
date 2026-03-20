package com.apollo.socially.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Lightweight UI-only model used in Search, Followers, Following screens.
 * Your real user data lives in com.apollo.socially.domain.model.User (Firebase).
 * When wiring to backend, map User → UiUserModel in your ViewModel.
 */
@Parcelize
data class UiUserModel(
    val id: String,
    val fullName: String,
    val username: String,
    val avatarRes: Int? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val isFollowing: Boolean = false
) : Parcelable
