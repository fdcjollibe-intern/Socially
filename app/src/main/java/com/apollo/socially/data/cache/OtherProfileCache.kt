package com.apollo.socially.data.cache

import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User

object OtherProfileCache {

    private var cachedUserId: String? = null
    private var cachedUser: User? = null
    private var cachedPosts: MutableList<Post> = mutableListOf()
    private var cachedIsFollowing: Boolean = false
    private var lastCacheTime: Long = 0

    private const val CACHE_VALIDITY_MS = 10 * 60 * 1000L

    fun hasCache(userId: String): Boolean {
        return cachedUserId == userId
                && cachedUser != null
                && isCacheValid()
    }

    fun getUser(userId: String): User? {
        if (cachedUserId != userId || !isCacheValid()) return null
        return cachedUser
    }

    fun getPosts(userId: String): List<Post>? {
        if (cachedUserId != userId || !isCacheValid()) return null
        return cachedPosts.toList()
    }

    fun getIsFollowing(userId: String): Boolean {
        if (cachedUserId != userId || !isCacheValid()) return false
        return cachedIsFollowing
    }

    fun cache(userId: String, user: User, posts: List<Post>, isFollowing: Boolean) {
        cachedUserId = userId
        cachedUser = user
        cachedPosts = posts.toMutableList()
        cachedIsFollowing = isFollowing
        lastCacheTime = System.currentTimeMillis()
    }

    fun updateUser(userId: String, user: User) {
        if (cachedUserId == userId) {
            cachedUser = user
            lastCacheTime = System.currentTimeMillis()
        }
    }

    fun updateIsFollowing(userId: String, isFollowing: Boolean) {
        if (cachedUserId == userId) {
            cachedIsFollowing = isFollowing
            lastCacheTime = System.currentTimeMillis()
        }
    }

    fun invalidate(userId: String? = null) {
        if (userId == null || cachedUserId == userId) {
            cachedUserId = null
            cachedUser = null
            cachedPosts.clear()
            cachedIsFollowing = false
            lastCacheTime = 0
        }
    }

    private fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime) < CACHE_VALIDITY_MS
    }
}