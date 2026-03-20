package com.apollo.socially.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FollowEventBus {
    private val _events = MutableSharedFlow<FollowEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<FollowEvent> = _events.asSharedFlow()

    suspend fun emit(event: FollowEvent) = _events.emit(event)

    data class FollowEvent(
        val targetUserId: String,
        val isNowFollowing: Boolean
    )
}